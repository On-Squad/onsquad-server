package revi1337.onsquad.common.fixture;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.squad.domain.entity.Squad;
import revi1337.onsquad.squad.domain.model.SquadCreateSpec;
import revi1337.onsquad.squad.domain.vo.Title;

public class SquadFixture {

    public static Squad createSquad(Crew crew, Member member) {
        return Squad.create(
                new SquadCreateSpec(
                        "title",
                        "content",
                        10,
                        "addr",
                        "addr-detail",
                        List.of(),
                        "https://kakao-link.com",
                        "https://discord-link.com"
                ),
                crew,
                member,
                LocalDateTime.now()
        );
    }

    public static Squad createSquad(Crew crew, Member member, String title) {
        return Squad.create(
                new SquadCreateSpec(
                        title,
                        "content",
                        10,
                        "addr",
                        "addr-detail",
                        List.of(),
                        "https://kakao-link.com",
                        "https://discord-link.com"
                ),
                crew,
                member,
                LocalDateTime.now()
        );
    }

    public static Squad createSquad(Crew crew, Member member, int capacity) {
        return Squad.create(
                new SquadCreateSpec(
                        "title",
                        "content",
                        capacity,
                        "addr",
                        "addr-detail",
                        List.of(),
                        "https://kakao-link.com",
                        "https://discord-link.com"
                ),
                crew,
                member,
                LocalDateTime.now()
        );
    }

    public static Squad createSquad(Crew crew, Member member, LocalDateTime leaderParticipantAt) {
        return Squad.create(
                new SquadCreateSpec(
                        "title",
                        "content",
                        10,
                        "addr",
                        "addr-detail",
                        List.of(),
                        "https://kakao-link.com",
                        "https://discord-link.com"
                ),
                crew,
                member,
                leaderParticipantAt
        );
    }

    public static Squad createSquad(Long id, Crew crew, Member member, String title) {
        Squad squad = createSquad(id.intValue(), crew, member);
        ReflectionTestUtils.setField(squad, "id", id);
        ReflectionTestUtils.setField(squad, "title", new Title(title));
        return squad;
    }

    public static Squad createSquad(Long id, Crew crew, Member member) {
        Squad squad = createSquad(id.intValue(), crew, member);
        ReflectionTestUtils.setField(squad, "id", id);
        return squad;
    }

    public static Squad createSquad(Long id, Crew crew, Member member, int capacity) {
        Squad squad = createSquad(id.intValue(), crew, member, capacity);
        ReflectionTestUtils.setField(squad, "id", id);
        return squad;
    }

    public static Squad createSquad(int sequence, Crew crew, Member member) {
        return Squad.create(
                new SquadCreateSpec(
                        "title" + sequence,
                        "content" + sequence,
                        10,
                        "addr" + sequence,
                        "addr-detail",
                        List.of(),
                        "https://kakao-link.com" + sequence,
                        "https://discord-link.com" + sequence
                ),
                crew,
                member,
                LocalDateTime.now()
        );
    }

    public static Squad createSquad(int sequence, Crew crew, Member member, int capacity) {
        return Squad.create(
                new SquadCreateSpec(
                        "title" + sequence,
                        "content" + sequence,
                        capacity,
                        "addr" + sequence,
                        "addr-detail",
                        List.of(),
                        "https://kakao-link.com" + sequence,
                        "https://discord-link.com" + sequence
                ),
                crew,
                member,
                LocalDateTime.now()
        );
    }
}
