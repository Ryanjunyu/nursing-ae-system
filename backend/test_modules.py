#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""六大业务模块联调测试：统计 / 用户管理 / 时间筛选 / 审计修复"""
import json, re, time, urllib.request, urllib.error

BASE = "http://127.0.0.1:8080"
YML = r"C:\Users\RyanJunYu\Desktop\classevent\nursing-ae-system\backend\src\main\resources\application.yml"


def req(method, path, data=None, token=None):
    body = json.dumps(data).encode() if data is not None else None
    r = urllib.request.Request(BASE + path, data=body, method=method)
    if data is not None:
        r.add_header("Content-Type", "application/json")
    if token:
        r.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(r, timeout=15) as resp:
            return resp.status, json.loads(resp.read())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read())
        except Exception:
            return e.code, {}


def get(path, token=None): return req("GET", path, None, token)
def post(path, data, token=None): return req("POST", path, data, token)
def put(path, data, token=None): return req("PUT", path, data, token)


def login(u):
    s, d = post("/api/auth/login", {"username": u, "password": "123456"})
    assert d.get("code") == 200, f"{u} 登录失败 {d}"
    return d["data"]["token"]


print("== 等待后端 ==")
for _ in range(60):
    try:
        if get("/api/dict/injury-levels")[0] in (200, 401):
            print("  就绪"); break
    except Exception:
        pass
    time.sleep(2)

admin = login("admin"); head1 = login("head1"); nurse1 = login("nurse1")

# ---------- 5) 统计分析 ----------
print("\n== 5) 统计分析 ==")
s, d = get("/api/statistics/summary", admin); sa = d["data"]
print("  admin 概览: total=%s reported=%s analyzed=%s anonymous=%s topType=%s(%s) topDept=%s(%s)" % (
    sa["total"], sa["reported"], sa["analyzed"], sa["anonymous"], sa["topType"], sa["topTypeCount"], sa["topDept"], sa["topDeptCount"]))
s, d = get("/api/statistics/by-dept", admin); print("  admin 各科室:", [(x["name"], x["value"]) for x in d["data"]])
s, d = get("/api/statistics/by-type", admin); print("  admin 各类型:", [(x["name"], x["value"]) for x in d["data"]])
s, d = get("/api/statistics/by-level", admin); print("  admin 各分级:", [(x["name"], x["value"]) for x in d["data"]])
s, d = get("/api/statistics/trend?months=6", admin); print("  admin 趋势:", [(x["name"], x["value"]) for x in d["data"]])
assert len(d["data"]) == 6, "趋势应为6个月"

s, d = get("/api/statistics/summary", head1); sh = d["data"]
s2, d2 = get("/api/statistics/by-dept", head1)
print("  head1 概览 total=%s；各科室=%s" % (sh["total"], [(x["name"], x["value"]) for x in d2["data"]]))
assert sh["total"] <= sa["total"], "护士长可见量应<=管理员"
assert all(x["name"] == "内科一病区" for x in d2["data"]), "护士长只能看到本科室"

# ---------- 4) 时间区间筛选 ----------
print("\n== 4) 事件检索-时间区间 ==")
from urllib.parse import urlencode
q1 = urlencode({"page": 1, "size": 10, "startTime": "2020-01-01 00:00:00", "endTime": "2020-12-31 23:59:59"})
s, d = get("/api/events?" + q1, admin)
print("  2020 年区间事件数:", d["data"]["total"], "（应为0）")
assert d["data"]["total"] == 0
q2 = urlencode({"page": 1, "size": 10, "startTime": "2026-01-01 00:00:00", "endTime": "2026-12-31 23:59:59"})
s, d = get("/api/events?" + q2, admin)
print("  2026 年区间事件数:", d["data"]["total"], "（应>0）")
assert d["data"]["total"] > 0

# ---------- 1) 用户管理（角色限制） ----------
print("\n== 1) 用户权限管理 ==")
s, d = get("/api/users", admin); print("  admin 用户列表数:", len(d["data"]))
uname = "t" + str(int(time.time()))[-6:]
s, d = post("/api/users", {"username": uname, "password": "temp123", "realName": "测试用户",
                           "role": "NURSE", "deptId": 1, "phone": "13800000000", "status": 1}, admin)
print("  admin 新增用户:", d.get("code"), d.get("message"), "-> id", d.get("data"))
assert d.get("code") == 200
uid = d["data"]
s, d = put(f"/api/users/{uid}", {"username": uname, "realName": "测试用户改", "role": "HEAD_NURSE",
                                 "deptId": 1, "status": 0}, admin)
print("  admin 修改用户:", d.get("code"), d.get("message"))
assert d.get("code") == 200
s, d = get("/api/users", admin)
row = next(x for x in d["data"] if x["userId"] == uid)
print("  改后:", row["realName"], row["role"], "状态(0=停用)写在列表:", row.get("status"))
# 非管理员新增 -> 403
for who, tk in [("head1", head1), ("nurse1", nurse1)]:
    s, d = post("/api/users", {"username": "x"+uname, "password": "p", "realName": "x", "role": "NURSE", "deptId": 1}, tk)
    print(f"  {who} 新增用户:", d.get("code"), d.get("message"))
    assert d.get("code") == 403

# ---------- 6) 审计修复验证 ----------
print("\n== 6) 审计日志（真实操作人，非 ANONYMOUS）==")
import pymysql
txt = open(YML, encoding="utf-8").read()
line = next(l for l in txt.splitlines() if l.strip().startswith("password"))
dbpwd = line.split("password", 1)[1].strip().lstrip(":： ").strip()
conn = pymysql.connect(host="127.0.0.1", port=3306, user="root", password=dbpwd, database="nursing_ae_db", charset="utf8mb4")
cur = conn.cursor()
cur.execute("""SELECT action_type, username, COUNT(*) FROM sys_audit_log
               WHERE create_time >= NOW() - INTERVAL 10 MINUTE
               GROUP BY action_type, username ORDER BY action_type, username""")
rows = cur.fetchall(); print("  近10分钟审计:", rows)
actions_admin = {(a, u) for a, u, c in rows}
assert ("VIEW", "admin") in actions_admin, "统计/查询的 VIEW 应记真实用户 admin"
assert ("UPDATE", "admin") in actions_admin, "用户修改的 UPDATE 应记真实用户 admin"
assert not any(a == "VIEW" and u == "ANONYMOUS" for a, u, c in rows), "VIEW 不应再被误记为 ANONYMOUS"
conn.close()

print("\n== 六模块联调全部通过 ✅ ==")
