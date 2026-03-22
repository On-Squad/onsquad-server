package revi1337.onsquad.crew.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_DETAIL_VALUE;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_IMAGE_LINK_VALUE;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_INTRODUCE_VALUE;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_KAKAO_LINK_VALUE;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_NAME_VALUE;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_UPDATED_DETAIL_VALUE;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_UPDATED_IMAGE_LINK_VALUE;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_UPDATED_INTRODUCE_VALUE;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_UPDATED_KAKAO_LINK_VALUE;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_UPDATED_NAME_VALUE;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createAndong;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.jdbc.Sql;
import revi1337.onsquad.common.ApplicationLayerTestSupport;
import revi1337.onsquad.crew.application.dto.CrewCreateDto;
import revi1337.onsquad.crew.application.dto.CrewUpdateDto;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.error.CrewBusinessException;
import revi1337.onsquad.crew.domain.repository.CrewRepository;
import revi1337.onsquad.crew_hashtag.domain.repository.CrewHashtagRepository;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.infrastructure.aws.s3.event.FileDeleteEvent;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberRepository;

@Sql({"/h2-hashtag.sql"})
class CrewCommandServiceTest extends ApplicationLayerTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CrewRepository crewRepository;

    @Autowired
    private CrewHashtagRepository crewHashtagRepository;

    @MockBean
    private CrewContextHandler contextHandler;

    @Autowired
    private ApplicationEvents events;

    @Autowired
    private CrewCommandService crewCommandService;

    @Autowired
    private CrewContextHandler crewContextHandler;

    @Nested
    class newCrew {

        @Test
        void test1() {
            Long memberId = memberRepository.save(createRevi()).getId();
            CrewCreateDto dto = new CrewCreateDto(
                    CREW_NAME_VALUE,
                    CREW_INTRODUCE_VALUE,
                    CREW_DETAIL_VALUE,
                    List.of(HashtagType.ACTIVE, HashtagType.ESCAPE),
                    CREW_KAKAO_LINK_VALUE
            );
            clearPersistenceContext();

            Long crewId = crewCommandService.newCrew(memberId, dto, CREW_IMAGE_LINK_VALUE);

            assertThat(crewRepository.findById(crewId)).isPresent();
            assertThat(crewHashtagRepository.fetchHashtagsByCrewIdIn(List.of(crewId))).hasSize(2);
        }
    }

    @Nested
    class updateCrew {

        @Test
        void test1() {
            Member member = memberRepository.save(createRevi());
            Crew crew = crewRepository.save(createCrew(member));
            CrewUpdateDto dto = new CrewUpdateDto(
                    CREW_UPDATED_NAME_VALUE,
                    CREW_UPDATED_INTRODUCE_VALUE,
                    CREW_UPDATED_DETAIL_VALUE,
                    List.of(HashtagType.FOODIE, HashtagType.PASSIONATE, HashtagType.MOVIE),
                    CREW_UPDATED_KAKAO_LINK_VALUE
            );
            clearPersistenceContext();

            crewCommandService.updateCrew(member.getId(), crew.getId(), dto);

            assertSoftly(softly -> {
                clearPersistenceContext();
                Crew updatedCrew = crewRepository.findById(crew.getId()).get();
                softly.assertThat(updatedCrew.getName().getValue()).isEqualTo(CREW_UPDATED_NAME_VALUE);
                softly.assertThat(updatedCrew.getIntroduce().getValue()).isEqualTo(CREW_UPDATED_INTRODUCE_VALUE);
                softly.assertThat(updatedCrew.getDetail().getValue()).isEqualTo(CREW_UPDATED_DETAIL_VALUE);
                softly.assertThat(crewHashtagRepository.fetchHashtagsByCrewIdIn(List.of(updatedCrew.getId()))).hasSize(3);
                softly.assertThat(updatedCrew.getKakaoLink()).isEqualTo(CREW_UPDATED_KAKAO_LINK_VALUE);
            });
        }

    }

    @Nested
    class updateImage {

        @Test
        void test1() {
            Member member = memberRepository.save(createRevi());
            Crew crew = crewRepository.save(createCrew(member));

            crewCommandService.updateImage(member.getId(), crew.getId(), CREW_UPDATED_IMAGE_LINK_VALUE);

            assertSoftly(softly -> {
                clearPersistenceContext();
                Crew updatedCrew = crewRepository.findById(crew.getId()).get();
                softly.assertThat(events.stream(FileDeleteEvent.class).count()).isEqualTo(1);
                softly.assertThat(updatedCrew.hasImage()).isTrue();
                softly.assertThat(updatedCrew.hasNotImage()).isFalse();
                softly.assertThat(updatedCrew.getImageUrl()).isEqualTo(CREW_UPDATED_IMAGE_LINK_VALUE);
            });
        }

        @Test
        void test2() {
            Member member = memberRepository.save(createRevi());
            Crew crew = crewRepository.save(createCrew(member));

            crewCommandService.updateImage(member.getId(), crew.getId(), CREW_UPDATED_IMAGE_LINK_VALUE);

            assertSoftly(softly -> {
                clearPersistenceContext();
                Crew updatedCrew = crewRepository.findById(crew.getId()).get();
                softly.assertThat(events.stream(FileDeleteEvent.class).count()).isEqualTo(1);
                softly.assertThat(updatedCrew.hasImage()).isTrue();
                softly.assertThat(updatedCrew.hasNotImage()).isFalse();
                softly.assertThat(updatedCrew.getImageUrl()).isEqualTo(CREW_UPDATED_IMAGE_LINK_VALUE);
            });
        }
    }

    @Nested
    class deleteImage {

        @Test
        void test1() {
            Member member = memberRepository.save(createRevi());
            Crew crew = crewRepository.save(createCrew(member));

            crewCommandService.deleteImage(member.getId(), crew.getId());
            clearPersistenceContext();

            Crew updatedCrew = crewRepository.findById(crew.getId()).get();
            assertSoftly(softly -> {
                softly.assertThat(events.stream(FileDeleteEvent.class).count()).isEqualTo(1);
                softly.assertThat(updatedCrew.hasImage()).isFalse();
                softly.assertThat(updatedCrew.hasNotImage()).isTrue();
            });
        }

        @Test
        void test2() {
            Member member = memberRepository.save(createRevi());
            Crew crew = crewRepository.save(createCrew(member));

            crewCommandService.deleteImage(member.getId(), crew.getId());

            assertSoftly(softly -> {
                clearPersistenceContext();
                Crew updatedCrew = crewRepository.findById(crew.getId()).get();
                softly.assertThat(events.stream(FileDeleteEvent.class).count()).isEqualTo(1);
                softly.assertThat(updatedCrew.hasImage()).isFalse();
                softly.assertThat(updatedCrew.hasNotImage()).isTrue();
            });
        }
    }

    @Nested
    class deleteCrew {

        @Test
        void test1() {
            Member member = memberRepository.save(createRevi());
            Crew crew = crewRepository.save(createCrew(member));

            crewCommandService.deleteCrew(member.getId(), crew.getId());

            verify(crewContextHandler).disposeContextWithSquads(crew);
        }

        @Test
        void test2() {
            Member member = memberRepository.save(createRevi());
            Member andong = memberRepository.save(createAndong());
            Crew crew = crewRepository.save(createCrew(member));

            assertThatThrownBy(() -> crewCommandService
                    .deleteCrew(andong.getId(), crew.getId()))
                    .isExactlyInstanceOf(CrewBusinessException.InsufficientAuthority.class);

            verify(crewContextHandler, never()).disposeContextWithSquads(any(Crew.class));
        }
    }
}
