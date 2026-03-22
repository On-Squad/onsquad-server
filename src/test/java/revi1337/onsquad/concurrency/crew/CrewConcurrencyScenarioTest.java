package revi1337.onsquad.concurrency.crew;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createMember;

import java.time.LocalDateTime;
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
import revi1337.onsquad.crew_member.domain.CrewRole;
import revi1337.onsquad.crew_member.domain.repository.CrewMemberJpaRepository;
import revi1337.onsquad.crew_request.application.CrewRequestCommandService;
import revi1337.onsquad.crew_request.domain.entity.CrewRequest;
import revi1337.onsquad.crew_request.domain.repository.CrewRequestJpaRepository;
import revi1337.onsquad.infrastructure.storage.redis.RedisCacheAspect;
import revi1337.onsquad.infrastructure.storage.sqlite.FileRecycleBinRepository;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;
import revi1337.onsquad.notification.application.listener.NotificationEventListener;

@Disabled("동시성 테스트는 스레드 간 격리 문제로 인해 수동 검증 시에만 단독 실행한다. (CI/CD 에서 문제 발생 가능)")
@Sql({"/h2-truncate.sql"})
@Import({ApplicationLayerConfiguration.class})
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@DisplayName("Crew 도메인 동시성 경합: 명시적 순서 제어를 통한 시나리오 검증")
public class CrewConcurrencyScenarioTest {

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
    private CrewRequestJpaRepository crewRequestRepository;

    @Autowired
    private CrewMemberJpaRepository crewMemberRepository;

    @Autowired
    private CrewRequestCommandService crewRequestCommandService;

    @Autowired
    private CrewMemberCommandService crewMemberCommandService;

    @Autowired
    private CrewMemberCommandServiceFacade crewMemberCommandServiceFacade;

    @Nested
    @DisplayName("acceptRequest(비관적 락) + leaveCrew(비관적 락) 경합 테스트")
    class acceptRequestWithLeaveCrew {

        @Nested
        @DisplayName("Owner 가 탈퇴하지 않을 때")
        class whenNotOwner {

