#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
教学原型库一键搭建工具（仅用于 nursing_ae_db 教学模拟库）
- 探测 127.0.0.1:3306 上的 MySQL 服务
- 优先用无密码 root 连接，失败再尝试常见本地密码
- 幂等：若 nursing_ae_db 已存在且 9 张表齐全，则跳过，不删数据
- 否则执行 01_init.sql 建库/建表/插字典数据，并校验
"""
import pymysql
import pathlib
import sys

SQL_PATH = pathlib.Path(__file__).resolve().parent / "01_init.sql"
HOST, PORT = "127.0.0.1", 3306
EXPECT_TABLES = [
    "sys_department", "dict_injury_level", "dict_event_type", "sys_user",
    "patient", "adverse_event", "event_factor", "rca_record", "sys_audit_log",
]

# 尝试的登录凭据（本地开发常见组合）；如都不行，脚本会提示你手动输入
CREDENTIALS = [("root", "123456"), ("root", ""), ("root", "root")]  # 本机探测: root@127.0.0.1 密码=123456; 其余为常见本地组合


def split_stmts(sql: str):
    """按分号拆分成可执行语句，跳过注释与空行。"""
    stmts, buf = [], ""
    for line in sql.splitlines():
        s = line.strip()
        if not s or s.startswith("--") or s.startswith("/*") or s.endswith("*/"):
            continue
        buf += line + "\n"
        if buf.rstrip().endswith(";"):
            stmt = buf.strip().rstrip(";").strip()
            if stmt:
                stmts.append(stmt)
            buf = ""
    if buf.strip():
        stmts.append(buf.strip().rstrip(";").strip())
    return stmts


def connect():
    last_err = None
    for user, pw in CREDENTIALS:
        try:
            conn = pymysql.connect(
                host=HOST, port=PORT, user=user, password=pw,
                charset="utf8mb4", connect_timeout=6, autocommit=False,
            )
            print(f"[OK] 已连接 MySQL：{user}@127.0.0.1:3306")
            return conn
        except pymysql.err.OperationalError as e:
            last_err = e
            print(f"[X] 凭据 {user}/{'<空>' if pw=='' else pw} 失败：{e}")
    return None


def main():
    if not SQL_PATH.exists():
        print("找不到 01_init.sql：", SQL_PATH)
        sys.exit(2)

    conn = connect()
    if conn is None:
        print("\n无法连接本地 MySQL。可能原因：root 有密码、或服务未启动。")
        print("请在本文件 CREDENTIALS 增加你的密码，或用 mysql 客户端手动执行 01_init.sql。")
        sys.exit(2)

    try:
        with conn.cursor() as cur:
            if "--rebuild" in sys.argv:
                print("[→] --rebuild：删除已存在的 nursing_ae_db 后重建（教学模拟库，无真实数据）")
                cur.execute("DROP DATABASE IF EXISTS nursing_ae_db")
                conn.commit()
            cur.execute("SELECT COUNT(*) FROM information_schema.schemata "
                        "WHERE schema_name='nursing_ae_db'")
            db_exists = cur.fetchone()[0] > 0

            if db_exists:
                cur.execute(
                    "SELECT COUNT(*) FROM information_schema.tables "
                    "WHERE table_schema='nursing_ae_db'")
                n = cur.fetchone()[0]
                if n >= len(EXPECT_TABLES):
                    print(f"[跳过] nursing_ae_db 已存在且含 {n} 张表，无需重建。")
                    return

            print("[→] 开始执行 01_init.sql（建库 + 建表 + 字典数据）...")
            sql = SQL_PATH.read_text(encoding="utf-8")
            stmts = split_stmts(sql)
            for i, st in enumerate(stmts, 1):
                # 打印进度（仅首行摘要）
                head = st.splitlines()[0].strip()[:60]
                cur.execute(st)
                print(f"  ({i}/{len(stmts)}) {head}")
            conn.commit()
            print("[✓] 01_init.sql 执行完成")

            # 校验
            cur.execute("USE nursing_ae_db")
            cur.execute("SHOW TABLES")
            tables = sorted(r[0] for r in cur.fetchall())
            print(f"[校验] 当前库表清单（{len(tables)}）：")
            for t in tables:
                print("   -", t)
            missing = [t for t in EXPECT_TABLES if t not in tables]
            if missing:
                print("[!] 缺失表：", missing)
                sys.exit(3)
            print("[✓] 9 张表全部就绪，数据库搭建成功。")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
