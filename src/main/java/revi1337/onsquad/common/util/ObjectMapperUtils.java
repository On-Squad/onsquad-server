package revi1337.onsquad.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ObjectMapperUtils {

    public static String serializeToString(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            String className = value.getClass().getName();
            log.error("JSON Serialization failed for type: [{}]", className, e);
            throw new RuntimeException("JSON Serialization Error: " + className, e);
        }
    }

    public static byte[] serializeToBytes(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            String className = value.getClass().getName();
            log.error("JSON Serialization to bytes failed for type: [{}]", className, e);
            throw new RuntimeException("JSON Serialization Error (bytes): " + className, e);
        }
    }

    public static void serializeToFileAsPretty(ObjectMapper objectMapper, File file, Object value) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, value);
        } catch (IOException e) {
            log.error("Failed to serialize JSON to file: [{}]", file.getName(), e);
            throw new RuntimeException("File serialization error: " + file.getName(), e);
        }
    }

    public static <T> T deserialize(ObjectMapper objectMapper, String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON Deserialization failed for class: [{}]", clazz.getName(), e);
            throw new RuntimeException("JSON Deserialization error for type: " + clazz.getName(), e);
        }
    }

    public static <T> T deserialize(ObjectMapper objectMapper, String json, JavaType javaType) {
        try {
            return objectMapper.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.error("JSON Deserialization failed for JavaType: [{}]", javaType.getTypeName(), e);
            throw new RuntimeException("JSON Deserialization error for JavaType: " + javaType.getTypeName(), e);
        }
    }

    public static <T> T deserializeFromFile(ObjectMapper objectMapper, File file, Class<T> clazz) {
        try {
            return objectMapper.readValue(file, clazz);
        } catch (IOException e) {
            log.error("Failed to read JSON from file: [{}]", file.getName(), e);
            throw new RuntimeException("File read error: " + file.getName(), e);
        }
    }

    public static JsonNode deserializeAsTreeOrEmpty(ObjectMapper objectMapper, String json) {
        try {
            if (json == null || json.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("JSON Deserialization as tree failed. Returning empty ObjectNode.", e);
            return objectMapper.createObjectNode();
        }
    }

    public static JsonNode toJsonNode(ObjectMapper objectMapper, Object source) {
        if (source == null) {
            return objectMapper.createObjectNode();
        }
        if (source instanceof JsonNode jsonNode) {
            return jsonNode;
        }
        if (source instanceof String json) {
            try {
                return objectMapper.readTree(json);
            } catch (JsonProcessingException e) {
                log.error("Invalid JSON string payload: [{}]", json, e);
                throw new IllegalArgumentException("Invalid JSON payload string", e);
            }
        }
        return objectMapper.valueToTree(source);
    }

    public static JavaType constructGenericType(ObjectMapper objectMapper, Method method) {
        Objects.requireNonNull(method, "Method must not be null");
        return objectMapper.getTypeFactory().constructType(method.getGenericReturnType());
    }

    public static JavaType constructRawType(ObjectMapper objectMapper, Method method) {
        Objects.requireNonNull(method, "Method must not be null");
        return objectMapper.getTypeFactory().constructType(method.getReturnType());
    }
}
