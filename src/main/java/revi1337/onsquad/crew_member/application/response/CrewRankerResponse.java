package revi1337.onsquad.crew_member.application.response;

import java.time.LocalDateTime;
import revi1337.onsquad.crew_member.domain.entity.CrewRanker;
import revi1337.onsquad.member.domain.vo.Mbti;

public record CrewRankerResponse(
        Long crewId,
        Long memberId,
        String nickname,
        String mbti,
        int rank,
        long score,
        LocalDateTime lastActivityTime
) {

    public static CrewRankerResponse from(CrewRanker crewRanker) {
        return new CrewRankerResponse(
                crewRanker.getCrewId(),
                crewRanker.getMemberId(),
                crewRanker.getNickname(),
                Mbti.getOrDefault(crewRanker.getMbti()),
                crewRanker.getRank(),
                crewRanker.getScore(),
                crewRanker.getLastActivityTime()
        );
    }
}
