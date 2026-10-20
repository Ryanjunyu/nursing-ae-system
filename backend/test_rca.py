#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""RCA 根因辅助分析模块 —— 接口联调测试"""
import json
import time
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:8080"


def post(path, data, token=None):
    req = urllib.request.Request(BASE + path, data=json.dumps(data).encode(), method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status, json.loads(r.read())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read())
        except Exception:
            return e.code, {}


def get(path, token=None):
    req = urllib.request.Request(BASE + path)
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status, json.loads(r.read())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read())
        except Exception:
            return e.code, {}


def login(username):
    s, d = post("/api/auth/login", {"username": username, "password": "123456"})
    assert s == 200 and d.get("code") == 200, f"{username} 登录失败: {s} {d}"
    return d["data"]["token"]


print("== 等待后端启动 ==")
up = False
for _ in range(60):
    try:
        s, _ = get("/api/dict/injury-levels")
        if s in (200, 401):
            up = True
            print("  后端已就绪")
            break
    except Exception:
        pass
    time.sleep(2)
assert up, "后端未就绪"

head1 = login("head1")     # 护士长 内科一病区(deptId=1)
nurse1 = login("nurse1")   # 护士 内科一病区
nurse2 = login("nurse2")   # 护士 内科二病区(deptId=2)

# 取一个一病区事件
s, d = get("/api/events", head1)
events = d["data"]["records"]
assert events, "没有可分析的事件，请先跑 test_api.py 造数据"
ev = events[0]
eid = ev["eventId"]
print(f"\n== 选用事件 #{eid}  {ev['eventTypeName']} / {ev['deptName']} ==")

# 1) 辅助分析
print("\n== 1) 辅助分析建议 ==")
s, d = get(f"/api/rca/assist/{eid}", head1)
a = d["data"]
dims = list(a["suggestions"].keys())
print("  五维建议维度:", dims)
print("  完成度:", a["completeness"], "%")
assert set(dims) == {"人", "机", "料", "法", "环"}, "应返回五维建议"
assert all(len(v) > 0 for v in a["suggestions"].values()), "每维都应有建议"

# 2) 保存五维诱因（护士长）
print("\n== 2) 护士长保存五维诱因 ==")
factors = [
    {"dimension": "人", "factorType": "ROOT", "factorContent": "护士跌倒风险评估培训不足"},
    {"dimension": "机", "factorType": "PROXIMAL", "factorContent": "床栏损坏未及时维修"},
    {"dimension": "环", "factorType": "PROXIMAL", "factorContent": "地面湿滑无警示标识"},
]
s, d = post("/api/rca/factors", {"eventId": eid, "factors": factors}, head1)
print("  保存返回:", s, d)
assert d.get("code") == 200, "保存诱因失败"

# 3) 读取五维诱因
print("\n== 3) 读取五维诱因 ==")
s, d = get(f"/api/rca/factors/{eid}", head1)
print("  诱因数:", len(d["data"]), "维度:", [x["dimension"] for x in d["data"]])
assert len(d["data"]) == 3, "应有 3 条诱因"

# 4) 保存根因分析
print("\n== 4) 护士长保存根因分析 ==")
s, d = post("/api/rca", {
    "eventId": eid,
    "timeline": "10:00 患者下床；10:05 跌倒；10:10 护士发现并处置",
    "rootHuman": "患者及家属防跌倒宣教不到位",
    "rootMachine": "床栏损坏",
    "rootMaterial": "",
    "rootMethod": "跌倒风险评估流程未落实",
    "rootEnv": "地面湿滑、照明不足",
    "improvement": "加强防跌倒宣教，每日检查床栏，湿滑区域设警示",
    "analysisStatus": "DONE"
}, head1)
print("  保存返回:", s, d)
assert d.get("code") == 200, "保存根因失败"
rca_id = d["data"]

# 5) 读取根因
print("\n== 5) 读取根因记录 ==")
s, d = get(f"/api/rca/{eid}", head1)
r = d["data"]
print("  rcaId:", r["rcaId"], " 状态:", r["analysisStatus"], " 人因:", r["rootHuman"])
assert r["analysisStatus"] == "DONE"
assert "宣教" in r["rootHuman"]

# 6) 再取辅助分析，完成度应上升（3 个维度已填 -> 60%）
print("\n== 6) 复算完成度 ==")
s, d = get(f"/api/rca/assist/{eid}", head1)
print("  完成度:", d["data"]["completeness"], "% 各维度:", d["data"]["factorCountByDimension"])
assert d["data"]["completeness"] == 60, "3/5 维度有数据应为 60%"

# 7) 护士保存诱因 -> 403
print("\n== 7) 护士尝试保存（应 403）==")
s, d = post("/api/rca/factors", {"eventId": eid, "factors": factors}, nurse1)
print("  返回:", s, d.get("message"))
assert d.get("code") == 403, "护士不应有分析权限"

# 8) nurse2 跨科室查看一病区 RCA -> 403
print("\n== 8) 二病区护士跨科室查看（应 403）==")
s, d = get(f"/api/rca/{eid}", nurse2)
print("  返回:", s, d.get("message"))
assert d.get("code") == 403, "跨科室应被隔离"

print("\n== RCA 接口联调全部通过 ✅ ==")
