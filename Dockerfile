# ── Stage 1: Build ────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copiar pom primero para aprovechar caché de capas de dependencias
COPY pom.xml ./
RUN mvn dependency:go-offline -q

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ───────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
