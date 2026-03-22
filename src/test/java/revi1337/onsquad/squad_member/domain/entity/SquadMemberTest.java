package revi1337.onsquad.squad_member.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createMember;
import static revi1337.onsquad.common.fixture.SquadFixture.createSquad;
import static revi1337.onsquad.common.fixture.SquadMemberFixture.createGeneralSquadMember;
import static revi1337.onsquad.common.fixture.SquadMemberFixture.createLeaderSquadMember;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.squad.domain.entity.Squad;
import revi1337.onsquad.squad_member.domain.SquadRole;

class SquadMemberTest {

    @Test
    @DisplayName("스쿼드 멤버 퇴장 시, 스쿼드의 현재 인원수가 감소하고 연관관계가 해제된다.")
    void leaveSquad() {
        Member member = createMember(1L);
        Crew crew = createCrew(1L, member);
        Squad squad = createSquad(1L, crew, member, 10);
        SquadMember squadMember = createGeneralSquadMember(squad, member);
        squad.addMembers(squadMember);

        squadMember.leaveSquad();

        assertSoftly(softly -> {
            softly.assertThat(squadMember.getSquad()).isNull();
            softly.assertThat(squadMember.getMember()).isNull();
        });
    }

    @Nested
    @DisplayName("역할 변경 테스트")
    class RoleUpdate {

        @Test
        @DisplayName("일반 멤버를 리더로 승격시킬 수 있다.")
        void promoteToLeader() {
            SquadMember squadMember = createGeneralSquadMember(null, null);

            squadMember.promoteToLeader();

            assertSoftly(softly -> {
                softly.assertThat(squadMember.getRole()).isEqualTo(SquadRole.LEADER);
                softly.assertThat(squadMember.isLeader()).isTrue();
            });
        }

        @Test
        @DisplayName("리더를 일반 멤버로 강등시킬 수 있다.")
        void demoteToGeneral() {
            SquadMember squadMember = createLeaderSquadMember(null, null);

            squadMember.demoteToGeneral();

            assertThat(squadMember.getRole()).isEqualTo(SquadRole.GENERAL);
            assertThat(squadMember.isLeader()).isFalse();
        }
    }

    @Test
    @DisplayName("동일한 ID를 가진 스쿼드 멤버 객체는 동등한 것으로 판단한다.")
    void equalsAndHashCode() {
        SquadMember sm1 = createGeneralSquadMember(null, null);
        SquadMember sm2 = createGeneralSquadMember(null, null);
        ReflectionTestUtils.setField(sm1, "id", 1L);
        ReflectionTestUtils.setField(sm2, "id", 1L);

        assertThat(sm1).isEqualTo(sm2);
        assertThat(sm1.hashCode()).isEqualTo(sm2.hashCode());
    }
}
