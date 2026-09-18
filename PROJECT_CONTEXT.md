# 项目上下文：校园实验室资源预约与状态监控系统

> 用途：新会话/新对话接手时，将本文件内容作为上下文即可快速恢复对项目的完整理解。

## 1. 项目位置与技术栈

- 根目录：`D:\explore\web\lab-system\`（前后端分离，两个独立工程）
- **后端** `backend/`：Java 17 + Spring Boot 3.2.5 + Spring Data JPA + MySQL 8 + JWT(jjwt 0.11.5) + SHA-256加盐密码
- **前端** `frontend/`：Vue 3.5 + Vite 6 + Element Plus + ECharts 5 + Pinia + Vue Router + Axios
- 数据库：MySQL 8.0.26 @ 127.0.0.1:3306，库名 `lab_system`（utf8mb4），root 密码见 `backend/src/main/resources/application.yml`
- 后端 8080 / 前端 5173；前端 Vite 代理 `/api` → 8080

## 2. 目录结构与职责

```
lab-system/
├── backend/src/main/java/com/campus/lab/
│   ├── LabSystemApplication.java        # 启动类
│   ├── config/      WebConfig(CORS+拦截器注册)、JwtInterceptor(TOKEN校验注入userId/role/userName)
│   ├── common/      Result(统一响应{code,msg,data})、BizException、GlobalExceptionHandler
│   ├── util/        JwtUtil、PasswordUtil(SHA-256+盐)、Auths(取当前用户/requireAdmin)
│   ├── entity/      User(sys_user)、Lab、Device、Reservation、Announcement（Reservation/Device 声明了 @Table indexes）
│   ├── repository/  5 个 JPA 接口；LabRepository.findByIdForUpdate、ReservationRepository.findByIdForUpdate/
│   │                findActiveByLabAndDateForUpdate（@Lock(PESSIMISTIC_WRITE) 防双占位）、countByLabId/countByUserId
│   ├── dto/         record：LoginRequest/Response、UserInfo、ReservationCreateRequest、ReviewRequest、
│   │                ReservationDTO(含displayStatus已完derived)、LabDTO、DeviceDTO、Overview/TrendPoint/LabUsage/StatusCount 等
│   ├── service/     Auth/UserService/LabService(含currentStatus状态引擎)/ReservationService(锁+冲突检测)/
│   │                DeviceService/AnnouncementService/StatsService/DataSeeder(首次启动种子数据)
│   └── controller/  auth/labs/devices/reservations/announcements/stats/users 七组 REST
└── frontend/src/
    ├── api/index.js       axios 封装（token 注入、统一 code!=0 报错、401 跳登录）+ 全部接口方法
    ├── store/auth.js      Pinia 登录态（localStorage: lab_token/lab_user）
    ├── router/index.js    路由+守卫（meta.roles 限 ADMIN：approvals/lab-manage/users）
    ├── layouts/MainLayout.vue   石墨靛蓝侧栏+琥珀竖条选中态+毛玻璃顶栏+实时时钟+主题化用户下拉(user-pop)
    └── views/             Login/Monitor(监控大屏)/Labs(预约+详情抽屉+时刻表)/MyReservations/Approvals/
                           Devices(报修+故障详情弹窗)/LabManage(状态下拉status-pop)/Announcements(点击弹详情)/Users/Profile
