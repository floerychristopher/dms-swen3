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

## Ablauf für das Code-Review

- Nginx leitet `/api/` an den REST-Server auf Container-Port 8081 weiter. Direkt ist die API am Host über Port 8080 erreichbar.
- Der REST-Server prüft die PDF-Signatur, speichert die Datei in MinIO und schreibt Dokument und OCR-Auftrag gemeinsam in PostgreSQL.
- Ein Hintergrundprozess sendet ausstehende Aufträge an die persistente Queue `document.created`. Er entfernt sie aus PostgreSQL erst nach Bestätigung durch RabbitMQ. Bei kurzzeitigen Ausfällen wiederholt er den Versand. Wenn RabbitMQ bestätigt und der Prozess vor dem Datenbank-Commit abbricht, kann derselbe Auftrag erneut zugestellt werden.
- Der eigenständige OCR-Worker lädt PDFs aus MinIO, rendert die Seiten mit Poppler und erkennt den Text mit Tesseract. Standardmäßig erkennt er Deutsch und Englisch. Für die OCR verarbeitet er PDFs mit bis zu 100 Seiten.
- RabbitMQ bestätigt die Verarbeitung nach dem Speichern des Ergebnisses. Wiederholte Zustellungen schreiben unter denselben Ergebnispfad. Nach ausgeschöpften Wiederholungsversuchen bleiben fehlgeschlagene Aufträge in `document.ocr.failed` erhalten, etwa bei verschlüsselten oder defekten PDFs.
- Der JSON-Endpunkt für bestehende Clients bleibt verfügbar. Der Worker überspringt Aufträge zu Dokumenten ohne Datei.
