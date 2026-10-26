# ADR 0002 · Docker Hub 不可达与镜像加速

- 状态：已采纳
- 日期：2026-10-26

## 问题

本机网络下 `https://registry-1.docker.io/v2/` 连接超时（15s 无响应），
`docker pull maven:3.9-eclipse-temurin-17` 会一直挂起。
Maven Central、npm registry 均正常，**只有 Docker Hub 受影响**。

## 已验证可用的加速源

| 源 | 探测结果 |
|---|---|
| `docker.m.daocloud.io` | 401（正常，需 token） |
| `docker.1panel.live` | 200 |
| `hub.rat.dev` | 302 |
| `dockerproxy.com` | 超时 |
| `mirror.ccs.tencentyun.com` | 连接失败 |

## 方案 A（推荐）：配置 Docker Desktop 镜像加速

Docker Desktop → Settings → Docker Engine，加入：

```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.1panel.live"
  ]
}
```

Apply & Restart 后，`docker pull mysql:8.0` 会自动走加速源，
`Dockerfile` 里的 `FROM` 也不需要改。

> 注意：`registry-mirrors` 只影响 Docker Hub（`docker.io`）的镜像；
> `minio/minio`、`node:16` 等都在 Docker Hub，可用。

## 方案 B（临时）：手动拉取并重命名

不改 daemon 配置，直接把镜像从加速源拉下来打上标准名字：

```bash
docker pull docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-17
docker tag  docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-17 maven:3.9-eclipse-temurin-17
```

之后 `docker build` 会优先命中本地镜像，不再访问 Hub。

## 影响

- 首次 `task up` 需要拉 `mysql:8.0`、`redis:7-alpine`、`minio/minio`、
  `maven:3.9-eclipse-temurin-17`、`eclipse-temurin:17-jre-jammy`、`node:16-bullseye`，
  体量约 2GB，建议先配置好镜像再执行。
- 换网络环境（如公司 VPN）后若恢复直连，可移除 `registry-mirrors`。
