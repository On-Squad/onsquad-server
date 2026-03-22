package revi1337.onsquad.squad_member.domain.entity;

import static revi1337.onsquad.squad_member.domain.SquadRole.GENERAL;
import static revi1337.onsquad.squad_member.domain.SquadRole.LEADER;

import java.time.LocalDateTime;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.squad.domain.entity.Squad;

public abstract class SquadMemberFactory {

    public static SquadMember leader(Squad squad, Member member, LocalDateTime participantAt) {
        return new SquadMember(squad, member, LEADER, participantAt);
    }

    public static SquadMember general(Squad squad, Member member, LocalDateTime participantAt) {
        return new SquadMember(squad, member, GENERAL, participantAt);
    }
}
