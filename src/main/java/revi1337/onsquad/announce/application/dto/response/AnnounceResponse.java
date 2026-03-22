package revi1337.onsquad.announce.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import revi1337.onsquad.announce.domain.entity.Announce;
import revi1337.onsquad.crew_member.domain.CrewRole;
import revi1337.onsquad.member.application.dto.response.SimpleMemberResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnnounceResponse(
        AnnounceStates states,
        Long id,
        String title,
        String content,
        LocalDateTime createdAt,
        boolean pinned,
        LocalDateTime pinnedAt,
        SimpleMemberResponse writer
) {

    public static AnnounceResponse from(CrewRole role, Announce announce) {
        return new AnnounceResponse(
                new AnnounceStates(role),
                announce.getId(),
                announce.getTitle().getValue(),
                announce.getContent(),
                announce.getCreatedAt(),
                announce.isPinned(),
                announce.getPinnedAt(),
                announce.getMember() != null ? SimpleMemberResponse.from(announce.getMember()) : SimpleMemberResponse.DELETED_MEMBER
        );
    }
}
