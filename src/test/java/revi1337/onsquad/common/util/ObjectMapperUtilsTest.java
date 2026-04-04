package revi1337.onsquad.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;
import revi1337.onsquad.common.config.web.ObjectMapperConfig;

@JsonTest
@Import(ObjectMapperConfig.class)
class ObjectMapperUtilsTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Serialize 테스트")
    class SerializeTest {

        @Test
        @DisplayName("객체를 JSON 문자열로 직렬화한다.")
        void serializeToString() {
            TestDto dto = new TestDto("name", 20);

            String json = ObjectMapperUtils.serializeToString(objectMapper, dto);

            assertThat(json).contains("\"name\":\"name\"", "\"age\":20");
        }

        @Test
        @DisplayName("객체를 byte 배열로 직렬화한다.")
        void serializeToBytes() {
            TestDto dto = new TestDto("name", 20);

            byte[] bytes = ObjectMapperUtils.serializeToBytes(objectMapper, dto);

            assertThat(bytes).isNotEmpty();
            assertThat(new String(bytes)).contains("name");
        }

        @Test
        @DisplayName("객체를 파일에 Pretty 포맷으로 저장한다.")
        void serializeToFileAsPretty(@TempDir Path tempDir) throws IOException {
            File file = tempDir.resolve("test.json").toFile();
            TestDto dto = new TestDto("name", 20);

            ObjectMapperUtils.serializeToFileAsPretty(objectMapper, file, dto);

            String content = Files.readString(file.toPath());
            assertThat(content).contains("\n  \"name\"");
        }
    }

    @Nested
    @DisplayName("Deserialize 테스트")
    class DeserializeTest {

        @Test
        @DisplayName("JSON 문자열을 클래스 타입으로 역직렬화한다.")
        void deserializeToClass() {
            String json = "{\"name\":\"name\", \"age\":20}";
            TestDto result = ObjectMapperUtils.deserialize(objectMapper, json, TestDto.class);

            assertSoftly(softly -> {
                softly.assertThat(result.getName()).isEqualTo("name");
                softly.assertThat(result.getAge()).isEqualTo(20);
            });
        }

        @Test
        @DisplayName("JSON 배열 문자열을 클래스 배열 타입으로 역직렬화한다.")
        void deserializeToClassArray() {
            String json = "[{\"name\":\"a\", \"age\":10}, {\"name\":\"b\", \"age\":20}]";

            // 배열 타입으로 직접 역직렬화
            TestDto[] result = ObjectMapperUtils.deserialize(objectMapper, json, TestDto[].class);

            assertSoftly(softly -> {
                softly.assertThat(result).hasSize(2);
                softly.assertThat(result[0].getName()).isEqualTo("a");
                softly.assertThat(result[1].getName()).isEqualTo("b");
            });
        }

        @Test
        @DisplayName("JavaType을 이용하여 제네릭 타입을 역직렬화한다.")
        void deserializeToJavaType() {
            String json = "[{\"name\":\"a\"}, {\"name\":\"b\"}]";
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, TestDto.class);

            List<TestDto> result = ObjectMapperUtils.deserialize(objectMapper, json, listType);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("a");
        }

        @Test
        @DisplayName("유효하지 않은 JSON 문자열로 Tree 구조 생성 시 빈 객체를 반환한다.")
        void deserializeAsTreeOrEmpty() {
            String invalidJson = "{invalid-json}";

            JsonNode node = ObjectMapperUtils.deserializeAsTreeOrEmpty(objectMapper, invalidJson);

            assertSoftly(softly -> {
                softly.assertThat(node.isObject()).isTrue();
                softly.assertThat(node).isEmpty();
            });
        }

        @Test
        @DisplayName("JSON 배열 형태의 문자열도 Tree 구조(ArrayNode)로 파싱할 수 있다.")
        void deserializeAsTreeArray() {
            String json = "[\"활발한\",\"트랜디한\"]";

            JsonNode node = ObjectMapperUtils.deserializeAsTreeOrEmpty(objectMapper, json);

            assertSoftly(softly -> {
                softly.assertThat(node.isArray()).isTrue();
                softly.assertThat(node).hasSize(2);
                softly.assertThat(node.get(0).asText()).isEqualTo("활발한");
            });
        }
    }

    @Nested
    @DisplayName("JsonNode 변환 테스트")
    class ToJsonNodeTest {

        @Test
        @DisplayName("다양한 소스(String, Object, null)를 JsonNode로 변환한다.")
        void toJsonNode() {
            String jsonStr = "{\"id\":1}";
            TestDto dto = new TestDto("dto", 10);

            JsonNode fromStr = ObjectMapperUtils.toJsonNode(objectMapper, jsonStr);
            JsonNode fromDto = ObjectMapperUtils.toJsonNode(objectMapper, dto);
            JsonNode fromNull = ObjectMapperUtils.toJsonNode(objectMapper, null);

            assertSoftly(softly -> {
                softly.assertThat(fromStr.get("id").asInt()).isEqualTo(1);
                softly.assertThat(fromDto.get("name").asText()).isEqualTo("dto");
                softly.assertThat(fromNull.isObject()).isTrue();
                softly.assertThat(fromNull).isEmpty();
            });
        }
    }

    @Nested
    @DisplayName("Type Construction 테스트")
    class TypeConstructionTest {

        @Test
        @DisplayName("메서드의 제네릭 반환 타입을 추출한다.")
        void NoSuchMethodException() throws NoSuchMethodException {
            Method method = TestService.class.getMethod("getDtoList");

            JavaType javaType = ObjectMapperUtils.constructGenericType(objectMapper, method);

            assertThat(javaType.isCollectionLikeType()).isTrue();
            assertThat(javaType.getContentType().getRawClass()).isEqualTo(TestDto.class);
        }

        @Test
        @DisplayName("메서드의 Raw 반환 타입을 추출한다.")
        void NoSuchMethodException2() throws NoSuchMethodException {
            Method method = TestService.class.getMethod("getDtoList");

            JavaType javaType = ObjectMapperUtils.constructRawType(objectMapper, method);

            assertThat(javaType.getRawClass()).isEqualTo(List.class);
        }
    }

    static class TestDto {

        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public TestDto() {
        }

        public TestDto(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    static class TestService {

        public List<TestDto> getDtoList() {
            return List.of();
        }
    }
}
