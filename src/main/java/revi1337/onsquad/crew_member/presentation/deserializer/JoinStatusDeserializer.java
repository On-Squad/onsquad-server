package revi1337.onsquad.crew_member.presentation.deserializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import revi1337.onsquad.crew_member.domain.vo.JoinStatus;

public class JoinStatusDeserializer extends JsonDeserializer<JoinStatus> {

    @Override
    public JoinStatus deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        String joinStatus = p.getText();
        return JoinStatus.defaultEnumSet()
                .stream()
                .filter(status -> JoinStatus.checkEquivalence(status, joinStatus))
                .findFirst()
                .orElse(null);
    }
}
