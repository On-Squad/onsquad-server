package revi1337.onsquad.squad_request.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createAndong;
import static revi1337.onsquad.common.fixture.MemberFixture.createKwangwon;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;
import static revi1337.onsquad.common.fixture.SquadCategoryFixture.createSquadCategories;
import static revi1337.onsquad.common.fixture.SquadFixture.createSquad;
import static revi1337.onsquad.common.fixture.SquadMemberFixture.createGeneralSquadMember;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.common.ApplicationLayerTestSupport;
import revi1337.onsquad.common.dto.PageResponse;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.repository.CrewJpaRepository;
import revi1337.onsquad.crew_member.domain.entity.CrewMember;
import revi1337.onsquad.crew_member.domain.entity.CrewMemberFactory;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;
import revi1337.onsquad.squad.domain.entity.Squad;
import revi1337.onsquad.squad.domain.repository.SquadJpaRepository;
import revi1337.onsquad.squad_category.application.SquadCategoryAccessor;
import revi1337.onsquad.squad_category.domain.repository.SquadCategoryJdbcRepository;
import revi1337.onsquad.squad_request.application.response.MySquadRequestResponse;
import revi1337.onsquad.squad_request.application.response.SquadRequestResponse;
import revi1337.onsquad.squad_request.domain.entity.SquadRequest;
import revi1337.onsquad.squad_request.domain.repository.SquadRequestJpaRepository;

@Sql({"/h2-truncate.sql", "/h2-category.sql"})
class SquadRequestQueryServiceTest extends ApplicationLayerTestSupport {

    @Autowired
    private MemberJpaRepository memberRepository;

    @Autowired
    private CrewJpaRepository crewRepository;

    @Autowired
    private SquadJpaRepository squadRepository;

    @Autowired
    private SquadRequestJpaRepository squadRequestRepository;

    @Autowired
    private SquadCategoryJdbcRepository squadCategoryJdbcRepository;

    @SpyBean
    private SquadCategoryAccessor squadCategoryAccessor;

    @Autowired
    private SquadRequestQueryService squadRequestQueryService;

    @Nested
    @DisplayName("스쿼드별 전체 신청 목록 조회")
    class fetchAllRequests {

        @Test
        @DisplayName("스쿼드 리더는 해당 스쿼드에 들어온 모든 참가 신청 내역을 조회할 수 있다.")
        void success() {
            Member revi = memberRepository.save(createRevi());
            Member andong = memberRepository.save(createAndong());
            Member kwangwon = memberRepository.save(createKwangwon());
            Crew crew = crewRepository.save(createCrew(revi));
            Squad squad = squadRepository.save(createSquad(crew, revi));
            LocalDateTime baseTime = LocalDate.of(2026, 1, 4).atStartOfDay();
            SquadRequest request1 = squadRequestRepository.save(createSquadRequest(squad, andong, baseTime.plusHours(2)));
            SquadRequest request2 = squadRequestRepository.save(createSquadRequest(squad, kwangwon, baseTime.plusHours(1)));
            clearPersistenceContext();
            PageRequest pageRequest = PageRequest.of(0, 4, Sort.by("requestAt").descending());

            PageResponse<SquadRequestResponse> response = squadRequestQueryService.fetchAllRequests(revi.getId(), squad.getId(), pageRequest);

            assertSoftly(softly -> {
                List<SquadRequestResponse> results = response.results();
                softly.assertThat(results.get(0).id()).isEqualTo(request1.getId());
                softly.assertThat(results.get(1).id()).isEqualTo(request2.getId());
            });
        }

        @Test
        @DisplayName("스쿼드 리더 권한이 없는 회원이 신청 목록 조회를 시도하면 예외가 발생한다.")
        void fail() {
            Member revi = memberRepository.save(createRevi());
            Member andong = memberRepository.save(createAndong());
            Member kwangwon = memberRepository.save(createKwangwon());
            Crew crew = crewRepository.save(createCrew(revi));
            Squad squad = createSquad(crew, revi);
            squad.addMembers(createGeneralSquadMember(squad, andong));
            squadRepository.save(squad);
            LocalDateTime baseTime = LocalDate.of(2026, 1, 4).atStartOfDay();
            squadRequestRepository.save(createSquadRequest(squad, kwangwon, baseTime.plusHours(1)));
            clearPersistenceContext();
            PageRequest pageRequest = PageRequest.of(0, 4, Sort.by("requestAt").descending());

            assertThatThrownBy(() -> squadRequestQueryService.fetchAllRequests(andong.getId(), squad.getId(), pageRequest));
        }
    }

