package revi1337.onsquad.squad.domain.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createAndong;
import static revi1337.onsquad.common.fixture.MemberFixture.createKwangwon;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;
import static revi1337.onsquad.common.fixture.SquadMemberFixture.createGeneralSquadMember;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.squad.domain.error.SquadDomainException;
import revi1337.onsquad.squad.domain.model.SquadCreateSpec;
import revi1337.onsquad.squad_member.domain.SquadRole;
import revi1337.onsquad.squad_member.domain.entity.SquadMember;

class SquadTest {

    @Test
    @DisplayName("SquadCreateSpec을 통해 Squad를 생성하면 방장이 멤버로 등록되고 남은 정원이 계산된다.")
    void create() {
        Member member = createRevi(1L);
        Member andong = createAndong(2L);
        Crew crew = createCrew(1L, member);
        SquadCreateSpec createSpec = new SquadCreateSpec(
                "title",
                "content",
                20,
                "addr",
                null,
                List.of(),
                "https://카카오_링크.com",
                "https://디스코드_링크.com"
        );

        Squad squad = Squad.create(createSpec, crew, andong, LocalDateTime.now());

        assertSoftly(softly -> {
            softly.assertThat(squad.getTitle().getValue()).isEqualTo("title");
            softly.assertThat(squad.getContent().getValue()).isEqualTo("content");
            softly.assertThat(squad.getCapacity()).isEqualTo(20);
            softly.assertThat(squad.getRemain()).isEqualTo(19);
            softly.assertThat(squad.getAddress().getValue()).isEqualTo("addr");
            softly.assertThat(squad.getAddress().getDetail()).isNull();
            softly.assertThat(squad.getKakaoLink()).isNotNull();
            softly.assertThat(squad.getDiscordLink()).isNotNull();

            softly.assertThat(squad.getCrew()).isNotNull();
            softly.assertThat(squad.getMember()).isNotNull();
        });
    }

    @Test
    @DisplayName("새로운 멤버를 추가하면 현재 인원이 증가하고 남은 정원이 감소한다.")
    void addMembers() {
        Member member = createRevi(1L);
        Member andong = createAndong(2L);
        Crew crew = createCrew(1L, member);
        SquadCreateSpec createSpec = new SquadCreateSpec(
                "title",
                "content",
                20,
                "addr",
                null,
                List.of(),
                "https://카카오_링크.com",
                "https://디스코드_링크.com"
        );
        Squad squad = Squad.create(createSpec, crew, andong, LocalDateTime.now());

        squad.addMembers(createGeneralSquadMember(squad, createKwangwon(3L)));

        assertSoftly(softly -> {
            softly.assertThat(squad.getMembers().size()).isEqualTo(2);
            softly.assertThat(squad.getCurrentSize()).isEqualTo(2);
            softly.assertThat(squad.getRemain()).isEqualTo(18);
        });
    }

    @Test
    @DisplayName("인원수를 수동으로 증가시킬 때 남은 정원이 정상적으로 차감된다.")
    void increaseCurrentSize() {
        Member member = createRevi(1L);
        Member andong = createAndong(2L);
        Crew crew = createCrew(1L, member);
        SquadCreateSpec createSpec = new SquadCreateSpec(
                "title",
                "content",
                2,
                "addr",
                null,
                List.of(),
                "https://카카오_링크.com",
                "https://디스코드_링크.com"
        );
        Squad squad = Squad.create(createSpec, crew, andong, LocalDateTime.now());

        squad.increaseCurrentSize();

        assertSoftly(softly -> {
            softly.assertThat(squad.getCurrentSize()).isEqualTo(2);
            softly.assertThat(squad.getRemain()).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("최대 정원을 초과하여 인원을 증가시키려 하면 예외가 발생한다.")
    void increaseCurrentSize2() {
        Member member = createRevi(1L);
        Member andong = createAndong(2L);
        Crew crew = createCrew(1L, member);
        SquadCreateSpec createSpec = new SquadCreateSpec(
                "title",
                "content",
                2,
                "addr",
                null,
                List.of(),
                "https://카카오_링크.com",
                "https://디스코드_링크.com"
        );
        Squad squad = Squad.create(createSpec, crew, andong, LocalDateTime.now());
        squad.increaseCurrentSize();

        assertThatThrownBy(squad::increaseCurrentSize)
                .isExactlyInstanceOf(SquadDomainException.NotEnoughLeft.class);
    }

    @Test
    @DisplayName("인원수를 감소시킬 때 현재 인원은 줄어들고 남은 정원은 늘어난다.")
    void decreaseCurrentSize() {
        Member member = createRevi(1L);
        Member andong = createAndong(2L);
        Crew crew = createCrew(1L, member);
        SquadCreateSpec createSpec = new SquadCreateSpec(
                "title",
                "content",
                2,
                "addr",
                null,
                List.of(),
                "https://카카오_링크.com",
                "https://디스코드_링크.com"
        );
        Squad squad = Squad.create(createSpec, crew, andong, LocalDateTime.now());

        squad.decreaseCurrentSize();

        assertSoftly(softly -> {
            softly.assertThat(squad.getCurrentSize()).isEqualTo(0);
            softly.assertThat(squad.getRemain()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("인원수가 0명 미만으로 감소하려 하면 예외가 발생한다.")
    void decreaseCurrentSize2() {
        Member member = createRevi(1L);
        Member andong = createAndong(2L);
        Crew crew = createCrew(1L, member);
        SquadCreateSpec createSpec = new SquadCreateSpec(
                "title",
                "content",
                2,
                "addr",
                null,
                List.of(),
                "https://카카오_링크.com",
                "https://디스코드_링크.com"
        );
        Squad squad = Squad.create(createSpec, crew, andong, LocalDateTime.now());
        squad.decreaseCurrentSize();

        assertThatThrownBy(squad::decreaseCurrentSize)
                .isExactlyInstanceOf(SquadDomainException.UnderflowSize.class);
    }

    @Test
    @DisplayName("방장 권한을 위임하면 기존 방장은 일반 멤버가 되고 엔티티의 대표 멤버 정보가 업데이트된다.")
    void delegateLeader() {
        Member member = createRevi(1L);
        Member andong = createAndong(2L);
        Crew crew = createCrew(1L, member);
        SquadCreateSpec createSpec = new SquadCreateSpec(
                "title",
                "content",
                2,
                "addr",
                null,
                List.of(),
                "https://카카오_링크.com",
                "https://디스코드_링크.com"
        );
        Squad squad = Squad.create(createSpec, crew, andong, LocalDateTime.now());
        SquadMember leader = squad.getMembers().get(0);
        SquadMember squadMember = createGeneralSquadMember(squad, createKwangwon(3L));
        squad.addMembers(squadMember);

        squad.delegateLeader(leader, squadMember);

        assertSoftly(softly -> {
            softly.assertThat(squad.getMember()).isEqualTo(createKwangwon(3L));
            softly.assertThat(leader.getRole()).isSameAs(SquadRole.GENERAL);
            softly.assertThat(squadMember.getRole()).isSameAs(SquadRole.LEADER);
        });
    }
}
