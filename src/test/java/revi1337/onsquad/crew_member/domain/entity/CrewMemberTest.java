package revi1337.onsquad.crew_member.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew_member.domain.CrewRole;
import revi1337.onsquad.member.domain.entity.Member;

class CrewMemberTest {

    @Nested
    @DisplayName("CrewMemberFactory 생성 검증")
    class factoryTest {

        @Test
        void createGeneral() {
            Member member = createRevi(1L);
            Crew crew = createCrew(2L, member);

            CrewMember crewMember = CrewMemberFactory.general(crew, member, LocalDateTime.now());

            assertThat(crewMember.getMember()).isEqualTo(member);
            assertThat(crewMember.getCrew()).isEqualTo(crew);
            assertThat(crewMember.getRole()).isSameAs(CrewRole.GENERAL);
        }

        @Test
        void createManager() {
            Member member = createRevi(1L);
            Crew crew = createCrew(2L, member);

            CrewMember crewMember = CrewMemberFactory.manager(crew, member, LocalDateTime.now());

            assertThat(crewMember.getMember()).isEqualTo(member);
            assertThat(crewMember.getCrew()).isEqualTo(crew);
            assertThat(crewMember.getRole()).isSameAs(CrewRole.MANAGER);
        }

        @Test
        void createOwner() {
            Member member = createRevi(1L);
            Crew crew = createCrew(2L, member);

            CrewMember crewMember = CrewMemberFactory.owner(crew, member, LocalDateTime.now());

            assertThat(crewMember.getMember()).isEqualTo(member);
            assertThat(crewMember.getCrew()).isEqualTo(crew);
            assertThat(crewMember.getRole()).isSameAs(CrewRole.OWNER);
        }
    }

    @Nested
    @DisplayName("CrewMember 도메인 로직 검증")
    class DomainLogicTest {

        @Test
        void leaveCrew() {
            Member member = createRevi(1L);
            Crew crew = createCrew(2L, member);
            CrewMember crewMember = CrewMemberFactory.general(crew, member, LocalDateTime.now());
            crew.addCrewMember(crewMember);

            crewMember.leaveCrew();

            assertThat(crewMember.getMember()).isNull();
            assertThat(crewMember.getCrew()).isNull();
            assertThat(crew.getCurrentSize()).isEqualTo(1);
        }

        @Test
        void releaseCrew() {
            Member member = createRevi(1L);
            Crew crew = createCrew(member);
            CrewMember crewMember = CrewMemberFactory.general(crew, member, LocalDateTime.now());

            crewMember.releaseCrew();

            assertThat(crewMember.getCrew()).isNull();
        }

        @Test
        void promoteToOwner() {
            Member member = createRevi(1L);
            Crew crew = createCrew(member);
            CrewMember crewMember = CrewMemberFactory.general(crew, member, LocalDateTime.now());

            crewMember.promoteToOwner();

            assertThat(crewMember.getRole()).isSameAs(CrewRole.OWNER);
        }

        @Test
        void demoteToGeneral() {
            Member member = createRevi(1L);
            Crew crew = createCrew(member);
            CrewMember crewMember = CrewMemberFactory.owner(crew, member, LocalDateTime.now());

            crewMember.demoteToGeneral();

            assertThat(crewMember.getRole()).isSameAs(CrewRole.GENERAL);
        }

        @Test
        void isLowerThanManager1() {
            Member member = createRevi(1L);
            Crew crew = createCrew(member);
            CrewMember crewMember = CrewMemberFactory.general(crew, member, LocalDateTime.now());

            boolean lowerThanManager = crewMember.isLowerThanManager();

            assertThat(lowerThanManager).isTrue();
        }

        @Test
        void isLowerThanManager2() {
            Member member = createRevi(1L);
            Crew crew = createCrew(member);
            CrewMember crewMember = CrewMemberFactory.manager(crew, member, LocalDateTime.now());

            boolean lowerThanManager = crewMember.isLowerThanManager();

            assertThat(lowerThanManager).isFalse();
        }

        @Test
        void isLowerThanManager3() {
            Member member = createRevi(1L);
            Crew crew = createCrew(member);
            CrewMember crewMember = CrewMemberFactory.owner(crew, member, LocalDateTime.now());

            boolean lowerThanManager = crewMember.isLowerThanManager();

            assertThat(lowerThanManager).isFalse();
        }

        @Test
        void isManagerOrHigher1() {
            Member member = createRevi(1L);
            Crew crew = createCrew(member);
            CrewMember crewMember = CrewMemberFactory.general(crew, member, LocalDateTime.now());

            boolean lowerThanManager = crewMember.isManagerOrHigher();

            assertThat(lowerThanManager).isFalse();
        }

        @Test
        void isManagerOrHigher2() {
            Member member = createRevi(1L);
            Crew crew = createCrew(member);
            CrewMember crewMember = CrewMemberFactory.manager(crew, member, LocalDateTime.now());

            boolean lowerThanManager = crewMember.isManagerOrHigher();

            assertThat(lowerThanManager).isTrue();
        }

        @Test
        void isManagerOrHigher3() {
            Member member = createRevi(1L);
            Crew crew = createCrew(member);
            CrewMember crewMember = CrewMemberFactory.owner(crew, member, LocalDateTime.now());

            boolean lowerThanManager = crewMember.isManagerOrHigher();

            assertThat(lowerThanManager).isTrue();
        }

        @Test
        void isManagerOrLower1() {
            Member member = createRevi(1L);
            Crew crew = createCrew(member);
            CrewMember crewMember = CrewMemberFactory.general(crew, member, LocalDateTime.now());

            boolean managerOrLower = crewMember.isManagerOrLower();

            assertThat(managerOrLower).isTrue();
        }

        @Test
        void isManagerOrLower2() {
            Member member = createRevi(1L);
            Crew crew = createCrew(member);
            CrewMember crewMember = CrewMemberFactory.manager(crew, member, LocalDateTime.now());

            boolean managerOrLower = crewMember.isManagerOrLower();

            assertThat(managerOrLower).isTrue();
        }

        @Test
        void isManagerOrLower3() {
            Member member = createRevi(1L);
            Crew crew = createCrew(member);
            CrewMember crewMember = CrewMemberFactory.owner(crew, member, LocalDateTime.now());

            boolean managerOrLower = crewMember.isManagerOrLower();

            assertThat(managerOrLower).isFalse();
        }
    }
}
