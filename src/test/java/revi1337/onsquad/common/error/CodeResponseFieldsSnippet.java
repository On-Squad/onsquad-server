package revi1337.onsquad.common.error;

import org.springframework.http.MediaType;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.payload.AbstractFieldsSnippet;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadSubsectionExtractor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CodeResponseFieldsSnippet extends AbstractFieldsSnippet {

    public CodeResponseFieldsSnippet(String type,
                                     List<FieldDescriptor> descriptors,
                                     Map<String, Object> attributes,
                                     boolean ignoreUndocumentedFields,
                                     PayloadSubsectionExtractor<?> subsectionExtractor) {
        super(type, descriptors, attributes, ignoreUndocumentedFields, subsectionExtractor);
    }

    @Override
    protected MediaType getContentType(Operation operation) {
        return operation.getResponse().getHeaders().getContentType();
    }

    @Override
    protected byte[] getContent(Operation operation) throws IOException {
        return operation.getResponse().getContent();
    }
}