```

## 3. 核心功能与约束

- **角色**：STUDENT/TEACHER（预约、报修）/ ADMIN（审批、实验室/设备/公告/用户管理）；JWT 24h
- **预约校验链**：实验室 OPEN、日期≥今天、开始>当前时间(当天)、时段在开放时间内、人数≤容量、时段冲突检测（PENDING+APPROVED 均占位，Java 层重叠判断）
- **并发安全**：create/review 统一"先锁实验室行、再锁预约行"（SELECT...FOR UPDATE），冲突检测用加锁读；已实测并发双订 1 成功 1 拦截
- **一致性**：3 外键(fk_reservation_lab/user、fk_device_lab)；删除守卫=全量 count 校验（禁止删有预约/设备的实验室、有预约的用户）
- **状态引擎**：实验室实时状态=维护中/已关闭/使用中(覆盖当前时刻)/即将开始(60min内)/空闲；APPROVED 且结束时间已过→displayStatus=FINISHED
- 统一响应 `{code:0,msg,data}`，401 返回 HTTP 401

## 4. 关键环境事实（新会话必读，踩过坑）

1. bash 每次**必须先** `export PATH="/c/Users/26303/.workbuddy/binaries/PortableGit/versions/1.2.0/usr/bin:/c/Users/26303/.workbuddy/binaries/PortableGit/versions/1.2.0/bin:$PATH"`
2. JDK 17.0.12 在 `D:\an2\Installation Package\JDK`（不在 PATH）
3. Maven 3.9.16 在 `D:\explore\web\.tools\apache-maven-3.9.16`；构建命令：
   `cd /d/explore/web/lab-system/backend && bash /d/explore/web/.tools/mvn.sh -s D:/explore/web/.tools/maven-settings.xml -DskipTests package`
   （mvn.sh=直接 java 调 classworlds；settings 配阿里云镜像；本机旧 Maven 3.5 不可用）
4. **沙箱注入 SERVER__PORT=12396** 会覆盖 Spring 端口，启动必须加 `--server.port=8080 --server.address=127.0.0.1`
5. Windows node.exe 跑脚本必须用 `C:/...` 风格路径（不是 /c/...）：
   - 前端启动：`cd frontend && "C:/Users/26303/.workbuddy/binaries/node/versions/22.22.2-3/node.exe" "D:/explore/web/lab-system/frontend/node_modules/vite/bin/vite.js" --host 127.0.0.1`
6. **重新打包前必须停掉旧后端进程**（Windows jar 文件锁导致 repackage 失败）
7. MySQL JDBC URL 需 `useSSL=false&allowPublicKeyRetrieval=true`；本机 MySQL 服务已在运行(3306)

## 5. 已完成

- 后端全部功能 + MySQL 建库建表 + 种子数据（9用户/6实验室/24设备/40+预约/4公告，含 24h 开放的"创新实践基地"LAB-302）
- 前端 10 页全部完成；后端已实测：登录、大屏、列表、预约冲突、并发锁、删除守卫全过
- 数据库加固：6 索引(EXPLAIN 验证走 idx_reservation_lab_date_status)+3 外键+全量删除校验
- **UI 已完成 v3 设计系统**：主色电光靛蓝 #4F46E5 + 活力青 #06B6D4(渐变搭档) + 琥珀 #F59E0B(强调/kicker/焦点环)，背景 #F5F7FF，文字 #0F1B33/#5E6C8A，侧栏靛蓝渍染渐变(#10162E→#1A2150)；EP 语义色换高饱和 Tailwind 系(success #10B981/warning #F59E0B/danger #F43F5E)；全套 token 在 styles.css :root
- UI 细节：kicker 小标签、实验室卡片编号水印、rise-in 交错入场、脉冲状态点、表格全 v-loading、操作列统一 plain 主题按钮(op-cell 弹性居中)、主题化下拉弹层(user-pop/status-pop)、公告点击弹详情、故障详情弹窗、焦点环已对 EP 表单控件豁免(无黄框)
- README.md 已同步

## 6. 未解决/后续待办

1. 时段存 VARCHAR("HH:mm")：DB 层无法做时间约束与重叠检查（应用层已兜底），理想方案是改 TIME 或区间表
2. 密码 SHA-256+盐（演示级）→ 生产应换 BCrypt
3. ddl-auto=update → 生产应上 Flyway/Liquibase
4. 无软删除/审计日志（reviewed_by 会被覆盖）；预约无归档策略（量大后建议归档表/分区）
5. 实验室/用户删除是硬拒绝（未提供级联或转移）
6. npm run build 生产构建未验证过；未做 WebSocket（现为 30s 轮询，Labs 页轮询已静默化）
7. 演示账号：admin / teacher1 / teacher2 / student1~6，密码均 123456

## 7. 启动命令速查

```bash
# 后端
cd /d/explore/web/lab-system/backend
"/d/an2/Installation Package/JDK/bin/java.exe" -jar target/lab-system-1.0.0.jar --server.port=8080 --server.address=127.0.0.1
# 前端
cd /d/explore/web/lab-system/frontend
"C:/Users/26303/.workbuddy/binaries/node/versions/22.22.2-3/node.exe" "D:/explore/web/lab-system/frontend/node_modules/vite/bin/vite.js" --host 127.0.0.1
```
