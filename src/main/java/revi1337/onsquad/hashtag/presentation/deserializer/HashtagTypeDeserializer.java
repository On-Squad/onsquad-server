package revi1337.onsquad.hashtag.presentation.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import revi1337.onsquad.hashtag.domain.vo.HashtagType;
import revi1337.onsquad.hashtag.util.HashtagTypeUtil;

public class HashtagTypeDeserializer extends StdDeserializer<HashtagType> {

    public HashtagTypeDeserializer() {
        super(HashtagType.class);
    }

    @Override
    public HashtagType deserialize(JsonParser p, DeserializationContext ctxt) {
        try {
            TextNode textNode = p.getCodec().readTree(p);
            String hashtag = textNode.asText();
            HashtagType hashtagType = HashtagType.fromText(hashtag);
            HashtagTypeUtil.validateHashtag(hashtagType);
            return hashtagType;
        } catch (IOException e) {
            throw new IllegalArgumentException("해시태그 JSON 파싱 중 에러 발생", e);
        }
    }
}
