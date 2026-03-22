package revi1337.onsquad.crew.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_UPDATED_NAME_VALUE;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createAndong;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import revi1337.onsquad.common.ApplicationLayerTestSupport;
import revi1337.onsquad.common.dto.DuplicateResponse;
import revi1337.onsquad.common.dto.PageResponse;
import revi1337.onsquad.crew.application.dto.response.CrewResponse;
import revi1337.onsquad.crew.application.dto.response.CrewWithParticipantStateResponse;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.repository.CrewRepository;
import revi1337.onsquad.crew_hashtag.domain.repository.CrewHashtagRepository;
import revi1337.onsquad.crew_request.domain.entity.CrewRequest;
import revi1337.onsquad.crew_request.domain.repository.CrewRequestRepository;
import revi1337.onsquad.hashtag.domain.entity.Hashtag;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberRepository;

@Sql({"/h2-hashtag.sql"})
class CrewQueryServiceTest extends ApplicationLayerTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CrewRepository crewRepository;

    @Autowired
    private CrewHashtagRepository crewHashtagRepository;

    @Autowired
    private CrewRequestRepository crewRequestRepository;

    @Autowired
    private CrewQueryService crewQueryService;

    @Test
    @DisplayName("이미 존재하는 크루 이름으로 중복 확인 시, true를 반환한다")
    void checkNameDuplicate1() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));

        DuplicateResponse response = crewQueryService.checkNameDuplicate(crew.getName().getValue());

        assertThat(response.duplicate()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 크루 이름으로 중복 확인 시, false를 반환한다")
    void checkNameDuplicate2() {
        Member member = memberRepository.save(createRevi());
        crewRepository.save(createCrew(member));

        DuplicateResponse response = crewQueryService.checkNameDuplicate(CREW_UPDATED_NAME_VALUE);

        assertThat(response.duplicate()).isFalse();
    }

    @Test
    @DisplayName("이미 참여 중인 크루의 상세 정보를 조회하면, 참여 상태(true)와 함께 크루 정보를 반환한다")
    void findCrewById1() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        crewHashtagRepository.insertBatch(crew.getId(), Hashtag.fromHashtagTypes(List.of(HashtagType.ACTIVE, HashtagType.PASSIONATE)));

        CrewWithParticipantStateResponse response = crewQueryService.findCrewById(member.getId(), crew.getId());

        assertSoftly(softly -> {
            softly.assertThat(response.states().alreadyRequest()).isNull();
            softly.assertThat(response.states().alreadyParticipant()).isTrue();
            softly.assertThat(response.id()).isEqualTo(crew.getId());
            softly.assertThat(response.hashtags()).hasSize(2);
            softly.assertThat(response.name()).isEqualTo(crew.getName().getValue());
            softly.assertThat(response.introduce()).isEqualTo(crew.getIntroduce().getValue());
            softly.assertThat(response.detail()).isEqualTo(crew.getDetail().getValue());
            softly.assertThat(response.imageUrl()).isEqualTo(crew.getImageUrl());
            softly.assertThat(response.kakaoLink()).isEqualTo(crew.getKakaoLink());
            softly.assertThat(response.detail()).isEqualTo(crew.getDetail().getValue());
            softly.assertThat(response.memberCount()).isEqualTo(crew.getCurrentSize());
            softly.assertThat(response.owner().id()).isEqualTo(member.getId());
            softly.assertThat(response.owner().email()).isNull();
            softly.assertThat(response.owner().nickname()).isEqualTo(member.getNickname().getValue());
            softly.assertThat(response.owner().introduce()).isEqualTo(member.getIntroduce().getValue());
            softly.assertThat(response.owner().mbti()).isEqualTo(member.getMbti().name());
        });
    }

    @Test
    @DisplayName("비로그인 사용자가 크루 상세 정보를 조회하면, 참여 및 신청 상태가 null로 반환된다")
    void findCrewById2() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        crewHashtagRepository.insertBatch(crew.getId(), Hashtag.fromHashtagTypes(List.of(HashtagType.ACTIVE, HashtagType.PASSIONATE)));

        CrewWithParticipantStateResponse response = crewQueryService.findCrewById(null, crew.getId());

        assertSoftly(softly -> {
            softly.assertThat(response.states().alreadyRequest()).isNull();
            softly.assertThat(response.states().alreadyParticipant()).isNull();
            softly.assertThat(response.id()).isEqualTo(crew.getId());
            softly.assertThat(response.hashtags()).hasSize(2);
            softly.assertThat(response.name()).isEqualTo(crew.getName().getValue());
            softly.assertThat(response.introduce()).isEqualTo(crew.getIntroduce().getValue());
            softly.assertThat(response.detail()).isEqualTo(crew.getDetail().getValue());
            softly.assertThat(response.imageUrl()).isEqualTo(crew.getImageUrl());
            softly.assertThat(response.kakaoLink()).isEqualTo(crew.getKakaoLink());
            softly.assertThat(response.detail()).isEqualTo(crew.getDetail().getValue());
            softly.assertThat(response.memberCount()).isEqualTo(crew.getCurrentSize());
            softly.assertThat(response.owner().id()).isEqualTo(member.getId());
            softly.assertThat(response.owner().email()).isNull();
            softly.assertThat(response.owner().nickname()).isEqualTo(member.getNickname().getValue());
            softly.assertThat(response.owner().introduce()).isEqualTo(member.getIntroduce().getValue());
            softly.assertThat(response.owner().mbti()).isEqualTo(member.getMbti().name());
        });
    }

    @Test
    @DisplayName("크루에 소속되지 않은 일반 회원이 상세 조회를 하면, 참여 및 신청 상태가 모두 false로 반환된다")
    void findCrewById3() {
        Member revi = memberRepository.save(createRevi());
        Member andong = memberRepository.save(createAndong());
        Crew crew = crewRepository.save(createCrew(revi));
        crewHashtagRepository.insertBatch(crew.getId(), Hashtag.fromHashtagTypes(List.of(HashtagType.ACTIVE, HashtagType.PASSIONATE)));

        CrewWithParticipantStateResponse response = crewQueryService.findCrewById(andong.getId(), crew.getId());

        assertSoftly(softly -> {
            softly.assertThat(response.states().alreadyRequest()).isFalse();
            softly.assertThat(response.states().alreadyParticipant()).isFalse();
            softly.assertThat(response.id()).isEqualTo(crew.getId());
            softly.assertThat(response.hashtags()).hasSize(2);
            softly.assertThat(response.name()).isEqualTo(crew.getName().getValue());
            softly.assertThat(response.introduce()).isEqualTo(crew.getIntroduce().getValue());
            softly.assertThat(response.detail()).isEqualTo(crew.getDetail().getValue());
            softly.assertThat(response.imageUrl()).isEqualTo(crew.getImageUrl());
            softly.assertThat(response.kakaoLink()).isEqualTo(crew.getKakaoLink());
            softly.assertThat(response.detail()).isEqualTo(crew.getDetail().getValue());
            softly.assertThat(response.memberCount()).isEqualTo(crew.getCurrentSize());
            softly.assertThat(response.owner().id()).isEqualTo(revi.getId());
            softly.assertThat(response.owner().email()).isNull();
            softly.assertThat(response.owner().nickname()).isEqualTo(revi.getNickname().getValue());
            softly.assertThat(response.owner().introduce()).isEqualTo(revi.getIntroduce().getValue());
            softly.assertThat(response.owner().mbti()).isEqualTo(revi.getMbti().name());
        });
    }

    @Test
    @DisplayName("크루 가입을 신청하고 대기 중인 사용자가 상세 조회를 하면, 신청 상태가 true, 참여 상태 false 로 반환된다")
    void findCrewById4() {
        Member revi = memberRepository.save(createRevi());
        Member andong = memberRepository.save(createAndong());
        Crew crew = crewRepository.save(createCrew(revi));
        crewHashtagRepository.insertBatch(crew.getId(), Hashtag.fromHashtagTypes(List.of(HashtagType.ACTIVE, HashtagType.PASSIONATE)));
        crewRequestRepository.save(CrewRequest.of(crew, andong, LocalDateTime.now()));

        CrewWithParticipantStateResponse response = crewQueryService.findCrewById(andong.getId(), crew.getId());

        assertSoftly(softly -> {
            softly.assertThat(response.states().alreadyRequest()).isTrue();
            softly.assertThat(response.states().alreadyParticipant()).isFalse();
            softly.assertThat(response.id()).isEqualTo(crew.getId());
            softly.assertThat(response.hashtags()).hasSize(2);
            softly.assertThat(response.name()).isEqualTo(crew.getName().getValue());
            softly.assertThat(response.introduce()).isEqualTo(crew.getIntroduce().getValue());
            softly.assertThat(response.detail()).isEqualTo(crew.getDetail().getValue());
            softly.assertThat(response.imageUrl()).isEqualTo(crew.getImageUrl());
            softly.assertThat(response.kakaoLink()).isEqualTo(crew.getKakaoLink());
            softly.assertThat(response.detail()).isEqualTo(crew.getDetail().getValue());
            softly.assertThat(response.memberCount()).isEqualTo(crew.getCurrentSize());
            softly.assertThat(response.owner().id()).isEqualTo(revi.getId());
            softly.assertThat(response.owner().email()).isNull();
            softly.assertThat(response.owner().nickname()).isEqualTo(revi.getNickname().getValue());
            softly.assertThat(response.owner().introduce()).isEqualTo(revi.getIntroduce().getValue());
            softly.assertThat(response.owner().mbti()).isEqualTo(revi.getMbti().name());
        });
    }

    @Test
    @DisplayName("크루 이름 검색 시, 요청한 페이지 번호와 사이즈에 맞게 페이징된 크루 목록을 반환한다")
    void fetchCrewsByName() {
        Member revi = memberRepository.save(createRevi());
        Crew crew1 = crewRepository.save(createCrew("crew-1", revi));
        Crew crew2 = crewRepository.save(createCrew("crew-2", revi));
        Crew crew3 = crewRepository.save(createCrew("crew-3", revi));
        Member andong = memberRepository.save(createAndong());
        Crew crew4 = crewRepository.save(createCrew("crew-4", andong));
        Crew crew5 = crewRepository.save(createCrew("crew-5", andong));
        Crew crew6 = crewRepository.save(createCrew("crew-6", andong));
        crewHashtagRepository.insertBatch(crew1.getId(), Hashtag.fromHashtagTypes(List.of(HashtagType.ACTIVE, HashtagType.PASSIONATE)));
        crewHashtagRepository.insertBatch(crew2.getId(), Hashtag.fromHashtagTypes(List.of(HashtagType.ESCAPE, HashtagType.MOVIE)));
        crewHashtagRepository.insertBatch(crew3.getId(), Hashtag.fromHashtagTypes(List.of(HashtagType.IMPULSIVE, HashtagType.GAME_LOVER_FEMALE)));
        crewHashtagRepository.insertBatch(crew4.getId(), Hashtag.fromHashtagTypes(List.of(HashtagType.HOME_BODY_MALE, HashtagType.POSITIVE)));
        crewHashtagRepository.insertBatch(crew5.getId(), Hashtag.fromHashtagTypes(List.of(HashtagType.IMPULSIVE, HashtagType.WINE)));
        crewHashtagRepository.insertBatch(crew6.getId(), Hashtag.fromHashtagTypes(List.of(HashtagType.WITTY, HashtagType.WALKING)));
        String crewName = "crew-";
        PageRequest pageRequest = PageRequest.of(1, 2);

        PageResponse<CrewResponse> response = crewQueryService.fetchCrewsByName(crewName, pageRequest);

        assertSoftly(softly -> {
            softly.assertThat(response.size()).isEqualTo(2);
            softly.assertThat(response.page()).isEqualTo(2);
            softly.assertThat(response.totalPages()).isEqualTo(3);
            softly.assertThat(response.totalCount()).isEqualTo(6);
            softly.assertThat(response.resultsSize()).isEqualTo(2);

            List<CrewResponse> results = response.results();
            softly.assertThat(results.get(0).id()).isEqualTo(crew4.getId());
            softly.assertThat(results.get(0).owner().id()).isEqualTo(andong.getId());
            softly.assertThat(results.get(1).id()).isEqualTo(crew3.getId());
            softly.assertThat(results.get(1).owner().id()).isEqualTo(revi.getId());
        });
    }
}
