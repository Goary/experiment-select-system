-- 实验选课系统数据库脚本
-- 使用方法：mysql -uroot -p < schema.sql（自动建库 lab_select）

CREATE DATABASE IF NOT EXISTS lab_select DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE lab_select;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    employee_no VARCHAR(64)  NOT NULL COMMENT '工号/学号（登录名）',
    password    VARCHAR(128) NOT NULL COMMENT 'BCrypt 加密密码',
    username    VARCHAR(64)  NOT NULL COMMENT '姓名',
    role        VARCHAR(16)  NOT NULL COMMENT '角色：admin/teacher/student',
    class_name  VARCHAR(64)  DEFAULT NULL COMMENT '学生所属班级',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_employee_no (employee_no)
) ENGINE = InnoDB COMMENT '用户表（三角色统一）';

DROP TABLE IF EXISTS classroom;
CREATE TABLE classroom (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL COMMENT '教室名称，如 实验楼A-301',
    location    VARCHAR(128) DEFAULT NULL COMMENT '位置说明',
    capacity    INT          NOT NULL COMMENT '教室固定容量（即批次容量）',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1可用 0停用',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB COMMENT '教室/实验室资源';

DROP TABLE IF EXISTS course;
CREATE TABLE course (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(32)  NOT NULL COMMENT '课程编号',
    name        VARCHAR(128) NOT NULL COMMENT '课程/实验模块名称',
    description TEXT         DEFAULT NULL COMMENT '课程简介',
    credit      DECIMAL(3,1) DEFAULT NULL COMMENT '学分',
    creator_id  BIGINT       NOT NULL COMMENT '创建教师 sys_user.id',
    status      VARCHAR(16)  NOT NULL DEFAULT 'draft' COMMENT 'draft/published/archived',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_code (course_code),
    KEY idx_creator (creator_id)
) ENGINE = InnoDB COMMENT '课程（实验模块）';

DROP TABLE IF EXISTS course_batch;
CREATE TABLE course_batch (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id      BIGINT      NOT NULL COMMENT '所属课程',
    batch_no       VARCHAR(32) NOT NULL COMMENT '批次号，如 第1批',
    classroom_id   BIGINT      NOT NULL COMMENT '上课教室',
    capacity       INT         NOT NULL COMMENT '批次容量（取教室容量快照）',
    selected_count INT         NOT NULL DEFAULT 0 COMMENT '已选人数',
    select_open    TINYINT     NOT NULL DEFAULT 0 COMMENT '1开放选课 0关闭',
    status         VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT 'draft/published/closed',
    deleted        TINYINT     NOT NULL DEFAULT 0,
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_batch (course_id, batch_no),
    KEY idx_classroom (classroom_id)
) ENGINE = InnoDB COMMENT '课程批次';

DROP TABLE IF EXISTS batch_session;
CREATE TABLE batch_session (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id    BIGINT   NOT NULL COMMENT '所属批次 id',
    session_no  INT      NOT NULL COMMENT '课次序号，从1开始',
    start_time  DATETIME NOT NULL COMMENT '本次上课开始时间',
    end_time    DATETIME NOT NULL COMMENT '本次上课结束时间',
    deleted     TINYINT  NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_batch_session (batch_id, session_no),
    KEY idx_batch_time (batch_id, start_time, end_time),
    KEY idx_session_time (start_time, end_time)
) ENGINE = InnoDB COMMENT '批次课次安排';

DROP TABLE IF EXISTS batch_teacher;
CREATE TABLE batch_teacher (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id   BIGINT NOT NULL COMMENT '批次 id',
    teacher_id BIGINT NOT NULL COMMENT '授课教师 sys_user.id',
    deleted    TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_batch_teacher (batch_id, teacher_id),
    KEY idx_teacher (teacher_id)
) ENGINE = InnoDB COMMENT '批次-授课教师关联';

DROP TABLE IF EXISTS course_selection;
CREATE TABLE course_selection (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id  BIGINT      NOT NULL COMMENT '学生 sys_user.id',
    course_id   BIGINT      NOT NULL COMMENT '课程 id（冗余）',
    batch_id    BIGINT      NOT NULL COMMENT '批次 id',
    select_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
    status      VARCHAR(16) NOT NULL DEFAULT 'selected' COMMENT 'selected/dropped',
    deleted     TINYINT     NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_student (student_id, status),
    KEY idx_batch (batch_id, status),
    KEY idx_course (course_id, status)
) ENGINE = InnoDB COMMENT '学生选课记录';

SET FOREIGN_KEY_CHECKS = 1;

-- 初始数据：超管账号 admin / admin123（BCrypt）
INSERT INTO sys_user (employee_no, password, username, role)
VALUES ('admin', '$2a$10$Licad57LDxBcx/EK3.4Ji.nSDixZQkoUb9LPRUSzJlQWZaHQPA7lq', '系统管理员', 'admin');

-- 示例教室
INSERT INTO classroom (name, location, capacity) VALUES
('实验楼A-301', '实验楼A座3层', 40),
('实验楼A-302', '实验楼A座3层', 40),
('实验楼B-201', '实验楼B座2层', 60);
