package revi1337.onsquad.hashtag.presentation.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import revi1337.onsquad.hashtag.domain.HashtagType;

public class HashtagTypeSerializer extends StdSerializer<HashtagType> {

    public HashtagTypeSerializer() {
        super(HashtagType.class);
    }

    @Override
    public void serialize(HashtagType hashtagType, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) {
        try {
            jsonGenerator.writeString(hashtagType.getText());
        } catch (IOException e) {
            throw new IllegalArgumentException("해시태그 직렬화 중 예외 발생", e);
        }
    }
}
