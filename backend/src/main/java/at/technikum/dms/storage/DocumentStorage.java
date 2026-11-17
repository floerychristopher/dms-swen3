package at.technikum.dms.storage;

import at.technikum.dms.exception.InvalidPdfException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Component
public class DocumentStorage {
    private static final byte[] PDF_HEADER = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final String BUCKET = "documents";
    private final MinioClient client;

    public DocumentStorage(@Value("${dms.storage.url}") String url,
                           @Value("${dms.storage.access-key}") String accessKey,
                           @Value("${dms.storage.secret-key}") String secretKey) {
        client = MinioClient.builder().endpoint(url).credentials(accessKey, secretKey).build();
        client.setTimeout(5000, 30000, 30000);
    }

    public String store(MultipartFile file) {
        String key = UUID.randomUUID() + ".pdf";
        try (var stream = new BufferedInputStream(file.getInputStream())) {
            stream.mark(PDF_HEADER.length);
            if (!Arrays.equals(stream.readNBytes(PDF_HEADER.length), PDF_HEADER)) {
                throw new InvalidPdfException();
            }
            stream.reset();
            ensureBucket();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        remove(key);
                    }
                }
            });
            client.putObject(PutObjectArgs.builder().bucket(BUCKET).object(key)
                    .contentType("application/pdf").stream(stream, file.getSize(), -1).build());
            log.info("Stored PDF {}", key);
            return key;
        } catch (InvalidPdfException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DocumentStorageException("Could not store PDF " + key, ex);
        }
    }

    public void removeAfterCommit(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                remove(key);
                remove("ocr/" + key + ".txt");
            }
        });
    }

    private void ensureBucket() throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
            try {
                client.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
            } catch (ErrorResponseException ex) {
                if (!"BucketAlreadyOwnedByYou".equals(ex.errorResponse().code())) {
                    throw ex;
                }
            }
        }
    }

    private void remove(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(BUCKET).object(key).build());
        } catch (Exception ex) {
            log.error("Could not remove PDF {} from MinIO", key, ex);
        }
    }
}
