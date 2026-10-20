-- =====================================================================
-- 住院护理不良事件上报与根因辅助分析原型系统 —— 建库脚本
-- 数据库：MySQL 8.0
-- 说明：本系统仅用于教学模拟，不连接任何真实临床数据；
--       所有患者信息均为模拟数据，且按隐私保护要求脱敏存储。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS nursing_ae_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE nursing_ae_db;

-- ---------------------------------------------------------------------
-- 1. 科室字典表（支撑按科室隔离与统计）
-- ---------------------------------------------------------------------
CREATE TABLE sys_department (
  dept_id     INT          PRIMARY KEY AUTO_INCREMENT COMMENT '科室ID',
  dept_code   VARCHAR(32)  NOT NULL UNIQUE COMMENT '科室编码',
  dept_name   VARCHAR(64)  NOT NULL COMMENT '科室名称',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室字典表';

-- ---------------------------------------------------------------------
-- 2. 事件损伤分级字典表（中国医院协会四级标准）
-- ---------------------------------------------------------------------
CREATE TABLE dict_injury_level (
  level_code CHAR(3)      PRIMARY KEY COMMENT '分级代码 I/II/III/IV',
  level_name VARCHAR(32)  NOT NULL COMMENT '分级名称',
  level_desc VARCHAR(255) DEFAULT NULL COMMENT '分级说明',
  sort_no    INT          NOT NULL DEFAULT 0 COMMENT '展示排序'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件损伤分级字典表';

-- ---------------------------------------------------------------------
-- 3. 不良事件类型字典表（聚焦跌倒/压疮/输液外渗/坠床等）
-- ---------------------------------------------------------------------
CREATE TABLE dict_event_type (
  type_id    INT          PRIMARY KEY AUTO_INCREMENT,
  type_code  VARCHAR(32)  NOT NULL UNIQUE COMMENT '类型编码',
  type_name  VARCHAR(64)  NOT NULL COMMENT '类型名称',
  category   VARCHAR(32)  DEFAULT NULL COMMENT '所属大类',
  sort_no    INT          NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不良事件类型字典表';

-- ---------------------------------------------------------------------
-- 4. 系统用户表（护士 / 护士长 / 管理员 三角色）
-- ---------------------------------------------------------------------
CREATE TABLE sys_user (
  user_id     INT          PRIMARY KEY AUTO_INCREMENT,
  username    VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录账号',
  password    VARCHAR(128) NOT NULL COMMENT '密码（BCrypt 加密存储）',
  real_name   VARCHAR(32)  DEFAULT NULL COMMENT '真实姓名',
  role        VARCHAR(20)  NOT NULL COMMENT 'NURSE/HEAD_NURSE/ADMIN',
  dept_id     INT          DEFAULT NULL COMMENT '所属科室（管理员可为NULL）',
  phone       VARCHAR(20)  DEFAULT NULL,
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (dept_id) REFERENCES sys_department(dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ---------------------------------------------------------------------
-- 5. 模拟患者信息表（脱敏存储，仅教学模拟）
-- ---------------------------------------------------------------------
CREATE TABLE patient (
  patient_id    INT          PRIMARY KEY AUTO_INCREMENT,
  masked_name   VARCHAR(32)  DEFAULT NULL COMMENT '脱敏姓名 如 张*',
  gender        VARCHAR(4)   DEFAULT NULL COMMENT '男/女',
  age           INT          DEFAULT NULL COMMENT '年龄',
  bed_no        VARCHAR(16)  DEFAULT NULL COMMENT '床号',
  admission_no  VARCHAR(32)  DEFAULT NULL COMMENT '住院号（脱敏）',
  dept_id       INT          DEFAULT NULL COMMENT '所在科室',
  diagnosis     VARCHAR(255) DEFAULT NULL COMMENT '主要诊断',
  id_no_masked  VARCHAR(32)  DEFAULT NULL COMMENT '身份证号脱敏（如 110***********1234）',
  FOREIGN KEY (dept_id) REFERENCES sys_department(dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟患者信息表（教学模拟，脱敏）';

-- ---------------------------------------------------------------------
-- 6. 不良事件主表
-- ---------------------------------------------------------------------
CREATE TABLE adverse_event (
  event_id      INT          PRIMARY KEY AUTO_INCREMENT,
  event_no      VARCHAR(32)  NOT NULL UNIQUE COMMENT '事件编号（系统生成）',
  reporter_id   INT          DEFAULT NULL COMMENT '上报人（匿名时为NULL）',
  reporter_role VARCHAR(20)  DEFAULT NULL COMMENT '上报人角色快照',
  is_anonymous  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否匿名 1匿名 0实名',
  dept_id       INT          NOT NULL COMMENT '事件发生科室',
  patient_id    INT          DEFAULT NULL COMMENT '关联患者（可空）',
  event_type    VARCHAR(32)  NOT NULL COMMENT '事件类型编码',
  injury_level  CHAR(3)      NOT NULL COMMENT '损伤分级 I/II/III/IV',
  occur_time    DATETIME     NOT NULL COMMENT '事件发生时间',
  event_desc    TEXT         NOT NULL COMMENT '事件经过',
  scene_desc    TEXT         DEFAULT NULL COMMENT '现场情况',
  status        VARCHAR(20)  NOT NULL DEFAULT 'REPORTED' COMMENT 'REPORTED/REVIEWING/ANALYZED/CLOSED',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (dept_id)      REFERENCES sys_department(dept_id),
  FOREIGN KEY (patient_id)   REFERENCES patient(patient_id),
  FOREIGN KEY (reporter_id)  REFERENCES sys_user(user_id),
  FOREIGN KEY (injury_level) REFERENCES dict_injury_level(level_code),
  FOREIGN KEY (event_type)   REFERENCES dict_event_type(type_code),
  INDEX idx_dept (dept_id),
  INDEX idx_type (event_type),
  INDEX idx_level (injury_level),
  INDEX idx_occur (occur_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不良事件主表';

-- ---------------------------------------------------------------------
-- 7. 事件诱因记录表（人-机-料-法-环 维度诱因采集）
-- ---------------------------------------------------------------------
CREATE TABLE event_factor (
  factor_id      INT          PRIMARY KEY AUTO_INCREMENT,
  event_id       INT          NOT NULL COMMENT '关联事件',
  dimension      VARCHAR(8)   NOT NULL COMMENT '人/机/料/法/环',
  factor_type    VARCHAR(16)  DEFAULT 'PROXIMAL' COMMENT 'PROXIMAL近端/ROOT根本',
  factor_content TEXT         NOT NULL COMMENT '诱因描述',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (event_id) REFERENCES adverse_event(event_id),
  INDEX idx_event (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件诱因记录表';

-- ---------------------------------------------------------------------
-- 8. RCA 根因分析记录表（5维度根因 + 时间序列表 + 改进措施）
-- ---------------------------------------------------------------------
CREATE TABLE rca_record (
  rca_id           INT          PRIMARY KEY AUTO_INCREMENT,
  event_id         INT          NOT NULL COMMENT '关联事件',
  analyst_id       INT          DEFAULT NULL COMMENT '分析人（护士长/管理员）',
  timeline         TEXT         DEFAULT NULL COMMENT '时间序列表（还原事件经过）',
  root_human       TEXT         DEFAULT NULL COMMENT '人-根本原因',
  root_machine     TEXT         DEFAULT NULL COMMENT '机-根本原因',
  root_material    TEXT         DEFAULT NULL COMMENT '料-根本原因',
  root_method      TEXT         DEFAULT NULL COMMENT '法-根本原因',
  root_env        TEXT         DEFAULT NULL COMMENT '环-根本原因',
  improvement      TEXT         DEFAULT NULL COMMENT '改进措施',
  analysis_status  VARCHAR(20)  NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'NOT_STARTED/ANALYZING/DONE',
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (event_id)  REFERENCES adverse_event(event_id),
  FOREIGN KEY (analyst_id) REFERENCES sys_user(user_id),
  INDEX idx_event (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RCA根因分析记录表';

-- ---------------------------------------------------------------------
-- 9. 系统操作审计日志表（填报/查看/修改/删除/导出 全留痕）
-- ---------------------------------------------------------------------
CREATE TABLE sys_audit_log (
  log_id      BIGINT       PRIMARY KEY AUTO_INCREMENT,
  user_id     INT          DEFAULT NULL COMMENT '操作人（匿名操作记 0）',
  username    VARCHAR(64)  DEFAULT NULL COMMENT '操作人账号（匿名记 ANONYMOUS）',
  role        VARCHAR(20)  DEFAULT NULL COMMENT '操作人角色',
  dept_id     INT          DEFAULT NULL COMMENT '操作人科室',
  action_type VARCHAR(32)  NOT NULL COMMENT 'LOGIN/REPORT/VIEW/UPDATE/DELETE/EXPORT',
  target_type VARCHAR(32)  DEFAULT NULL COMMENT '事件/患者/用户/字典',
  target_id   VARCHAR(32)  DEFAULT NULL COMMENT '操作对象ID',
  req_param   TEXT         DEFAULT NULL COMMENT '请求参数（敏感字段已脱敏）',
  ip_addr     VARCHAR(64)  DEFAULT NULL COMMENT '客户端IP',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user   (user_id),
  INDEX idx_action (action_type),
  INDEX idx_time   (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作审计日志表';

-- =====================================================================
-- 初始化字典数据（教学模拟示例）
-- =====================================================================

-- 损伤分级（中国医院协会四级）
INSERT INTO dict_injury_level (level_code, level_name, level_desc, sort_no) VALUES
  ('I',   '警告事件（Ⅰ级）', '非预期的死亡，或非疾病自然进展中造成永久性功能丧失', 1),
  ('II',  '不良后果事件（Ⅱ级）', '诊疗活动中因诊疗而非疾病本身造成患者机体与功能损害', 2),
  ('III', '未造成后果事件（Ⅲ级）', '发生错误事实，但未造成机体与功能损害', 3),
  ('IV',  '隐患事件（Ⅳ级）', '及时发现错误，未形成事实（近似错误/Near-miss）', 4);

-- 事件类型（聚焦题目四类，并补充常见类型）
INSERT INTO dict_event_type (type_code, type_name, category, sort_no) VALUES
  ('FALL',     '跌倒',       '意外事件', 1),
  ('PRESSURE', '压疮',       '皮肤护理', 2),
  ('INFIL',    '输液外渗',   '静脉治疗', 3),
  ('FALL_BED', '坠床',       '意外事件', 4),
  ('WRONG_DRUG','给药错误',  '不良治疗', 5),
  ('TUBE',     '管路滑脱',   '管道护理', 6),
  ('OTHER',    '其他',       '其他',     99);

-- 科室
INSERT INTO sys_department (dept_code, dept_name) VALUES
  ('N-01', '内科一病区'),
  ('N-02', '内科二病区'),
  ('S-01', '外科一病区'),
  ('ICU',  '重症医学科'),
  ('ADMIN','护理部（管理）');
