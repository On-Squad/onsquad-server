package revi1337.onsquad.announce.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import revi1337.onsquad.crew_member.domain.CrewRole;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnnounceStates(
        CrewRole role,
        Boolean canPin,
        Boolean canModify,
        Boolean canWrite
) {

    public AnnounceStates(CrewRole role) {
        this(role, null, null, null);
    }

    public AnnounceStates(CrewRole role, boolean canPin, boolean canModify) {
        this(role, canPin, canModify, null);
    }

    public AnnounceStates(boolean canWrite) {
        this(null, null, null, canWrite);
    }
}
