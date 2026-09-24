# Build (Maven kompiliert das Projekt)
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Maven Wrapper und Config Datein kopieren
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Source-Code kopieren
COPY src ./src

# App bauen (Tests überspringen)
RUN ./mvnw clean package -DskipTests

# Run: Nur fertiges JAR ausführen
FROM eclipse-temurin:25-jre
WORKDIR /app

# Fertiges JAR aus Build Phase kopieren
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]