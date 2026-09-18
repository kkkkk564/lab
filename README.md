# 校园实验室资源预约与状态监控系统

基于 **Spring Boot 3 + Vue 3 前后端分离架构**的全栈项目。

## 一、技术栈

| 端 | 技术 |
|---|---|
| 后端 | Java 17 · Spring Boot 3.2 · Spring Web / Data JPA / Validation · JWT (jjwt 0.11) · **MySQL 8**（数据库 `lab_system`，utf8mb4） |
| 前端 | Vue 3.5 · Vite 6 · Element Plus · ECharts 5 · Pinia · Vue Router · Axios |
| 通信 | REST JSON · Bearer Token · 前端 Vite 代理 `/api` → 后端 8080 |

## 二、功能模块

- **登录认证**：JWT（24h 有效）+ RBAC 三角色（学生 / 教师 / 管理员），路由守卫按角色控制菜单
- **实时监控大屏**：8 项全局指标、近 14 天预约趋势、状态分布环形图、实验室使用时长排行、实验室实时状态卡片（空闲 / 使用中 / 即将开始 / 维护中 / 已关闭），30 秒自动刷新
- **实验室预约**：条件筛选浏览、详情抽屉（含当日时刻表）、预约提交（开放时段 / 容量 / 日期 / 时段冲突四重校验）
- **预约审批**（管理员）：通过 / 驳回（附意见），审批时二次冲突检测
- **我的预约**：状态筛选、未开始的预约可取消
- **设备管理**：台账 CRUD、师生报修、管理员处理修复，状态流转（正常→故障→维修中→正常）
- **实验室管理**（管理员）：CRUD、开放状态切换（开放 / 维护 / 关闭）
- **公告通知**：置顶排序，管理员发布 / 编辑 / 删除
- **用户管理**（管理员）：CRUD、停用启用、密码重置
- **个人中心**：资料展示、修改密码

## 三、目录结构

```
lab-system/
├── backend/                          # Spring Boot 后端（独立工程）
│   ├── pom.xml
│   └── src/main/java/com/campus/lab/
│       ├── LabSystemApplication.java # 启动类
│       ├── config/                   # JWT 拦截器、CORS 与拦截器注册
│       ├── common/                   # 统一响应 Result、业务异常、全局异常处理
│       ├── util/                     # JwtUtil、PasswordUtil(SHA-256 加盐)、Auths
│       ├── entity/                   # 5 个 JPA 实体
│       ├── repository/               # 5 个 Spring Data 接口
│       ├── dto/                      # 请求 / 响应记录类（Java record）
│       ├── service/                  # 业务层 + DataSeeder 种子数据
│       └── controller/               # 7 组 REST 控制器
│   └── src/main/resources/application.yml
└── frontend/                         # Vue 3 前端（独立工程）
    └── src/
        ├── api/index.js              # axios 封装 + 全部接口
        ├── store/auth.js             # Pinia 登录态
        ├── router/index.js           # 路由 + 权限守卫
        ├── layouts/MainLayout.vue    # 侧边栏主布局
        └── views/                    # 10 个页面（含监控大屏）
```

## 四、快速启动

### 1. 数据库

```sql
CREATE DATABASE IF NOT EXISTS lab_system CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

账号与地址在 `backend/src/main/resources/application.yml` 中配置（当前为 localhost:3306 / root）。

### 2. 后端（需 JDK 17+）

```bash
cd backend
mvn package -DskipTests
java -jar target/lab-system-1.0.0.jar
# 启动于 http://localhost:8080
# 首次启动由 JPA 自动建表并写入演示数据（sys_user/lab/device/reservation/announcement）
```

### 3. 前端（需 Node 18+）

```bash
cd frontend
npm install
npm run dev
# 启动于 http://localhost:5173，/api 自动代理到 8080
```

### 3. 演示账号（密码均为 `123456`）

| 账号 | 角色 | 能力 |
|---|---|---|
| admin | 管理员 | 审批、实验室 / 设备 / 公告 / 用户管理 |
| teacher1 / teacher2 | 教师 | 预约、报修、查看监控 |
| student1 ~ student6 | 学生 | 预约、报修、查看监控 |

## 五、主要 REST API（前缀 /api）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /auth/login | 登录获取 token |
| GET | /stats/overview · /trends · /lab-usage · /status-distribution | 监控大屏数据 |
| GET | /labs · /labs/{id} · /labs/{id}/timetable?date= | 实验室列表 / 详情 / 当日时刻表 |
| POST | /reservations | 提交预约（冲突检测） |
| PUT | /reservations/{id}/cancel · /{id}/review | 取消 / 审批 |
| GET | /devices · POST /devices/{id}/report · PUT /{id}/repair | 设备台账 / 报修 / 修复 |
| CRUD | /labs /devices /announcements /users | 管理员维护接口 |

统一响应格式：`{ code: 0, msg: "ok", data: ... }`；未认证返回 HTTP 401。

## 六、切换回嵌入式 H2（可选）

如需零依赖演示，`application.yml` 底部注释给出了 H2 的 datasource 配置；同时在 `pom.xml` 中把 `mysql-connector-j` 换回 `com.h2database:h2` 依赖即可，其余代码无需改动。

## 七、说明

- 密码采用 SHA-256 加盐哈希（演示级），生产环境建议替换为 BCrypt 并启用 HTTPS
- 种子数据含近 14 天历史预约，保证监控大屏的趋势图、排行、状态分布开箱即有数据
