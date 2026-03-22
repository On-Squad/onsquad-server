package revi1337.onsquad.infrastructure.aws.s3.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import revi1337.onsquad.common.application.file.FileStorageManager;
import revi1337.onsquad.common.error.FileActionException;
import revi1337.onsquad.common.error.FileErrorCode;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@RequiredArgsConstructor
public class S3StorageManager implements FileStorageManager {

    private final S3Client s3Client;
    private final String bucket;

    @Override
    public String upload(byte[] fileBytes, String filePathAndName) {
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            RequestBody requestBody = RequestBody.fromInputStream(inputStream, fileBytes.length);
            MediaType type = MediaType.parseMediaType(Files.probeContentType(Paths.get(filePathAndName)));

            return uploadFile(filePathAndName, type, requestBody);
        } catch (IOException exception) {
            log.error("byte array 처리 중 예외 발생", exception);
            throw new FileActionException.ProcessFail(FileErrorCode.FAIL_PROCESS, exception);
        }
    }

    @Override
    public String upload(MultipartFile multipart) {
        try {
            RequestBody requestBody = RequestBody.fromInputStream(multipart.getInputStream(), multipart.getSize());
            MediaType type = MediaType.parseMediaType(Files.probeContentType(Paths.get(multipart.getOriginalFilename())));

            return uploadFile(multipart.getOriginalFilename(), type, requestBody);
        } catch (IOException exception) {
            log.error("multipart 처리 중 예외 발생", exception);
            throw new FileActionException.ProcessFail(FileErrorCode.FAIL_PROCESS, exception);
        }
    }

    @Override
    public String upload(MultipartFile multipart, String filePathAndName) {
        try {
            RequestBody requestBody = RequestBody.fromInputStream(multipart.getInputStream(), multipart.getSize());
            MediaType type = MediaType.parseMediaType(Files.probeContentType(Paths.get(multipart.getOriginalFilename())));

            return uploadFile(filePathAndName, type, requestBody);
        } catch (IOException exception) {
            log.error("multipart 처리 중 예외 발생", exception);
            throw new FileActionException.ProcessFail(FileErrorCode.FAIL_PROCESS, exception);
        }
    }

    @Override
    public void delete(String filePathAndName) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(filePathAndName)
                    .build();

            s3Client.deleteObject(deleteRequest);
        } catch (SdkClientException | AwsServiceException exception) {
            log.error("s3 삭제 중 예외 발생", exception);
            throw new FileActionException.DeleteFail(FileErrorCode.FAIL_DELETE, exception);
        }
    }

    private String uploadFile(String filePathAndName, MediaType mediaType, RequestBody requestBody) {
        try {
            PutObjectRequest uploadRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(filePathAndName)
                    .contentType(mediaType.toString())
                    .build();

            s3Client.putObject(uploadRequest, requestBody);

            return s3Client.utilities()
                    .getUrl(builder -> builder.bucket(bucket).key(filePathAndName))
                    .toExternalForm();
        } catch (SdkClientException | AwsServiceException exception) {
            log.error("s3 업로드 중 예외 발생", exception);
            throw new FileActionException.UploadFail(FileErrorCode.FAIL_UPLOAD, exception);
        }
    }
}
