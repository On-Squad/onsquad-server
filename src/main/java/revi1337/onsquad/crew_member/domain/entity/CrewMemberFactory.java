package revi1337.onsquad.crew_member.domain.entity;

import static revi1337.onsquad.crew_member.domain.CrewRole.GENERAL;
import static revi1337.onsquad.crew_member.domain.CrewRole.MANAGER;
import static revi1337.onsquad.crew_member.domain.CrewRole.OWNER;

import java.time.LocalDateTime;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.member.domain.entity.Member;

public abstract class CrewMemberFactory {

    public static CrewMember general(Crew crew, Member member, LocalDateTime participantAt) {
        return new CrewMember(crew, member, GENERAL, participantAt);
    }

    public static CrewMember manager(Crew crew, Member member, LocalDateTime participantAt) {
        return new CrewMember(crew, member, MANAGER, participantAt);
    }

    public static CrewMember owner(Crew crew, Member member, LocalDateTime participantAt) {
        return new CrewMember(crew, member, OWNER, participantAt);
    }
}
