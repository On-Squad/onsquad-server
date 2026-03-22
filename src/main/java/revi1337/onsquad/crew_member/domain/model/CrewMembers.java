package revi1337.onsquad.crew_member.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import revi1337.onsquad.crew_member.domain.entity.CrewMember;
import revi1337.onsquad.crew_member.domain.CrewRole;

public class CrewMembers {

    private final List<CrewMember> crewMembers;

    public CrewMembers(List<CrewMember> crewMembers) {
        this.crewMembers = Collections.unmodifiableList(crewMembers);
    }

    public Map<Long, CrewRole> splitRolesByMemberId() {
        return crewMembers.stream()
                .collect(Collectors.toMap(crewMember -> crewMember.getMember().getId(), CrewMember::getRole));
    }
}
