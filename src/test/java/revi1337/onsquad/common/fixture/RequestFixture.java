package revi1337.onsquad.common.fixture;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import org.springframework.mock.web.MockMultipartFile;

public class RequestFixture {

    public static final String DEFAULT_MULTIPART_NAME = "file";
    public static final String DEFAULT_PNG_FILE_NAME = "test.png";
    public static final String DEFAULT_JPG_FILE_NAME = "test.jpg";
    public static final byte[] PNG_BYTES = new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D,
            (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};
    public static final byte[] JPG_BYTES = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    public static MockMultipartFile JSON_MULTIPART(String name, String originalFileName, String content) {
        return new MockMultipartFile(name, originalFileName, APPLICATION_JSON_VALUE, content.getBytes(UTF_8));
    }

    public static MockMultipartFile JSON_MULTIPART(String name, String content) {
        return new MockMultipartFile(name, name, APPLICATION_JSON_VALUE, content.getBytes(UTF_8));
    }

    public static MockMultipartFile PNG_MULTIPART(String name, String originalFileName) {
        return withMultipart(name, originalFileName, PNG_BYTES);
    }

    public static MockMultipartFile JPG_MULTIPART(String name, String originalFileName) {
        return withMultipart(name, originalFileName, JPG_BYTES);
    }

    public static MockMultipartFile PNG_MULTIPART(String originalFileName) {
        return withMultipart(DEFAULT_MULTIPART_NAME, originalFileName, PNG_BYTES);
    }

    public static MockMultipartFile JPG_MULTIPART(String originalFileName) {
        return withMultipart(DEFAULT_MULTIPART_NAME, originalFileName, JPG_BYTES);
    }

    private static MockMultipartFile withMultipart(String name, String originalFileName, byte[] bytes) {
        return new MockMultipartFile(name, originalFileName, MULTIPART_FORM_DATA_VALUE, bytes);
    }
}
