# ============ 阶段一：Maven 构建 ============
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 先复制 pom.xml，利用 Docker 缓存层
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码进行编译
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============ 阶段二：运行时镜像 ============
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 创建非 root 用户运行（安全最佳实践）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 复制 jar 包
COPY --from=builder /app/target/moforum-0.0.1-SNAPSHOT.jar app.jar

# 创建配置目录
RUN mkdir -p /app/config && chown -R appuser:appgroup /app

USER appuser
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "/app/app.jar", \
  "--spring.config.import=optional:file:/app/config/.env[.properties]"]