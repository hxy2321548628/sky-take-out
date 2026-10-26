# ADR 0001 · 技术栈与改造边界

- 状态：已采纳
- 日期：2026-10-26

## 背景

把黑马「苍穹外卖」单体项目改造成云原生形态。当前处于课程第一课结束的位置，
后续还要继续跟着视频写业务代码。

## 决策

### 1. 保持 Spring Boot 2.7.3，不升级 3.x

升级到 3.x 需要 `javax.*` → `jakarta.*`、springfox → springdoc、jjwt 0.9 → 0.12
等一串连锁改动。在"还要跟着课程写代码"的阶段做这些，会让视频里的代码和本地
仓库对不上，学习成本反而上升。

**代价**：`fastjson 1.2.76`（CVE-2022-25845）、`jjwt 0.9.1` 等老依赖暂时保留。
**缓解**：阶段 3 做安全加固时统一替换为 Jackson + jjwt 0.12，届时业务代码已成型。

### 2. 使用 JDK 17，不使用本机已有的 JDK 21

- Spring Boot 2.7.x 官方支持区间是 Java 8–19，21 不在支持范围。
- `lombok 1.18.20` 在 JDK 16+ 直接编译失败（`IllegalAccessError`），
  已在 `backend/pom.xml` 升到 `1.18.30`（兼容 8~21）。
- 结论：**JDK 17 是这套依赖组合的最省心选择**，也是主流 LTS。

### 3. 构建不依赖本机 JDK/Maven

- `task build` 在 `maven:3.9-eclipse-temurin-17` 容器里编译。
- 本地只有 IntelliJ 需要 SDK，用 IDE 自带下载即可，不污染 shell 环境。
- 目的："不想因为环境给学习造成阻碍" —— 环境问题收敛到 Docker 一处。

### 4. 只做 Docker Compose，K8s 先留空

`deploy/k8s/{base,overlays}` 只放 `.gitkeep`。理由：K8s 的价值在
"多副本 + 声明式发布 + 自愈"，在只有一个服务、还没有业务功能的阶段引入，
只会让每次改动多一层调试成本。等阶段 2 功能铺开后再填清单。

## 影响

- `Dockerfile` 固定 `eclipse-temurin:17-jre`。
- 新增 `application-docker.yml`，与 `application-dev.yml` 并存：
  前者读环境变量（容器），后者读 localhost（IDE 直连本机/容器映射端口）。
