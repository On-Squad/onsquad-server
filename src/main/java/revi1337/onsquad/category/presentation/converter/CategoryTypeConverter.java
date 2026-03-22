package revi1337.onsquad.category.presentation.converter;

import org.springframework.core.convert.converter.Converter;
import revi1337.onsquad.category.domain.vo.CategoryType;

public class CategoryTypeConverter implements Converter<String, CategoryType> {

    @Override
    public CategoryType convert(String categoryText) {
        try {
            return CategoryType.fromText(categoryText);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
