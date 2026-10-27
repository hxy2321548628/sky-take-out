# 苍穹外卖 · 云原生改造

基于黑马程序员《苍穹外卖》课程项目的**学习型重构**：业务代码保留原课程结构，重点改造工程结构与交付方式——用 Docker Compose 跑起整套本地环境、用 Flyway 管理表结构与种子数据、用 Actuator 暴露健康探针、镜像通过环境变量适配不同环境，并用 Taskfile 收敛所有常用命令。

> 当前进度：**员工登录 / 登出**已迁移到重构后的骨架；MySQL、Redis、MinIO 等基础设施与前后端镜像已打通。其余业务模块（分类、菜品、套餐、订单、报表等）按课程章节逐步补齐，DTO / VO / 实体已在 `sky-pojo` 中预留。

## 技术栈

| 层次 | 选型 |
| --- | --- |
| 后端 | Java 17、Spring Boot 2.7.3、MyBatis + PageHelper、Druid、JWT、Knife4j（Springfox 3.0）、Actuator |
| 数据 | MySQL 8（Flyway 迁移）、Redis 7、MinIO（本地替代阿里云 OSS，待接入业务代码） |
| 前端 | Vue 2.6 + TypeScript + Element UI + Vue CLI（Node 16）、Nginx 托管 |
| 工程 | Maven Wrapper（3.9.9）、Docker / Compose v2、Taskfile（兼容 `make`）、阿里云 Maven 镜像 |

## 目录结构

```text
.
├── backend/                     # Maven 多模块后端
│   ├── mvnw / mvnw.cmd          # Maven Wrapper，本机构建无需预装 Maven
│   ├── .mvn/                    # wrapper 配置 + settings.xml（阿里云镜像）
│   ├── sky-common/              # 常量、异常、JWT / OSS 工具、通用返回结构
│   ├── sky-pojo/                # entity / DTO / VO
│   └── sky-server/              # 启动模块：Controller / Service / Mapper
│       └── src/main/resources/
│           ├── application*.yml # dev（本机 IDE）/ docker（容器）两套 profile
│           └── db/migration/    # Flyway：V1 建表、V2 种子数据
├── frontend/admin-web/          # 管理端（Vue 2 + TS + Element UI）
│   ├── nginx/                   # 运行时模板：/api 反代到后端
│   └── Dockerfile               # Node 16 构建 → Nginx 托管
├── deploy/
│   ├── compose/                 # 本地开发编排（mysql / redis / minio / 应用）
│   └── k8s/                     # K8s 清单占位（后续）
├── docs/
│   ├── api/                     # 接口文档（YApi 导出）
│   └── design/                  # 数据库设计、原型
├── Makefile                     # 转发到 Taskfile
├── Taskfile.yml                 # 统一命令入口
└── .env.example                 # 本地环境变量模板
```

## 快速开始

### 前置条件

