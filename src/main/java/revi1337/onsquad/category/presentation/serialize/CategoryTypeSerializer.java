package revi1337.onsquad.category.presentation.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import revi1337.onsquad.category.domain.vo.CategoryType;

public class CategoryTypeSerializer extends StdSerializer<CategoryType> {

    public CategoryTypeSerializer() {
        super(CategoryType.class);
    }

    @Override
    public void serialize(CategoryType categoryType, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) {
        try {
            jsonGenerator.writeString(categoryType.getText());
        } catch (IOException e) {
            throw new IllegalArgumentException("해시태그 직렬화 중 예외 발생", e);
        }
    }
}
