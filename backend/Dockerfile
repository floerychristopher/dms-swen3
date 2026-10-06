# ---------- Build-Stage: Maven kompiliert, testet und paketiert das Projekt ----------
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Maven Wrapper und Config Dateien kopieren
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Abhängigkeiten in eigenem Layer laden -> werden bei reinen Code-Änderungen aus dem Cache genommen
RUN ./mvnw -B -q dependency:go-offline

# Source-Code kopieren
COPY src ./src

# App bauen inkl. Tests (laufen gegen H2, brauchen also keine Datenbank).
# Für schnelle lokale Builds: docker compose build --build-arg SKIP_TESTS=true
ARG SKIP_TESTS=false
RUN ./mvnw -B package -DskipTests=${SKIP_TESTS}

# ---------- Run-Stage: Nur fertiges JAR mit schlanker JRE ausführen ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Nicht als root laufen
RUN useradd --system --uid 1001 spring
USER spring

# Fertiges JAR aus Build Phase kopieren
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]