package at.technikum.ocrworker.ocr;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class OcrService {
    private static final Logger log = LoggerFactory.getLogger(OcrService.class);
    private static final String BUCKET = "documents";
    private static final Pattern PAGE_COUNT = Pattern.compile("^Pages:[ \\t]+([0-9]+)$", Pattern.MULTILINE);
    private static final int MAX_PAGES = 100;
    private static final Duration JOB_TIMEOUT = Duration.ofMinutes(8);
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(45);

    private final MinioClient storage;
    private final String languages;

    public OcrService(@Value("${dms.storage.url}") String url,
                      @Value("${dms.storage.access-key}") String accessKey,
                      @Value("${dms.storage.secret-key}") String secretKey,
                      @Value("${dms.ocr.languages}") String languages) {
        storage = MinioClient.builder().endpoint(url).credentials(accessKey, secretKey).build();
        storage.setTimeout(5000, 30000, 30000);
        this.languages = languages;
    }

    public void process(Long documentId, String storageKey) {
        Path workspace = null;
        try {
            workspace = Files.createTempDirectory("dms-ocr-");
            Path pdf = workspace.resolve("document.pdf");
            if (!downloadPdf(storageKey, pdf)) {
                log.info("Skipping OCR for document {} because its PDF is missing", documentId);
                return;
            }

            long deadline = System.nanoTime() + JOB_TIMEOUT.toNanos();
            int pages = countPages(pdf, deadline);
            Path text = workspace.resolve("document.txt");
            recognizePages(pdf, text, pages, deadline);
            String resultKey = "ocr/" + storageKey + ".txt";
            try (var input = Files.newInputStream(text)) {
                storage.putObject(PutObjectArgs.builder().bucket(BUCKET).object(resultKey)
                        .contentType("text/plain; charset=utf-8").stream(input, Files.size(text), -1).build());
            }

            // Reconcile a document deletion that overlapped OCR.
            try {
                storage.statObject(StatObjectArgs.builder().bucket(BUCKET).object(storageKey).build());
            } catch (ErrorResponseException ex) {
                if (!isMissing(ex)) {
                    throw ex;
                }
                storage.removeObject(RemoveObjectArgs.builder().bucket(BUCKET).object(resultKey).build());
                log.info("Removed OCR result for deleted document {}", documentId);
                return;
            }
            log.info("Completed OCR for document {} ({} pages, result: {})", documentId, pages, resultKey);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OcrProcessingException("OCR interrupted for document " + documentId, ex);
        } catch (Exception ex) {
            throw new OcrProcessingException("OCR failed for document " + documentId, ex);
        } finally {
            cleanWorkspace(workspace);
        }
    }

    private boolean downloadPdf(String storageKey, Path target) throws Exception {
        try (var input = storage.getObject(GetObjectArgs.builder().bucket(BUCKET).object(storageKey).build())) {
            Files.copy(input, target);
            return true;
        } catch (ErrorResponseException ex) {
            if (!isMissing(ex)) {
                throw ex;
            }
            return false;
        }
    }

    private static boolean isMissing(ErrorResponseException ex) {
        return "NoSuchKey".equals(ex.errorResponse().code()) || "NoSuchBucket".equals(ex.errorResponse().code());
    }

    private int countPages(Path pdf, long deadline) throws IOException, InterruptedException {
        Path info = pdf.resolveSibling("pdfinfo.txt");
        run(pdf.getParent(), info, deadline, "pdfinfo", pdf.toString());
        var matcher = PAGE_COUNT.matcher(Files.readString(info, StandardCharsets.UTF_8));
        if (!matcher.find()) {
            throw new IOException("PDF page count is missing");
        }
        int pages = Integer.parseInt(matcher.group(1));
        if (pages < 1 || pages > MAX_PAGES) {
            throw new IOException("PDF must have between 1 and " + MAX_PAGES + " pages");
        }
        return pages;
    }

    private void recognizePages(Path pdf, Path text, int pages, long deadline)
            throws IOException, InterruptedException {
        Path imagePrefix = pdf.resolveSibling("page");
        Path image = pdf.resolveSibling("page.png");
        Path pageText = pdf.resolveSibling("page.txt");
        try (var output = Files.newOutputStream(text)) {
            for (int page = 1; page <= pages; page++) {
                String number = Integer.toString(page);
                run(pdf.getParent(), null, deadline, "pdftoppm", "-f", number, "-l", number, "-r", "300", "-scale-to", "3500",
                        "-singlefile", "-gray", "-png", pdf.toString(), imagePrefix.toString());
                run(pdf.getParent(), pageText, deadline, "tesseract", image.toString(), "stdout", "-l", languages,
                        "--dpi", "300");
                appendPage(output, pageText, page);
                Files.delete(image);
            }
        }
    }

    private static void appendPage(OutputStream output, Path pageText, int page) throws IOException {
        if (page > 1) {
            output.write('\f');
            output.write('\n');
        }
        Files.copy(pageText, output);
    }

    private static void run(Path workspace, Path stdout, long deadline, String... command)
            throws IOException, InterruptedException {
        long timeout = Math.min(COMMAND_TIMEOUT.toNanos(), deadline - System.nanoTime());
        if (timeout <= 0) {
            throw new IOException("OCR job exceeded its time limit");
        }
        Path stderr = workspace.resolve("command-error.txt");
        var builder = new ProcessBuilder(command)
                .redirectOutput(stdout == null ? ProcessBuilder.Redirect.DISCARD : ProcessBuilder.Redirect.to(stdout.toFile()))
                .redirectError(stderr.toFile());
        builder.environment().put("LC_ALL", "C");
        builder.environment().put("OMP_THREAD_LIMIT", "2");
        Process process = builder.start();
        try {
            if (!process.waitFor(timeout, TimeUnit.NANOSECONDS)) {
                throw new IOException(command[0] + " timed out");
            }
            if (process.exitValue() != 0) {
                try (var errors = Files.newInputStream(stderr)) {
                    String detail = new String(errors.readNBytes(2048), StandardCharsets.UTF_8).strip();
                    throw new IOException(command[0] + " exited with " + process.exitValue() + ": " + detail);
                }
            }
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static void cleanWorkspace(Path workspace) {
        if (workspace == null) {
            return;
        }
        try (var files = Files.walk(workspace)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ex) {
            log.warn("Could not clean OCR temporary files in {}", workspace, ex);
        }
    }
}
