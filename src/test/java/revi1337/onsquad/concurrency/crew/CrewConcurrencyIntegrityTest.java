package revi1337.onsquad.concurrency.crew;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createMember;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import revi1337.onsquad.common.aspect.RedisCacheAspect;
import revi1337.onsquad.common.aspect.ThrottlingAspect;
import revi1337.onsquad.common.config.ApplicationLayerConfiguration;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.error.CrewBusinessException;
import revi1337.onsquad.crew.domain.repository.CrewJpaRepository;
import revi1337.onsquad.crew_member.application.CrewMemberCommandService;
import revi1337.onsquad.crew_member.application.CrewMemberCommandServiceFacade;
import revi1337.onsquad.crew_member.application.leaderboard.CrewLeaderboardService;
import revi1337.onsquad.crew_member.domain.entity.CrewMember;
import revi1337.onsquad.crew_member.domain.entity.CrewMemberFactory;
import revi1337.onsquad.crew_member.domain.repository.CrewMemberJpaRepository;
import revi1337.onsquad.crew_request.application.CrewRequestCommandService;
import revi1337.onsquad.crew_request.domain.entity.CrewRequest;
import revi1337.onsquad.crew_request.domain.repository.CrewRequestJpaRepository;
import revi1337.onsquad.infrastructure.storage.sqlite.FileRecycleBinRepository;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;
import revi1337.onsquad.notification.application.listener.NotificationEventListener;

@Disabled("동시성 테스트는 스레드 간 격리 문제로 인해 수동 검증 시에만 단독 실행한다. (CI/CD 에서 문제 발생 가능)")
@Sql({"/h2-truncate.sql"})
@Import({ApplicationLayerConfiguration.class})
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@DisplayName("Crew 도메인 동시성 경합: 무작위 실행 및 반복 테스트를 통한 데이터 정합성 검증")
class CrewConcurrencyIntegrityTest {

    @MockBean
    private NotificationEventListener notificationEventListener;

    @MockBean
    private CrewLeaderboardService crewLeaderboardService;

    @MockBean
    private FileRecycleBinRepository fileRecycleBinRepository;

    @MockBean
    private ThrottlingAspect throttlingAspect;

    @MockBean
    private RedisCacheAspect redisCacheAspect;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MemberJpaRepository memberRepository;

    @Autowired
    private CrewJpaRepository crewRepository;

    @Autowired
    private CrewRequestJpaRepository crewRequestRepository;

    @Autowired
    private CrewMemberJpaRepository crewMemberRepository;

    @Autowired
    private CrewRequestCommandService crewRequestCommandService;

    @Autowired
    private CrewMemberCommandService crewMemberCommandService;

    @Autowired
    private CrewMemberCommandServiceFacade crewMemberCommandServiceFacade;