    @Nested
    @DisplayName("나의 스쿼드 신청 내역 조회")
    class fetchMyRequests {

        @Test
        @DisplayName("사용자가 보낸 신청 내역이 존재할 경우, 스쿼드의 카테고리 정보가 포함된 목록을 최신순으로 반환한다.")
        void success1() {
            Member revi = memberRepository.save(createRevi());
            Member andong = memberRepository.save(createAndong());
            Member kwangwon = memberRepository.save(createKwangwon());
            Crew crew = createCrew(revi);
            crew.addCrewMember(createGeneralCrewMember(crew, andong));
            crew.addCrewMember(createGeneralCrewMember(crew, kwangwon));
            crewRepository.save(crew);
            Squad squad1 = squadRepository.save(createSquad(crew, revi));
            squadCategoryJdbcRepository.insertBatch(createSquadCategories(squad1, List.of(CategoryType.GAME, CategoryType.MANGACAFE)));
            Squad squad2 = squadRepository.save(createSquad(crew, andong));
            squadCategoryJdbcRepository.insertBatch(createSquadCategories(squad2, List.of(CategoryType.MOVIE, CategoryType.BADMINTON)));
            LocalDateTime baseTime = LocalDate.of(2026, 1, 4).atStartOfDay();
            SquadRequest request1 = createSquadRequest(squad1, kwangwon, baseTime.plusHours(2));
            SquadRequest request2 = createSquadRequest(squad2, kwangwon, baseTime.plusHours(1));
            squadRequestRepository.saveAll(List.of(request1, request2));
            clearPersistenceContext();
            PageRequest pageRequest = PageRequest.of(0, 4, Sort.by("requestAt").descending());

            PageResponse<MySquadRequestResponse> response = squadRequestQueryService.fetchMyRequests(kwangwon.getId(), pageRequest);

            assertSoftly(softly -> {
                softly.assertThat(response.totalCount()).isEqualTo(2);
                List<MySquadRequestResponse> results = response.results();

                softly.assertThat(results.get(0).id()).isEqualTo(request1.getId());
                softly.assertThat(results.get(0).squad().categories())
                        .containsExactlyInAnyOrder(CategoryType.GAME.getText(), CategoryType.MANGACAFE.getText());

                softly.assertThat(results.get(1).id()).isEqualTo(request2.getId());
                softly.assertThat(results.get(1).squad().categories())
                        .containsExactlyInAnyOrder(CategoryType.MOVIE.getText(), CategoryType.BADMINTON.getText());

                verify(squadCategoryAccessor, times(1)).fetchCategoriesBySquadIdIn(anyList());
            });
        }

        @Test
        @DisplayName("사용자가 보낸 신청 내역이 없을 경우, 빈 목록을 반환하며 카테고리 조회를 수행하지 않는다.")
        void success2() {
            Member revi = memberRepository.save(createRevi());
            Member andong = memberRepository.save(createAndong());
            Member kwangwon = memberRepository.save(createKwangwon());
            Crew crew = createCrew(revi);
            crew.addCrewMember(createGeneralCrewMember(crew, andong));
            crew.addCrewMember(createGeneralCrewMember(crew, kwangwon));
            crewRepository.save(crew);
            squadRepository.save(createSquad(crew, revi));
            squadRepository.save(createSquad(crew, andong));
            clearPersistenceContext();
            PageRequest pageRequest = PageRequest.of(0, 4, Sort.by("requestAt").descending());

            PageResponse<MySquadRequestResponse> response = squadRequestQueryService.fetchMyRequests(kwangwon.getId(), pageRequest);

            assertSoftly(softly -> {
                softly.assertThat(response.totalCount()).isEqualTo(0);
                verify(squadCategoryAccessor, never()).fetchCategoriesBySquadIdIn(any());
            });
        }
    }

    private static CrewMember createGeneralCrewMember(Crew crew, Member member) {
        return CrewMemberFactory.general(crew, member, LocalDateTime.now());
    }

    private static SquadRequest createSquadRequest(Squad squad, Member member, LocalDateTime requestAt) {
        return SquadRequest.of(squad, member, requestAt);
    }
}