            @Test
            @DisplayName("참여 수락(Accept)이 먼저 락을 잡은 경우 [manger1 가 general 수락(정상) --> manager2 탈퇴(정상)]")
            void test1() {
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
                CountDownLatch acceptLatch = new CountDownLatch(1);
                CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    acceptLatch.countDown();
                    crewRequestCommandService.acceptRequest(manager1.getId(), crew.getId(), request.getId());
                }, executor);
                CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    waitToStart(acceptLatch);
                    crewMemberCommandService.leaveCrew(manager2.getId(), crew.getId());
                }, executor);
                startLatch.countDown();
                CompletableFuture.allOf(future1, future2).join();
                executor.shutdown();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()))
                            .as("general 은 manager1 에 의해 승인되었기 때문에 조회된다.")
                            .isPresent();
                    softly.assertThat(crewRequestRepository.findById(request.getId()))
                            .as("general 이 신청한 요청은 폐기된다.")
                            .isEmpty();
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager2.getId()))
                            .as("manager2 자진탈퇴 했기 때문에 조회되지 않는다.")
                            .isEmpty();
                    softly.assertThat(crewRepository.findById(savedCrew.getId()).get().getCurrentSize())
                            .as("3명 중 1명(general) 추가 + 1명(manager2) 탈퇴되어 3명이 남는다.")
                            .isEqualTo(3);
                });
            }

            @Test
            @DisplayName("자진 탈퇴(Leave)가 먼저 락을 잡은 경우 [manager2 탈퇴(정상) --> manger1 가 general 수락(정상)]")
            void test2() {
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
                CountDownLatch leaveLatch = new CountDownLatch(1);
                CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    leaveLatch.countDown();
                    crewMemberCommandService.leaveCrew(manager2.getId(), crew.getId());
                }, executor);
                CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    waitToStart(leaveLatch);
                    crewRequestCommandService.acceptRequest(manager1.getId(), crew.getId(), request.getId());
                }, executor);
                startLatch.countDown();
                CompletableFuture.allOf(future2, future1).join();
                executor.shutdown();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager2.getId()))
                            .as("manager2 자진탈퇴 했기 때문에 조회되지 않는다.")
                            .isEmpty();
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()))
                            .as("general 은 manager1 에 의해 승인되었기 때문에 조회된다.")
                            .isPresent();
                    softly.assertThat(crewRequestRepository.findById(request.getId()))
                            .as("general 이 신청한 요청은 폐기된다.")
                            .isEmpty();
                    softly.assertThat(crewRepository.findById(savedCrew.getId()).get().getCurrentSize())
                            .as("3명 중 1명(manager2) 탈퇴 + 1명(general) 추가되어 3명이 남는다.")
                            .isEqualTo(3);
                });
            }
        }

        @Nested
        @DisplayName("Owner 가 탈퇴할 때")
        class whenOwner {

            @Test
            @DisplayName("참여 수락(Accept)이 먼저 락을 잡은 경우 [manger1 가 general 수락(정상) --> owner 탈퇴(예외)]")
            void test1() {
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
                CountDownLatch acceptLatch = new CountDownLatch(1);
                CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    acceptLatch.countDown();
                    crewRequestCommandService.acceptRequest(manager1.getId(), crew.getId(), request.getId());
                }, executor);
                CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    waitToStart(acceptLatch);
                    assertThatThrownBy(() -> crewMemberCommandService
                            .leaveCrew(owner.getId(), crew.getId()))
                            .as("아직 manager1,2 가 잔류해 owner 는 탈퇴할 수 없다는 예외가 발생한다.")
                            .isExactlyInstanceOf(CrewBusinessException.InsufficientAuthority.class);
                }, executor);
                startLatch.countDown();
                CompletableFuture.allOf(future1, future2).join();
                executor.shutdown();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()))
                            .as("general 은 manager1 에 의해 승인되었기 때문에 조회된다.")
                            .isPresent();
                    softly.assertThat(crewRequestRepository.findById(request.getId()))
                            .as("general 이 신청한 요청은 폐기된다.")
                            .isEmpty();
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), owner.getId()))
                            .as("owner 탈퇴 당시, manager1,2 가 잔류했기 때문에 탈퇴처리되지 않았다.")
                            .isPresent();
                    softly.assertThat(crewRepository.findById(savedCrew.getId()).get().getCurrentSize())
                            .as("3명 중 1명(general) 추가되어 4명이 남는다.")
                            .isEqualTo(4);
                });
            }

            @Test
            @DisplayName("자진 탈퇴(Leave)가 먼저 락을 잡은 경우 [owner 탈퇴(예외) --> manger1 가 general 수락(정상)]")
            void test2() {
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
                CountDownLatch leaveLatch = new CountDownLatch(1);
                CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    leaveLatch.countDown();
                    assertThatThrownBy(() -> crewMemberCommandService
                            .leaveCrew(owner.getId(), crew.getId()))
                            .as("아직 manager1,2 가 잔류해 owner 는 탈퇴할 수 없다는 예외가 발생한다.")
                            .isExactlyInstanceOf(CrewBusinessException.InsufficientAuthority.class);
                }, executor);
                CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    waitToStart(leaveLatch);
                    crewRequestCommandService.acceptRequest(manager1.getId(), crew.getId(), request.getId());
                }, executor);
                startLatch.countDown();
                CompletableFuture.allOf(future2, future1).join();
                executor.shutdown();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), owner.getId()))
                            .as("owner 탈퇴 당시, manager1,2 가 잔류했기 때문에 탈퇴처리되지 않았다.")
                            .isPresent();
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()))
                            .as("general 은 manager1 에 의해 승인되었기 때문에 조회된다.")
                            .isPresent();
                    softly.assertThat(crewRequestRepository.findById(request.getId()))
                            .as("general 이 신청한 요청은 폐기된다.")
                            .isEmpty();
                    softly.assertThat(crewRepository.findById(savedCrew.getId()).get().getCurrentSize())
                            .as("3명 중 1명(general) 추가되어 4명이 남는다.")
                            .isEqualTo(4);
                });
            }
        }
    }

    @Nested
    @DisplayName("leaveCrew(비관적 락) + kickOutMember(Atomic Update + Manual Versioning) 경합 테스트")
    class leaveCrewWithKickOut {

        @Nested
        @DisplayName("Owner 가 탈퇴하지 않을 때")
        class whenNotOwner {

            @Test
            @DisplayName("강제 추방(Kickout)이 먼저 락을 잡은 경우 [manger 가 general 추방(정상) --> manager 탈퇴(정상)]")
            void test1() {
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
                CountDownLatch kickoutLatch = new CountDownLatch(1);
                CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    kickoutLatch.countDown();
                    crewMemberCommandService.kickOutMember(manager.getId(), crew.getId(), general.getId());
                }, executor);
                CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    waitToStart(kickoutLatch);
                    crewMemberCommandService.leaveCrew(manager.getId(), crew.getId());
                }, executor);
                startLatch.countDown();
                CompletableFuture.allOf(future1, future2).join();
                executor.shutdown();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()))
                            .as("general 은 manager 에 의해 추방되었기 때문에 조회되지 않는다.")
                            .isEmpty();
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager.getId()))
                            .as("manager 는 general 추방 후 자진탈퇴 했기 때문에 조회되지 않는다.")
                            .isEmpty();
                    softly.assertThat(crewRepository.findById(savedCrew.getId()).get().getCurrentSize())
                            .as("3명 중 2명이(general 추방 & manager 탈퇴) 사라져 1명이 남는다.")
                            .isEqualTo(1);
                });
            }

            @Test
            @DisplayName("자진 탈퇴(Leave)가 먼저 락을 잡은 경우 [general 탈퇴(예외) --> owner 가 manager 추방(정상)]")
            void test2() {
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
                CountDownLatch leaveLatch = new CountDownLatch(1);
                CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    leaveLatch.countDown();
                    crewMemberCommandService.leaveCrew(general.getId(), crew.getId());
                }, executor);
                CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    waitToStart(leaveLatch);
                    crewMemberCommandService.kickOutMember(owner.getId(), crew.getId(), manager.getId());
                }, executor);
                startLatch.countDown();
                CompletableFuture.allOf(future2, future1).join();
                executor.shutdown();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()))
                            .as("general 은 자진 탈퇴했기 때문에 조회되지 않는다.")
                            .isEmpty();
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager.getId()))
                            .as("manager 는 owner 에 의해 추방되어 조회되지 않는다.")
                            .isEmpty();
                    softly.assertThat(crewRepository.findById(savedCrew.getId()).get().getCurrentSize())
                            .as("3명 중 2명이(general 탈퇴 & manager 추방) 사라져 1명이 남는다.")
                            .isEqualTo(1);
                });
            }
        }

        @Nested
        @DisplayName("Owner 가 탈퇴할 때")
        class whenOwner {

            @Test
            @DisplayName("강제 추방(Kickout)이 먼저 락을 잡은 경우 [manger 가 general 추방(정상) --> owner 탈퇴(예외)]")
            void test1() {
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
                CountDownLatch kickoutLatch = new CountDownLatch(1);
                CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    kickoutLatch.countDown();
                    crewMemberCommandService.kickOutMember(manager.getId(), crew.getId(), general.getId());
                }, executor);
                CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    waitToStart(kickoutLatch);
                    assertThatThrownBy(() -> crewMemberCommandService
                            .leaveCrew(owner.getId(), crew.getId()))
                            .as("아직 manager 가 잔류해 owner 는 탈퇴할 수 없다는 예외가 발생한다.")
                            .isExactlyInstanceOf(CrewBusinessException.InsufficientAuthority.class);
                }, executor);
                startLatch.countDown();
                CompletableFuture.allOf(future1, future2).join();
                executor.shutdown();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()))
                            .as("general 은 manager 에 의해 추방되었기 때문에 조회되지 않는다.")
                            .isEmpty();
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager.getId()))
                            .as("manager 는 추방 & 탈퇴하지 않았기 때문에 조회된다.")
                            .isPresent();
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), owner.getId()))
                            .as("owner 는 자진 탈퇴하려 했지만, manager 가 잔류하여 탈퇴처리되지 않았다.")
                            .isPresent();
                    softly.assertThat(crewRepository.findById(savedCrew.getId()).get().getCurrentSize())
                            .as("3명 중 1명이(general 추방) 사라져 2명이 남는다.")
                            .isEqualTo(2);
                });
            }

            @Test
            @DisplayName("자진 탈퇴(Leave)가 먼저 락을 잡은 경우 [owner 탈퇴(예외) --> manger 가 general 추방(정상)]")
            void test2() {
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
                CountDownLatch leaveLatch = new CountDownLatch(1);
                CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    leaveLatch.countDown();
                    assertThatThrownBy(() -> crewMemberCommandService
                            .leaveCrew(owner.getId(), crew.getId()))
                            .as("아직 manager & general 이 잔류해 owner 는 탈퇴할 수 없다는 예외가 발생한다.")
                            .isExactlyInstanceOf(CrewBusinessException.InsufficientAuthority.class);
                }, executor);
                CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    waitToStart(leaveLatch);
                    crewMemberCommandService.kickOutMember(manager.getId(), crew.getId(), general.getId());
                }, executor);
                startLatch.countDown();
                CompletableFuture.allOf(future2, future1).join();
                executor.shutdown();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager.getId()))
                            .as("owner 는 manager & general 이 잔류했었기 때문에 탈퇴하지 못했다.")
                            .isPresent();
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()))
                            .as("general 은 manager 에 의해 추방되어 조회되지 않는다.")
                            .isEmpty();
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager.getId()))
                            .as("manager 는 아직 잔류한다.")
                            .isPresent();
                    softly.assertThat(crewRepository.findById(savedCrew.getId()).get().getCurrentSize())
                            .as("3명 중 1명이(general 추방) 사라져 2명이 남는다.")
                            .isEqualTo(2);
                });
            }
        }
    }

    @Nested
    @DisplayName("delegateOwner(낙관적 락) + leaveCrew(비관적 락) 경합 테스트")
    class delegateOwnerWithLeaveCrew {

        @Nested
        @DisplayName("Owner 가 탈퇴하지 않을 때")
        class whenNotOwner {

            @Test
            @DisplayName("owner 위임(delegateOwner)이 먼저 락을 잡은 경우 [(owner 위임 to general(정상))  --> manager 탈퇴(정상)]")
            void test1() {
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
                CountDownLatch delegateLatch = new CountDownLatch(1);
                CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    delegateLatch.countDown();
                    crewMemberCommandServiceFacade.delegateOwner(owner.getId(), crew.getId(), general.getId());
                }, executor);
                CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    waitToStart(delegateLatch);
                    crewMemberCommandService.leaveCrew(manager.getId(), crew.getId());
                }, executor);
                startLatch.countDown();
                CompletableFuture.allOf(future1, future2).join();
                executor.shutdown();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), owner.getId()).get().getRole())
                            .as("기존 owner 는 general 로 강등되었다.")
                            .isSameAs(CrewRole.GENERAL);
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()).get().getRole())
                            .as("general 은 새롭게 owner 로 임명되었다.")
                            .isSameAs(CrewRole.OWNER);
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager.getId()))
                            .as("manager 는 자진탈퇴 했기 때문에 조회되지 않는다.")
                            .isEmpty();
                    softly.assertThat(crewRepository.findById(savedCrew.getId()).get().getCurrentSize())
                            .as("3명 중 1명이(manager 탈퇴) 사라져 2명이 남는다.")
                            .isEqualTo(2);
                });
            }

            @Test
            @DisplayName("자진 탈퇴(Leave)가 먼저 락을 잡은 경우 [manager 탈퇴(정상) --> (owner 위임 to general)")
            void test2() {
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
                CountDownLatch leaveLatch = new CountDownLatch(1);
                CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    leaveLatch.countDown();
                    crewMemberCommandService.leaveCrew(manager.getId(), crew.getId());
                }, executor);
                CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    waitToStart(leaveLatch);
                    crewMemberCommandServiceFacade.delegateOwner(owner.getId(), crew.getId(), general.getId());
                }, executor);
                startLatch.countDown();
                CompletableFuture.allOf(future2, future1).join();
                executor.shutdown();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), manager.getId()))
                            .as("manager 는 자진탈퇴 했기 때문에 조회되지 않는다.")
                            .isEmpty();
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), owner.getId()).get().getRole())
                            .as("기존 owner 는 general 로 강등되었다.")
                            .isSameAs(CrewRole.GENERAL);
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()).get().getRole())
                            .as("general 은 새롭게 owner 로 임명되었다.")
                            .isSameAs(CrewRole.OWNER);
                    softly.assertThat(crewRepository.findById(savedCrew.getId()).get().getCurrentSize())
                            .as("3명 중 1명이(manager 탈퇴) 사라져 2명이 남는다.")
                            .isEqualTo(2);
                });
            }
        }

        @Nested
        @DisplayName("Owner 가 탈퇴할 때")
        class whenOwner {

            @Test
            @DisplayName("owner 위임(delegateOwner)이 먼저 락을 잡은 경우 [(owner 위임 to general(정상))  --> 기존 owner 탈퇴(예외)]")
            void test1() {
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
                CountDownLatch delegateLatch = new CountDownLatch(1);
                CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    delegateLatch.countDown();
                    crewMemberCommandServiceFacade.delegateOwner(owner.getId(), savedCrew.getId(), general.getId());
                }, executor);
                CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    waitToStart(delegateLatch);
                    assertThatThrownBy(() -> crewMemberCommandService.leaveCrew(owner.getId(), savedCrew.getId()))
                            .as("위임이 완료되어 더 이상 방장이 아니므로 탈퇴(해체) 권한이 없다.")
                            .isExactlyInstanceOf(CrewBusinessException.InsufficientAuthority.class);
                }, executor);
                startLatch.countDown();
                CompletableFuture.allOf(future1, future2).join();
                executor.shutdown();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()).get().getRole())
                            .as("general 은 새롭게 owner 로 임명되었다")
                            .isSameAs(CrewRole.OWNER);
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), owner.getId()).get().getRole())
                            .as("기존 owner 는 general 로 강등되었다.")
                            .isSameAs(CrewRole.GENERAL);
                    softly.assertThat(crewRepository.findById(savedCrew.getId()).get().getCurrentSize())
                            .as("기존 owner 는 강등된 상태로 탈퇴를 시도했지만 실패하여 인원수는 그대로 3명이다.")
                            .isEqualTo(3);
                });
            }

            @Test
            @DisplayName("자진 탈퇴(Leave)가 먼저 락을 잡은 경우 [owner 탈퇴(예외) --> (owner 위임 to general)]")
            void test2() {
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
                CountDownLatch leaveLatch = new CountDownLatch(1);
                CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    leaveLatch.countDown();
                    assertThatThrownBy(() -> crewMemberCommandService.leaveCrew(owner.getId(), savedCrew.getId()))
                            .as("잔류 인원(manager, general)이 있어 방장은 자진 탈퇴할 수 없다.")
                            .isExactlyInstanceOf(CrewBusinessException.InsufficientAuthority.class);
                }, executor);
                CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    waitToStart(leaveLatch);
                    crewMemberCommandServiceFacade.delegateOwner(owner.getId(), savedCrew.getId(), general.getId());
                }, executor);
                startLatch.countDown();
                CompletableFuture.allOf(future1, future2).join();
                executor.shutdown();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(crewRepository.findById(savedCrew.getId()).get().getCurrentSize())
                            .as("owner 는 탈퇴를 시도했지만 실패하여 인원수는 그대로 3명이다.")
                            .isEqualTo(3);
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), general.getId()).get().getRole())
                            .as("general 은 새롭게 owner 로 임명되었다")
                            .isSameAs(CrewRole.OWNER);
                    softly.assertThat(crewMemberRepository.findByCrewIdAndMemberId(savedCrew.getId(), owner.getId()).get().getRole())
                            .as("기존 owner 는 general 로 강등되었다.")
                            .isSameAs(CrewRole.GENERAL);
                });
            }
        }
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