    @RepeatedTest(20)
    @DisplayName("""
            [비결정적] Accept와 Leave가 진짜 동시에 실행되어도 정합성 보장 [Accept(manager1 -> general) vs Leave(manager2)]
            --> 두 작업 모두 "비관적 락"으로 줄을 서므로, 순서에 상관없이 최종 인원수 3명이 유지되어야 함
            """)
    void concurrencyAcceptWithLeave() {
        // given
        Member owner = memberRepository.save(createMember(1));
        Member manager1 = memberRepository.save(createMember(2));
        Member manager2 = memberRepository.save(createMember(3));
        Member general = memberRepository.save(createMember(4));
        Crew crew = createCrew(owner);
        crew.addCrewMember(createManagerCrewMember(crew, manager1), createManagerCrewMember(crew, manager2));
        Crew savedCrew = crewRepository.save(crew);
        CrewRequest request = crewRequestRepository.save(createCrewRequest(savedCrew, general));

        // when
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean acceptSuccess = new AtomicBoolean(false);
        AtomicBoolean leaveSuccess = new AtomicBoolean(false);
        CompletableFuture<Void> acceptFuture = CompletableFuture.runAsync(() -> {
            waitToStart(startLatch);
            crewRequestCommandService.acceptRequest(manager1.getId(), crew.getId(), request.getId());
            acceptSuccess.set(true);
        }, executor);
        CompletableFuture<Void> leaveFuture = CompletableFuture.runAsync(() -> {
            waitToStart(startLatch);
            crewMemberCommandService.leaveCrew(manager2.getId(), crew.getId());
            leaveSuccess.set(true);
        }, executor);
        startLatch.countDown();
        CompletableFuture.allOf(acceptFuture, leaveFuture).join();
        executor.shutdown();

        // then
        assertSoftly(softly -> {
            softly.assertThat(acceptSuccess.get() && leaveSuccess.get())
                    .as("(manager1가 general 요청 승인) 과 (manager2 탈퇴) 모두 성공해야 한다. (아니면 모순)")
                    .isTrue();

            softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()))
                    .as("general은 수락되었다.")
                    .isPresent();
            softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager2.getId()))
                    .as("manager2는 탈퇴되었다.")
                    .isEmpty();

            Crew finalCrew = crewRepository.findById(savedCrew.getId()).get();
            softly.assertThat(finalCrew.getCurrentSize())
                    .as("3명 - 1명(manager2 탈퇴) + 1명(general 수락) = 3명")
                    .isEqualTo(3);
            softly.assertThat(finalCrew.getCurrentSize())
                    .as("currentSize와 실제 멤버 수는 일치한다.")
                    .isEqualTo(entityManager.createQuery("select count(cm.id) from CrewMember cm where cm.crew.id = :crewId", Long.class)
                            .setParameter("crewId", savedCrew.getId())
                            .getSingleResult());
        });
    }

    @RepeatedTest(20)
    @DisplayName("""
            [비결정적] Leave와 KickOut이 같은 멤버를 대상으로 경합 [Leave(manager) vs Kick(owner -> manager)]
            --> 누가 먼저 락을 잡든 결과는 "단 한 명의 삭제"로 수렴해야 함 (인원수 정합성 보장)
            """)
    void concurrencyLeaveWithKickOut() {
        // given
        Member owner = memberRepository.save(createMember(1));
        Member manager = memberRepository.save(createMember(2));
        Member general = memberRepository.save(createMember(3));
        Crew crew = createCrew(owner);
        crew.addCrewMember(createManagerCrewMember(crew, manager), createGeneralCrewMember(crew, general));
        Crew savedCrew = crewRepository.save(crew);

        // when
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean leaveSuccess = new AtomicBoolean(false);
        AtomicBoolean kickoutSuccess = new AtomicBoolean(false);
        AtomicReference<Exception> leaveException = new AtomicReference<>();
        AtomicReference<Exception> kickoutException = new AtomicReference<>();
        CompletableFuture<Void> leaveFuture = CompletableFuture.runAsync(() -> {
            waitToStart(startLatch);
            try {
                crewMemberCommandService.leaveCrew(manager.getId(), crew.getId());
                leaveSuccess.set(true);
            } catch (Exception e) {
                leaveException.set(e);
            }
        }, executor);
        CompletableFuture<Void> kickoutFuture = CompletableFuture.runAsync(() -> {
            waitToStart(startLatch);
            try {
                crewMemberCommandService.kickOutMember(owner.getId(), crew.getId(), manager.getId());
                kickoutSuccess.set(true);
            } catch (Exception e) {
                kickoutException.set(e);
            }
        }, executor);

        startLatch.countDown();
        CompletableFuture.allOf(leaveFuture, kickoutFuture).join();
        executor.shutdown();

        // then
        assertSoftly(softly -> {
            softly.assertThat(leaveSuccess.get() || kickoutSuccess.get())
                    .as("(manager 탈퇴) 와 (owner가 manager 추방) 중 하나는 성공해야 한다.")
                    .isTrue();
            softly.assertThat(leaveSuccess.get() && kickoutSuccess.get())
                    .as("(manager 탈퇴) 와 (owner가 manager 추방) 둘다 성공하면 논리적 모순.")
                    .isFalse();
            softly.assertThat(!leaveSuccess.get() && !kickoutSuccess.get())
                    .as("(manager 탈퇴) 와 (owner가 manager 추방) 둘다 실패해도 논리적 모순.")
                    .isFalse();
            softly.assertThat(leaveSuccess.get() ? kickoutException.get() : leaveException.get())
                    .as("실패한 쪽은 반드시 예외가 발생했어야 한다.")
                    .isNotNull();

            softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager.getId()))
                    .as("어쨋거나 manager는 탈퇴 또는 추방됨 ㅇㅇ")
                    .isEmpty();

            Crew finalCrew = crewRepository.findById(savedCrew.getId()).get();
            softly.assertThat(finalCrew.getCurrentSize())
                    .as("3명 - 1명(manager) = 2명")
                    .isEqualTo(2);
            softly.assertThat(finalCrew.getCurrentSize())
                    .as("currentSize와 실제 멤버 수는 일치한다.")
                    .isEqualTo(entityManager.createQuery("select count(cm.id) from CrewMember cm where cm.crew.id = :crewId", Long.class)
                            .setParameter("crewId", savedCrew.getId())
                            .getSingleResult());
        });
    }

    @RepeatedTest(20)
    @DisplayName("""
            [비결정적] Delegate와 Owner Leave가 동시 발생 [Delegate(owner -> general) vs Leave(기존 owner)]
            --> 이 경합의 승자는 무조건 Delegate(owner -> general)이어야 함 (비관적 락이 줄을 세워주므로)
            """)
    void concurrencyDelegateWithOwnerLeave() {
        // given
        Member owner = memberRepository.save(createMember(1));
        Member manager = memberRepository.save(createMember(2));
        Crew crew = createCrew(owner);
        crew.addCrewMember(createManagerCrewMember(crew, manager));
        Crew savedCrew = crewRepository.save(crew);

        // when
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean delegateSuccess = new AtomicBoolean(false);
        AtomicBoolean leaveSuccess = new AtomicBoolean(false);
        AtomicReference<Exception> delegateException = new AtomicReference<>();
        AtomicReference<Exception> leaveException = new AtomicReference<>();
        CompletableFuture<Void> delegateFuture = CompletableFuture.runAsync(() -> {
            waitToStart(startLatch);
            try {
                crewMemberCommandServiceFacade.delegateOwner(owner.getId(), crew.getId(), manager.getId());
                delegateSuccess.set(true);
            } catch (Exception e) {
                delegateException.set(e);
            }
        }, executor);
        CompletableFuture<Void> leaveFuture = CompletableFuture.runAsync(() -> {
            waitToStart(startLatch);
            try {
                crewMemberCommandService.leaveCrew(owner.getId(), crew.getId());
                leaveSuccess.set(true);
            } catch (Exception e) {
                leaveException.set(e);
            }
        }, executor);
        startLatch.countDown();
        CompletableFuture.allOf(delegateFuture, leaveFuture).join();
        executor.shutdown();

        // then
        assertSoftly(softly -> {
            softly.assertThat(delegateSuccess.get()).as("위임은 반드시 성공해야 함").isTrue();
            softly.assertThat(leaveSuccess.get()).as("인원이 남았으므로 탈퇴는 성공할 수 없음").isFalse();
            softly.assertThat(leaveException.get())
                    .as("탈퇴 실패 원인은 권한 부족(위임 필요)이어야 함")
                    .isExactlyInstanceOf(CrewBusinessException.InsufficientAuthority.class);
        });
    }

    private void waitToStart(CountDownLatch start) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private CrewRequest createCrewRequest(Crew crew, Member andong) {
        return CrewRequest.of(crew, andong, LocalDateTime.now());
    }

    private CrewMember createManagerCrewMember(Crew crew, Member member) {
        return CrewMemberFactory.manager(crew, member, LocalDateTime.now());
    }

    private CrewMember createGeneralCrewMember(Crew crew, Member member) {
        return CrewMemberFactory.general(crew, member, LocalDateTime.now());
    }
}
