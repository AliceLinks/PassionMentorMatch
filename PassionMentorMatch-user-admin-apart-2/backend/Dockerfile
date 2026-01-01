# syntax=docker/dockerfile:1

# ===== Build stage =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 构建上下文是 backend/ 根目录：此处直接复制 pom.xml 和 src
COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

# ===== Runtime stage =====
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
# 云托管会注入 PORT 环境变量
ENV PORT=8080
# 安装系统 CA 证书，确保 HTTPS 信任链完整（避免 PKIX path building failed）
RUN apt-get update \
	&& apt-get install -y --no-install-recommends ca-certificates \
	&& update-ca-certificates \
	&& rm -rf /var/lib/apt/lists/*
# 拷贝构建产物
COPY --from=build /app/target/mentor-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
# 运行参数：server.port 会从 ${PORT} 读取
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
