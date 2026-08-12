# =========================================================
# STAGE 1: BUILD
# =========================================================

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Maven Wrapper
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Maven Wrapper cần quyền thực thi trên Linux
RUN chmod +x mvnw

# Download dependencies trước
# Giúp tận dụng Docker layer cache
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build Spring Boot application
# Test đã được chạy riêng trước đó
RUN ./mvnw clean package -DskipTests


# =========================================================
# STAGE 2: RUNTIME
# =========================================================

FROM eclipse-temurin:21-jre

WORKDIR /app

# Tạo các thư mục runtime
RUN mkdir -p /app/uploads \
    /app/files/qr

# Chỉ copy JAR từ build stage
COPY --from=builder /app/target/*.jar app.jar

# Spring Boot
EXPOSE 8080

# Cho phép truyền JVM options từ bên ngoài
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
