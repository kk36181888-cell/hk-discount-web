# 使用 Java 21 環境
FROM eclipse-temurin:21-jdk
WORKDIR /app

# 將你嘅 Code 複製入去
COPY . .

# 解鎖並打包 Spring Boot 程式
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# 暴露 8080 端口
EXPOSE 8080

# 啟動伺服器
CMD ["java", "-jar", "target/discount-web-0.0.1-SNAPSHOT.jar"]