- Docker 与 Docker Compose v2
- [Task](https://taskfile.dev/)（macOS：`brew install go-task`；也可直接 `make <target>`，Makefile 只做转发）
- 可选：JDK 17（IDE 里本机跑后端 / `task build` 时用）、Node 16（本机跑管理端开发服务器时用）、jq（`task smoke` 用）

### 启动

```bash
cp .env.example .env          # 按需修改密码与 JWT 密钥
task doctor                   # 检查 docker / compose / buildx
task up                       # 构建镜像并启动 mysql、redis、minio、后端、管理端
```

首次启动时后端会由 Flyway 自动建表并写入课程种子数据，随后可用内置管理员登录。

| 入口 | 地址 | 说明 |
| --- | --- | --- |
| 管理端 | http://localhost:8888 | 账号 `admin` / 密码 `123456` |
| 后端 API | http://localhost:8080 | 管理端请求经 Nginx 的 `/api` 反代 |
| 健康检查 | http://localhost:8080/actuator/health | Docker / Compose 探针同用 |
| 接口文档 | http://localhost:8080/doc.html | Knife4j UI |
| MinIO 控制台 | http://localhost:9001 | 账号密码见 `.env`（默认 `minioadmin`） |

端口、数据库名、JWT 密钥等都在 `.env` 中配置，Compose 与 Taskfile 共用这一份文件。

### 常用命令

```bash
task --list          # 查看全部命令
task up              # 启动全部服务（含构建镜像）
task up:infra        # 只启动 mysql / redis / minio（后端在 IDE 里跑时用）
task fe:docker       # Docker 启动管理端，/api 反代到本机后端
task ps              # 查看容器与健康状态
task logs            # 跟踪后端日志；task logs -- admin-web 可切换服务
task down            # 停止服务（保留数据卷）
task restart         # 重建并重启后端与管理端
task db:migrations   # 查看 Flyway 迁移记录
task db:reset        # 删除数据卷并重新初始化（会丢数据）
task db:shell        # 进入 MySQL 命令行
task smoke           # 冒烟：调用管理员登录接口
task smoke:health    # 冒烟：检查健康端点
```

## 本机开发

不把应用跑在容器里时，基础设施仍交给 Compose：

```bash
task up:infra        # mysql :3306、redis :6379、minio :9000
```

后端：

- IDE 中直接运行 `com.sky.SkyApplication`（默认 `dev` profile，连 `localhost`，账号 `root/root`）。
- 命令行构建：`task build`。会自动定位 JDK 17，并统一走 `backend/mvnw`（Maven 3.9.9），**不需要本机预装 Maven**；wrapper 首次运行会从阿里云下载 Maven，之后使用本地缓存。

管理端：

```bash
task fe:install      # 安装依赖（Node 16，跳过 Cypress 二进制）
task fe:dev          # 本地开发服务器 :8888，/api 代理到 localhost:8080
```

> 管理端锁定 Node 16：Vue CLI 3 + webpack 4 在 Node 17+ 的 OpenSSL 3 下会报 `ERR_OSSL_EVP_UNSUPPORTED`。

## 数据库变更

- 表结构与种子数据只认 `backend/sky-server/src/main/resources/db/migration/` 下的脚本。
- **已执行过的迁移不要修改**，变更请新增 `V3__xxx.sql`。
- 配置关闭了 `baseline-on-migrate`：如果沿用旧数据卷（没有 `flyway_schema_history`），需要先 `task db:reset`，避免 Flyway 在已有表的库上误判。

## 构建与部署

- `task image`：按当前平台构建 `sky-server` / `sky-admin-web` 镜像，tag 为当前 commit 短 SHA。
- 同一个后端镜像可跑在任意环境：容器内使用 `docker` profile，数据库、Redis、JWT 密钥全部由环境变量注入。
- 管理端镜像内置 Nginx 模板，`SKY_BACKEND_ORIGIN` 决定反向代理目标，换环境无需重新构建。
- 国内网络默认走阿里云 Maven 镜像；海外 CI 可在构建镜像时传 `--build-arg MAVEN_SETTINGS=` 关闭。
- `deploy/k8s/` 目前是占位目录，后续补 K8s 清单与 CI。

## 文档

- [数据库设计](docs/design/database-design.md)
- 接口定义：[管理端](docs/api/admin-api.json) / [用户端](docs/api/user-api.json)（YApi 导出）
- 原型与设计稿：`docs/design/prototype/`

## 已知事项 / 待办

- 员工模块目前只有登录、登出，且密码仍为明文比对（代码中有 MD5 的 TODO）；员工 CRUD 等按课程继续补齐。
- MinIO 已在 Compose 中就绪并初始化业务桶，业务代码仍使用课程自带的阿里云 OSS 工具类，待替换为本地对象存储。
- 管理端基于 [vue-typescript-admin-template](https://github.com/Armour/vue-typescript-admin-template)（MIT），业务页面待随课程补充。
