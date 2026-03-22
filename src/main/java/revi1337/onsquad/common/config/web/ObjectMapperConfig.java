package revi1337.onsquad.common.config.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.category.presentation.serialize.CategoryTypeDeserializer;
import revi1337.onsquad.category.presentation.serialize.CategoryTypeSerializer;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.hashtag.presentation.serialize.HashtagTypeDeserializer;
import revi1337.onsquad.hashtag.presentation.serialize.HashtagTypeSerializer;

@Configuration
public class ObjectMapperConfig {

    /**
     * Provides the primary {@link ObjectMapper} bean for the application.
     * <p>
     * This {@link ObjectMapper} is configured for the application's domain-specific needs:
     * <ul>
     *     <li>Registers {@link JavaTimeModule} to handle Java 8 date and time types.</li>
     *     <li>Adds custom serializers and deserializers for {@link HashtagType} and {@link CategoryType}.</li>
     *     <li>Disables writing dates as timestamps to produce human-readable ISO-8601 format.</li>
     * </ul>
     * <p>
     * Being marked as {@link Primary}, this mapper will be injected by default wherever an {@link ObjectMapper} is required.
     * Use this bean for general application serialization and deserialization involving domain-specific types.
     *
     * @param builder the {@link Jackson2ObjectMapperBuilder} used to create the mapper
     * @return a configured {@link ObjectMapper} instance ready for domain-specific serialization
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.createXmlMapper(false).build()
                .registerModule(new JavaTimeModule())
                .registerModule(new SimpleModule().addDeserializer(HashtagType.class, new HashtagTypeDeserializer()))
                .registerModule(new SimpleModule().addSerializer(HashtagType.class, new HashtagTypeSerializer()))
                .registerModule(new SimpleModule().addDeserializer(CategoryType.class, new CategoryTypeDeserializer()))
                .registerModule(new SimpleModule().addSerializer(CategoryType.class, new CategoryTypeSerializer()))
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    /**
     * Provides a default {@link ObjectMapper} configured for general-purpose serialization and deserialization.
     * <p>
     * This mapper registers the {@link JavaTimeModule} and disables writing dates as timestamps. Suitable for Redis or messaging scenarios where
     * domain-specific serializers are not required.
     *
     * @param builder the {@link Jackson2ObjectMapperBuilder} used to create the mapper
     * @return a configured {@link ObjectMapper} instance ready for general serialization
     */
    @Bean
    public ObjectMapper defaultObjectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.createXmlMapper(false)
                .build()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    /**
     * Provides an {@link ObjectMapper} capable of handling collections with polymorphic types.
     * <p>
     * This mapper activates default typing for non-final classes, allowing serialization and deserialization of heterogeneous object graphs (e.g., collections
     * with multiple subtypes). Also registers {@link JavaTimeModule} and disables timestamp serialization for dates.
     * <p>
     * Suitable for caching, messaging, or persistence scenarios requiring polymorphic deserialization.
     *
     * @param builder the {@link Jackson2ObjectMapperBuilder} used to create the mapper
     * @return a configured {@link ObjectMapper} instance for polymorphic collections
     */
    @Bean
    public ObjectMapper collectionObjectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.createXmlMapper(false)
                .build()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(Object.class)
                                .build(),
                        ObjectMapper.DefaultTyping.NON_FINAL
                );
    }
}
