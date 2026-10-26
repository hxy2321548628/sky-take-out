# syntax=docker/dockerfile:1.7
# ============================================================
# sky-server 镜像（多阶段 + 分层 jar，arm64/amd64 均可构建）
# 构建上下文：backend/
# ============================================================

# ---------- 1. 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 国内网络下 repo.maven.apache.org 很慢，默认走阿里云镜像；
# 海外 CI 可以传 --build-arg MAVEN_SETTINGS= 关掉（直接连 Maven Central）。
ARG MAVEN_SETTINGS=.mvn/settings.xml

# 注意：缓存要挂在 repository 子目录，不能挂在 /root/.m2，
# 否则 cache mount 会覆盖掉这里的 settings.xml，等于没用镜像源。
COPY . .
RUN --mount=type=cache,target=/root/.m2/repository \
    if [ -n "$MAVEN_SETTINGS" ]; then \
        mvn -B -s "$MAVEN_SETTINGS" -DskipTests package; \
    else \
        mvn -B -DskipTests package; \
    fi

# ---------- 2. 分层提取 ----------
# 把 fat jar 拆成 dependencies / spring-boot-loader / snapshot-dependencies / application
# 代码改动只会让 application 层失效，镜像推送和拉取都更快
FROM eclipse-temurin:17-jre-jammy AS layers
WORKDIR /app
COPY --from=build /build/sky-server/target/sky-server-1.0-SNAPSHOT.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ---------- 3. 运行阶段 ----------
FROM eclipse-temurin:17-jre-jammy

ENV TZ=Asia/Shanghai \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"

RUN groupadd --gid 10001 sky \
 && useradd --uid 10001 --gid sky --create-home --shell /usr/sbin/nologin sky \
 && apt-get update \
 && apt-get install -y --no-install-recommends curl tzdata \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=layers --chown=sky:sky /app/dependencies/          ./
COPY --from=layers --chown=sky:sky /app/spring-boot-loader/    ./
COPY --from=layers --chown=sky:sky /app/snapshot-dependencies/ ./
COPY --from=layers --chown=sky:sky /app/application/           ./

# 非 root 运行
USER 10001

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
