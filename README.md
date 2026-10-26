# 苍穹外卖 · 云原生改造

黑马「苍穹外卖」项目 → 云原生实践版。目标不是一上来就拆微服务，而是先把单体做成
**可容器化、配置外置、可观测、可声明式部署**的形态，再按业务域逐步拆分。

## 当前进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| 0 | 仓库结构、Docker Compose 一键起环境、镜像化 | ✅ 已完成 |
| 1 | 配置外置、Actuator 探针、Flyway、优雅停机 | 🚧 进行中 |
| 2 | K8s 清单（Kustomize）、Ingress、HPA | ⏸ 目录已留空 |
| 3 | 结构化日志、Prometheus/Grafana、CI 流水线 | ⏸ 未开始 |
| 4 | 按业务域拆服务 | ⏸ 未开始 |

业务代码进度与课程同步：目前完成 **第一课**（员工登录、JWT 拦截、knife4j）。

## 快速开始

前置：Docker Desktop（或 colima）、Git。**不需要本机安装 Maven / MySQL / Redis**。

```bash
cp .env.example .env      # 按需修改端口和密码
brew install go-task      # 或 winget install Task.Task
task up                   # 构建并启动 mysql + redis + minio + 后端 + 管理端
task ps                   # 看健康状态
task smoke                # 冒烟：登录接口应返回 code=1
```

访问：

| 服务 | 地址 |
|---|---|
| 管理端 | http://localhost:8888 |
| 后端 API | http://localhost:8080 |
| 健康检查 | http://localhost:8080/actuator/health |
| 接口文档 | http://localhost:8080/doc.html |
| MinIO 控制台 | http://localhost:9001 |

默认账号：`admin` / `123456`（来自 `db/init/02-seed.sql`）。

## 目录结构

```
backend/                   Maven 三模块单体（Spring Boot 2.7.3 + JDK 17）
  sky-common/              工具、常量、异常、统一返回
  sky-pojo/                entity / dto / vo
  sky-server/              应用入口
  Dockerfile               多阶段构建 + 分层 jar + 非 root
frontend/
  admin-web/               管理端（Vue2 + Vue CLI3 + TS）
    nginx/                 反代模板（envsubst，镜像与环境解耦）
  miniapp/                 小程序（待补）
deploy/
  compose/                 本地一键编排
  k8s/                     base + overlays/{dev,prod}（阶段 2 填充）
db/
  init/                    首次启动自动执行的 schema + seed
  legacy/sky.sql           原始导库脚本（归档，勿改）
docs/
  api/                     管理端/用户端 OpenAPI 契约
  design/                  数据库设计、Axure 原型
  adr/                     架构决策记录
```

## 常用命令

```bash
task --list               # 全部任务（有 desc 的任务会自动列出）
task up / down            # 起 / 停
task logs                 # 跟后端日志（task logs -- admin-web 换服务）
task db:reset             # 删卷重建库（会丢数据）
task build                # 容器内编译（无需本机 JDK）
task image                # 构建镜像
```

`make up` 也能用：根目录的 `Makefile` 只是转发到 task。

## 技术决策

详见 `docs/adr/`。当前生效的三条：

1. **保持 Spring Boot 2.7.3**，不升 3.x —— 与课程/资料一致，避免自造障碍。
2. **用 JDK 17**，不用本机的 JDK 21 —— 2.7.3 官方支持到 Java 19；
   `lombok 1.18.20` 在 17 上会 `IllegalAccessError`，已升到 `1.18.30`。
3. **只做 Docker Compose，K8s 留空目录** —— 先把「环境能一键起来」这件事做扎实。

## 环境说明（重要）

- **Docker Hub 在本机网络下不可达**，拉镜像需要走加速源，见 `docs/adr/0002-docker-mirror.md`。
- 管理端构建锁 **Node 16**：Vue CLI 3 + webpack 4 在 Node 17+ 会因 OpenSSL 3 报 `ERR_OSSL_EVP_UNSUPPORTED`。
- 数据库初始化只在**数据卷为空**时执行；改了 `db/init/*.sql` 需要 `task db:reset`。
