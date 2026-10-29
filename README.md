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

### 后端

```bash
# 准备 MySQL，创建数据库
mysql -uroot -p -e "CREATE DATABASE lab_select DEFAULT CHARSET utf8mb4;"
mysql -uroot -p lab_select < backend/src/main/resources/schema.sql

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
