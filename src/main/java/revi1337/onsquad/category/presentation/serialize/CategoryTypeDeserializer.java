package revi1337.onsquad.category.presentation.serialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import revi1337.onsquad.category.domain.vo.CategoryType;

public class CategoryTypeDeserializer extends StdDeserializer<CategoryType> {

    public CategoryTypeDeserializer() {
        super(CategoryType.class);
    }

    @Override
    public CategoryType deserialize(JsonParser p, DeserializationContext ctxt) {
        try {
            TextNode textNode = p.getCodec().readTree(p);
            String hashtag = textNode.asText();
            return CategoryType.fromText(hashtag);
        } catch (IOException e) {
            throw new IllegalArgumentException("카테고리 JSON 파싱 중 에러 발생", e);
        }
    }
}
