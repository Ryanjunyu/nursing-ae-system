#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""后端接口自动化冒烟测试（验证用户权限模块 + 事件填报表单模块）"""
import json
import time
import urllib.request
import urllib.error
import pymysql

BASE = "http://127.0.0.1:8080"


def post(path, data, token=None):
    req = urllib.request.Request(BASE + path, data=json.dumps(data).encode(),
                                 method="POST")
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
    with urllib.request.urlopen(req, timeout=10) as r:
        return r.status, json.loads(r.read())


def login(username):
    s, d = post("/api/auth/login", {"username": username, "password": "123456"})
    assert s == 200 and d.get("code") == 200, f"{username} 登录失败: {s} {d}"
    return d["data"]["token"]


print("== 等待后端启动 ==")
up = False
for _ in range(50):
    try:
        s, _ = get("/api/dict/injury-levels")
        if s in (200, 401):
            up = True
            print("  后端已就绪 (HTTP %d)" % s)
            break
    except Exception:
        pass
    time.sleep(2)
if not up:
    print("!! 后端未在 100s 内就绪，终止")
    raise SystemExit(1)

print("\n== 1) 登录与字典 ==")
token_admin = login("admin")
print("  admin 登录成功, token 长度", len(token_admin))
s, d = get("/api/dict/departments", token_admin)
print("  科室字典:", [x["deptName"] for x in d["data"]])
s, d = get("/api/dict/injury-levels", token_admin)
print("  损伤分级:", [x["levelName"] for x in d["data"]])

print("\n== 2) 管理员填报（实名）==")
s, d = post("/api/events", {
    "occurTime": "2026-10-20 10:00:00", "deptId": 1, "eventType": "FALL",
    "injuryLevel": "II", "eventDesc": "患者自行下床跌倒", "sceneDesc": "地面湿滑",
    "isAnonymous": 0, "patientMaskedName": "张*"
}, token_admin)
print("  返回:", s, d)

print("\n== 3) 护士长(head1, 内科一病区)列出事件 ==")
token_head = login("head1")
s, d = get("/api/events", token_head)
print("  head1 可见事件数:", d["data"]["total"])

print("\n== 4) 护士(nurse1)匿名填报 ==")
token_nurse = login("nurse1")
s, d = post("/api/events", {
    "occurTime": "2026-10-20 11:00:00", "deptId": 1, "eventType": "PRESSURE",
    "injuryLevel": "III", "eventDesc": "长期卧床出现压疮", "isAnonymous": 1
}, token_nurse)
print("  匿名填报返回:", s, d)
anon_event_id = d.get("data")

print("\n== 5) 数据范围隔离校验 ==")
s, d = get("/api/events", token_nurse)
nurse_rows = d["data"]["records"]
print("  nurse1 可见事件:", [(x["eventTypeName"], x["reporterDisplay"]) for x in nurse_rows])
# 越权：nurse2 属内科二病区(deptId=2)，尝试看内科一病区的事件应被隔离
token_nurse2 = login("nurse2")
s, d = get("/api/events", token_nurse2)
print("  nurse2(二病区) 可见事件数:", d["data"]["total"], "（应为仅二病区，不含一病区事件）")

print("\n== 6) 越权访问明细校验 ==")
# 取一个一病区事件 id（nurse1 可见的）
one_dept_event = nurse_rows[0]["eventId"] if nurse_rows else None
if one_dept_event:
    s, d = get("/api/events/%d" % one_dept_event, token_nurse2)
    print("  nurse2 访问一病区事件明细:", s, d.get("message", ""), "（403 即隔离生效）")

print("\n== 7) 审计日志校验（直接查库）==")
conn = pymysql.connect(host="127.0.0.1", port=3306, user="root", password="123456",
                       database="nursing_ae_db", charset="utf8mb4")
cur = conn.cursor()
cur.execute("SELECT action_type, username, COUNT(*) c FROM sys_audit_log "
            "GROUP BY action_type, username ORDER BY c DESC")
print("  审计分组:", cur.fetchall())
cur.execute("SELECT COUNT(*) FROM sys_audit_log WHERE username='ANONYMOUS'")
print("  匿名操作审计条数:", cur.fetchone()[0], "（匿名填报应>=1）")
conn.close()

print("\n== 全部冒烟测试完成 ==")
