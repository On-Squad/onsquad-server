package revi1337.onsquad.concurrency.crew;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createMember;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import revi1337.onsquad.crew_member.domain.entity.vo.CrewRole;
import revi1337.onsquad.crew_member.domain.repository.CrewMemberJpaRepository;
import revi1337.onsquad.infrastructure.storage.sqlite.FileRecycleBinRepository;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;
import revi1337.onsquad.notification.application.listener.NotificationEventListener;

@Disabled("동시성 테스트는 스레드 간 격리 문제로 인해 수동 검증 시에만 단독 실행한다. (CI/CD 에서 문제 발생 가능)")
@Sql({"/h2-truncate.sql"})
@Import({ApplicationLayerConfiguration.class})
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class CrewMemberConcurrencyCommandServiceTest {

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

    @Autowired
    private MemberJpaRepository memberRepository;

    @Autowired
    private CrewJpaRepository crewRepository;

    @Autowired
    private CrewMemberJpaRepository crewMemberRepository;

    @Autowired
    private CrewMemberCommandService commandService;

    @Autowired
    private CrewMemberCommandServiceFacade commandServiceFacade;

    @Nested
    class delegateOwner {

        @Test
        @DisplayName("방장 위임 동시 요청 시, Optimistic Lock 과 Retry를 통해 중복 방장 발생을 방지하고 정합성을 유지한다")
        void delegateOwner() {
            // given
            Member owner = memberRepository.save(createMember(1));
            Member nextOwnerCandidate1 = memberRepository.save(createMember(2));
            Member nextOwnerCandidate2 = memberRepository.save(createMember(3));
            Crew crew = createCrew(owner);
            crew.addCrewMember(createManagerCrewMember(crew, nextOwnerCandidate1), createManagerCrewMember(crew, nextOwnerCandidate2));
            Crew savedCrew = crewRepository.save(crew);

            // when
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                waitToStart(startLatch);
                commandServiceFacade.delegateOwner(owner.getId(), crew.getId(), nextOwnerCandidate1.getId());
            }, executor);
            CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                waitToStart(startLatch);
                commandServiceFacade.delegateOwner(owner.getId(), crew.getId(), nextOwnerCandidate2.getId());
            }, executor);
            startLatch.countDown();
            CompletableFuture.allOf(future1, future2).join();
            executor.shutdown();

            // then
            assertSoftly(softly -> {
                Crew finalCrew = crewRepository.findById(savedCrew.getId()).orElseThrow();
                CrewMember delegatedOwner1 = crewMemberRepository.findByCrewIdAndMemberId(crew.getId(), nextOwnerCandidate1.getId()).get();
                CrewMember delegatedOwner2 = crewMemberRepository.findByCrewIdAndMemberId(crew.getId(), nextOwnerCandidate2.getId()).get();

                softly.assertThat(delegatedOwner1.getRole())
                        .as("owner 위임 대상이었던 두명의 role 은 다를 수 밖에 없다.")
                        .isNotSameAs(delegatedOwner2.getRole());
                softly.assertThat(finalCrew.getMember().getId())
                        .as("crew 의 실제 member 와 crewmember 의 member 는 같을 수 밖에 없다.")
                        .isSameAs((delegatedOwner1.getRole() == CrewRole.OWNER ? delegatedOwner1 : delegatedOwner2).getMember().getId());

                System.out.printf("Actual Crew Owner: %d\n", finalCrew.getMember().getId());
                System.out.printf("nextOwnerCandidate1: %d role: %s%n", nextOwnerCandidate1.getId(), delegatedOwner1.getRole());
                System.out.printf("nextOwnerCandidate2: %d role: %s%n", nextOwnerCandidate2.getId(), delegatedOwner2.getRole());
            });
        }
    }

    @Nested
    class leaveCrew {

        @Test
        @DisplayName("크루원 탈퇴 시 동시 요청이 발생해도, Pessimistic Lock을 통해 잔류 인원수 정합성을 보장한다.")
        void leaveCrew() {
            // given
            Member owner = memberRepository.save(createMember(1));
            Member manager = memberRepository.save(createMember(2));
            Member general = memberRepository.save(createMember(3));
            Crew crew = createCrew(owner);
            crew.addCrewMember(createManagerCrewMember(crew, manager), createManagerCrewMember(crew, general));
            Crew savedCrew = crewRepository.save(crew);

            // when
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                waitToStart(startLatch);
                commandService.leaveCrew(manager.getId(), savedCrew.getId());
            }, executor);
            CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                waitToStart(startLatch);
                commandService.leaveCrew(general.getId(), savedCrew.getId());
            }, executor);
            startLatch.countDown();
            CompletableFuture.allOf(future1, future2).join();
            executor.shutdown();

            // then
            assertSoftly(softly -> {
                Crew finalCrew = crewRepository.findById(savedCrew.getId()).get();
                softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager.getId()))
                        .as("manager 는 탈퇴했기 때문에 조회되지 않는다.")
                        .isEmpty();
                softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()))
                        .as("general 는 탈퇴했기 때문에 조회되지 않는다.")
                        .isEmpty();
                softly.assertThat(finalCrew.getCurrentSize())
                        .as("3명에서 2명이 나가 잔류인원은 1명이 된다.")
                        .isEqualTo(1);
            });
        }

        @Test
        @DisplayName("[Write Skew 방지] 앞선 멤버의 탈퇴로 인한 상태 변화를 인지하여, Owner가 낡은 정보로 헛발질하지 않고 정상 해체한다.")
        void leaveCrew2() {
            // given
            Member owner = memberRepository.save(createMember(1));
            Member manager = memberRepository.save(createMember(2));
            Crew crew = createCrew(owner);
            crew.addCrewMember(createManagerCrewMember(crew, manager));
            Crew savedCrew = crewRepository.save(crew);

            // when
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch managerStarted = new CountDownLatch(1);
            CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                waitToStart(startLatch);
                managerStarted.countDown();
                commandService.leaveCrew(manager.getId(), savedCrew.getId());
            }, executor);
            CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                waitToStart(startLatch);
                waitToStart(managerStarted);
                sleep(100);
                commandService.leaveCrew(owner.getId(), savedCrew.getId());
            }, executor);
            startLatch.countDown();
            CompletableFuture.allOf(future1, future2).join();
            executor.shutdown();

            // then
            assertSoftly(softly -> {
                Optional<Crew> crewOpt = crewRepository.findById(savedCrew.getId());
                boolean managerExists = crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager.getId()).isPresent();
                boolean ownerExists = crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), owner.getId()).isPresent();

                softly.assertThat(managerExists)
                        .as("manager 는 가장 먼저 탈퇴했다.")
                        .isFalse();
                softly.assertThat(ownerExists)
                        .as("owner 도 manager 탈퇴 후(잔류 인원1) 탈퇴했기 때문에 탈퇴에 성공했다.")
                        .isFalse();
                softly.assertThat(crewOpt)
                        .as("owner 가 탈퇴했기 때문에, crew 도 삭제되었다.")
                        .isEmpty();
            });
        }

        @Test
        @DisplayName("[Stale Read 방지] Owner가 먼저 선점한 경우, 최신 인원 상태를 고정하여 도메인 정책(위임 필요)에 따른 정당한 거절을 수행한다.")
        void leaveCrew3() {
            // given
            Member owner = memberRepository.save(createMember(1));
            Member manager = memberRepository.save(createMember(2));
            Crew crew = createCrew(owner);
            crew.addCrewMember(createManagerCrewMember(crew, manager));
            Crew savedCrew = crewRepository.save(crew);

            // when
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch ownerStarted = new CountDownLatch(1);
            CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                waitToStart(startLatch);
                ownerStarted.countDown();
                assertThatThrownBy(() -> commandService
                        .leaveCrew(owner.getId(), savedCrew.getId()))
                        .isExactlyInstanceOf(CrewBusinessException.InsufficientAuthority.class);
            }, executor);
            CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                waitToStart(startLatch);
                waitToStart(ownerStarted);
                sleep(100);
                commandService.leaveCrew(manager.getId(), savedCrew.getId());
            }, executor);
            startLatch.countDown();
            CompletableFuture.allOf(future2, future1).join();
            executor.shutdown();

            // then
            assertSoftly(softly -> {
                Optional<Crew> crewOpt = crewRepository.findById(savedCrew.getId());
                boolean ownerExists = crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), owner.getId()).isPresent();
                boolean managerExists = crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager.getId()).isPresent();

                softly.assertThat(ownerExists)
                        .as("owner 는 manager 보다 먼저 탈퇴하려 했기 때문에 탈퇴하지 못했다.")
                        .isTrue();
                softly.assertThat(managerExists)
                        .as("manager 는 크루 잔류인원 수에 상관없이 탈퇴에 성공했다.")
                        .isFalse();
                softly.assertThat(crewOpt)
                        .as("owner 가 탈퇴하지 못했기 떄문에 crew 도 삭제되지 않았다.")
                        .isPresent();
            });
        }
    }

    @Nested
    class kickOutMember {

        @Test
        @DisplayName("크루 추방 시 동시 요청이 발생해도 Atomic Update(+Manually Version Update)를 통해 인원수 정합성을 보장한다.")
        void kickOutMember() {
            // given
            Member owner = memberRepository.save(createMember(1));
            Member manager = memberRepository.save(createMember(2));
            Member general = memberRepository.save(createMember(3));
            Crew crew = createCrew(owner);
            crew.addCrewMember(createManagerCrewMember(crew, manager), createManagerCrewMember(crew, general));
            Crew savedCrew = crewRepository.save(crew);

            // when
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                waitToStart(startLatch);
                commandServiceFacade.kickOutMember(owner.getId(), crew.getId(), manager.getId());
            }, executor);
            CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                waitToStart(startLatch);
                commandServiceFacade.kickOutMember(owner.getId(), crew.getId(), general.getId());
            }, executor);
            startLatch.countDown();
            CompletableFuture.allOf(future1, future2).join();
            executor.shutdown();

            // then
            assertSoftly(softly -> {
                softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager.getId()))
                        .as("manager 는 추방되었기 때문에 조회되지 않는다.")
                        .isEmpty();
                softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()))
                        .as("general 는 추방되었기 때문에 조회되지 않는다.")
                        .isEmpty();
                softly.assertThat(crewRepository.findById(savedCrew.getId()).get().getCurrentSize())
                        .as("Atomic Update 로 인해 정상적으로 Crew 잔류인원 정합성이 맞다. (1명남음)")
                        .isEqualTo(1);
            });
        }

        @Test
        @DisplayName("Atomic Update(+Manually Version Update) 기반 크루원 추방과 Optimistic Lock 기반 크루장 위임이 동시 발생하면, 버전 충돌을 감지 및 재시도하고 정합성이 보장된다.")
        void kickOutMemberWithLeaderDelegate() {
            // given
            Member owner = memberRepository.save(createMember(1));
            Member general = memberRepository.save(createMember(2));
            Member nextOwnerCandidate = memberRepository.save(createMember(3));
            Crew crew = createCrew(owner);
            crew.addCrewMember(createGeneralCrewMember(crew, general), createManagerCrewMember(crew, nextOwnerCandidate));
            Crew savedCrew = crewRepository.save(crew);
            System.out.println(savedCrew.getVersion());

            // when
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CompletableFuture<Void> kickOutFuture = CompletableFuture.runAsync(() -> {
                waitToStart(startLatch);
                commandServiceFacade.kickOutMember(owner.getId(), savedCrew.getId(), general.getId());
            }, executor);
            CompletableFuture<Void> delegateFuture = CompletableFuture.runAsync(() -> {
                waitToStart(startLatch);
                commandServiceFacade.delegateOwner(owner.getId(), savedCrew.getId(), nextOwnerCandidate.getId());
            }, executor);
            startLatch.countDown();
            CompletableFuture.allOf(kickOutFuture, delegateFuture).join();
            executor.shutdown();

            // then
            assertSoftly(softly -> {
                Crew finalCrew = crewRepository.findById(savedCrew.getId()).get();
                softly.assertThat(finalCrew.getCurrentSize())
                        .as("3명(owner, general, nextOwnerCandidate) - 1명(일반 추방) = 2명")
                        .isEqualTo(2);
                softly.assertThat(finalCrew.getMember().getId())
                        .as("새롭게 변경된 owner(nextOwnerCandidate) 의 ID와 일치")
                        .isEqualTo(nextOwnerCandidate.getId());
                softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()))
                        .as("general 은 추방되어 삭제된다.")
                        .isEmpty();
                softly.assertThat(finalCrew.getVersion())
                        .as("초기(0) + 추방 성공(+1) + 위임 재시도 성공(+1) = 최종 버전은 2여야 함 (실패한 위임 1차 시도는 반영되지 않음)")
                        .isGreaterThanOrEqualTo(2);
            });
        }
    }

    private void waitToStart(CountDownLatch start) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private CrewMember createManagerCrewMember(Crew crew, Member member) {
        return CrewMemberFactory.manager(crew, member, LocalDateTime.now());
    }

    private CrewMember createGeneralCrewMember(Crew crew, Member member) {
        return CrewMemberFactory.general(crew, member, LocalDateTime.now());
    }
}
