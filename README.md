# DMS – Sprint 4

Docker Desktop mit Linux-Containern starten und im Projektverzeichnis ausführen.

```powershell
docker compose build
docker compose up -d --wait
docker compose ps
```

Die Oberfläche ist unter `http://localhost` erreichbar. Im Formular **New Document** kann eine PDF mit bis zu 20 MiB ausgewählt werden. Der Upload lässt sich auch direkt testen. Dazu eine PDF als `beispiel.pdf` im Arbeitsverzeichnis verwenden.

```powershell
curl.exe -i -F "title=OCR-Test" -F "file=@beispiel.pdf;type=application/pdf" http://localhost/api/documents
docker compose logs --tail 30 ocr-worker
```

Die API antwortet mit `201 Created` und einem `Location`-Header. Die Texterkennung läuft im Hintergrund. Nach der Verarbeitung protokolliert der Worker `Completed OCR for document <id>` mit Seitenzahl und Pfad zum Ergebnis. In der MinIO-Konsole unter `http://localhost:9090` liegt die PDF im Bucket `documents`. Der erkannte UTF-8-Text liegt im gleichen Bucket unter `ocr/<storage-key>.txt`.
