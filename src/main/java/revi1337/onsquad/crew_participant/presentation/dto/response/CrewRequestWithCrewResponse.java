package revi1337.onsquad.crew_participant.presentation.dto.response;

import revi1337.onsquad.crew.presentation.dto.response.SimpleCrewResponse;
import revi1337.onsquad.crew_participant.application.dto.CrewRequestWithCrewDto;

public record CrewRequestWithCrewResponse(
        CrewRequestResponse request,
        SimpleCrewResponse crew
) {
    public static CrewRequestWithCrewResponse from(CrewRequestWithCrewDto crewParticipantRequest) {
        return new CrewRequestWithCrewResponse(
                CrewRequestResponse.from(crewParticipantRequest.request()),
                SimpleCrewResponse.from(crewParticipantRequest.crew())
        );
    }
}
