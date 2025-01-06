package kr.mafoo.recap.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ObjectStorageService {

    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.endpoint}")
    private String endpoint;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public Mono<String> uploadFile(String filePath) {
        return Mono.fromCallable(() -> {
            File file = new File(filePath);
            String keyName = "recap/" + file.getName();

            if (!file.exists() || !file.isFile()) {
                throw new IllegalArgumentException("Invalid file path: " + filePath);
            }

            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.length());
            objectMetadata.setContentType("application/octet-stream");

            try (InputStream inputStream = FileUtils.openInputStream(file)) {
                amazonS3Client.putObject(
                    new PutObjectRequest(bucketName, keyName, inputStream, objectMetadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead));

                return generateFileLink(keyName);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload file to object storage: " + filePath, e);
            }
        });
    }

    public Mono<Void> removeFile(String fileUrl) {
        return Mono.fromRunnable(() -> {
            try {
                amazonS3Client.deleteObject(bucketName, extractKeyName(fileUrl));
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete object from S3: " + fileUrl, e);
            }
        });
    }

    public Mono<String> setObjectPublicRead(String filePath) {
        return Mono.fromCallable(() -> {
            try {
                String keyName = extractKeyName(filePath);
                amazonS3Client.setObjectAcl(bucketName, keyName, CannedAccessControlList.PublicRead);
                return generateFileLink(keyName);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set ACL to PublicRead for the file: " + filePath, e);
            }
        });
    }

    private String extractKeyName(String filePath) {
        return filePath.split("object.ncloudstorage.com/")[1];
    }

    private String generateFileLink(String keyName) {
        return endpoint + "/" + bucketName + "/" + keyName;
    }
}
