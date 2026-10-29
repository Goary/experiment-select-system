# 实验选课系统（experiment-select-system）

高校实验选课系统，最小闭环版本。角色：超级管理员 / 教师 / 学生。

详细设计见 [docs/设计方案.md](docs/设计方案.md)。

## 技术栈

- 后端：Spring Boot 3 + MyBatis-Plus + MySQL 8 + JWT（JDK 17+，Maven）
- 前端：Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios

## 目录结构

```
backend/    Spring Boot 后端工程（端口 8080）
frontend/   Vue3 前端工程（端口 5173）
docs/       设计文档
```

## 快速启动

### 数据库（MySQL 8，系统服务）

```bash
# 安装（Ubuntu）
sudo apt update && sudo apt install -y mysql-server

# 设置 root 密码为 root（与 application.yml 一致）
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH caching_sha2_password BY 'root'; FLUSH PRIVILEGES;"

# 初始化数据库与表（首次）
mysql -h127.0.0.1 -uroot -proot < backend/src/main/resources/schema.sql

# 服务管理
sudo systemctl start mysql    # 启动
sudo systemctl stop mysql     # 停止
```

### 后端

```bash

# 修改 backend/src/main/resources/application.yml 中的数据源账号密码后启动
cd backend && mvn spring-boot:run
```

### 前端

```bash
cd frontend && npm install && npm run dev
```

## 默认账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 超级管理员 | admin | admin123 |
