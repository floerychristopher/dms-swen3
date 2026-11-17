# ---------- Build-Stage: Maven kompiliert und paketiert den Worker ----------
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

COPY --chmod=755 mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Abhängigkeiten in eigenem Layer laden -> werden bei reinen Code-Änderungen aus dem Cache genommen
RUN ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN ./mvnw -B package

# ---------- Run-Stage: Worker mit PDF-Renderer und OCR ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends poppler-utils tesseract-ocr tesseract-ocr-deu tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

# Nicht als root laufen
RUN useradd --system --uid 1001 spring
USER spring

COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
