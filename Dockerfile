# 使用帶有 Maven 同 Java 21 嘅官方標準映像檔
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 複製所有檔案
COPY . .

# 給予執行權限並打包
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# 執行階段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/discount-web-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]