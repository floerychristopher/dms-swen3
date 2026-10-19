# ---------- Build-Stage: Maven kompiliert und paketiert den Worker ----------
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Abhängigkeiten in eigenem Layer laden -> werden bei reinen Code-Änderungen aus dem Cache genommen
RUN ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN ./mvnw -B package

# ---------- Run-Stage: Nur fertiges JAR mit schlanker JRE ausführen ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Nicht als root laufen
RUN useradd --system --uid 1001 spring
USER spring

COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
