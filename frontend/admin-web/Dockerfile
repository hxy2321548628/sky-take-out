# syntax=docker/dockerfile:1.7
# ============================================================
# 管理端镜像：Node 构建 → Nginx 托管
# 构建上下文：frontend/admin-web/
#
# 为什么锁 Node 16：项目是 Vue CLI 3 + webpack 4，Node 17+ 的 OpenSSL 3
# 会让 webpack 的 md4 hash 报 ERR_OSSL_EVP_UNSUPPORTED。Node 16 是这套
# 依赖组合的稳定版本（也是原课程推荐版本）。
# ============================================================

# ---------- 1. 构建阶段 ----------
FROM node:16-bullseye AS build
WORKDIR /app

ENV NODE_OPTIONS=--max-old-space-size=2048

COPY package.json yarn.lock package-lock.json ./
RUN yarn install --frozen-lockfile --network-timeout 600000 || npm ci

COPY . .
RUN yarn build

# ---------- 2. 运行阶段 ----------
FROM nginx:1.27-alpine

ENV TZ=Asia/Shanghai \
    SKY_BACKEND_ORIGIN=http://sky-server:8080

COPY --from=build /app/dist /usr/share/nginx/html
# 官方镜像会把 /etc/nginx/templates/*.template 做 envsubst 后输出到 conf.d，
# 于是同一个镜像可以指向不同的后端地址 —— 镜像不随环境重新构建
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
    CMD wget -q --spider http://127.0.0.1/ || exit 1
