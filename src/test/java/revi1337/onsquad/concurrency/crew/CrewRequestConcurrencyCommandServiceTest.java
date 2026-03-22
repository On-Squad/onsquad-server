package revi1337.onsquad.concurrency.crew;

import static org.assertj.core.api.Assertions.assertThat;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createAndong;
import static revi1337.onsquad.common.fixture.MemberFixture.createKwangwon;
import static revi1337.onsquad.common.fixture.MemberFixture.createMember;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.StopWatch;
import revi1337.onsquad.common.aspect.ThrottlingAspect;
import revi1337.onsquad.common.config.ApplicationLayerConfiguration;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.repository.CrewJpaRepository;
import revi1337.onsquad.crew_member.application.leaderboard.CrewLeaderboardService;
import revi1337.onsquad.crew_member.domain.entity.CrewMember;
import revi1337.onsquad.crew_member.domain.entity.CrewMemberFactory;
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
class CrewRequestConcurrencyCommandServiceTest {

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
    private CrewRequestCommandService commandService;

    @Test
    @DisplayName("Pessimistic Lock: 크루 owner 와 manager 가 동시에 서로 다른 참여자의 요청을 수락해도 crew 의 정합성은 보장된다.")
    void accept() {
        // given
        Member revi = memberRepository.save(createRevi());
        Member andong = memberRepository.save(createAndong());
        Member kwangwon = memberRepository.save(createKwangwon());
        Member dummy = memberRepository.save(createMember(1));
        Crew crew = createCrew(revi);
        crew.addCrewMember(createManagerCrewMember(crew, andong));
        Crew savedCrew = crewRepository.save(crew);
        CrewRequest request1 = crewRequestRepository.save(createCrewRequest(savedCrew, kwangwon));
        CrewRequest request2 = crewRequestRepository.save(createCrewRequest(savedCrew, dummy));

        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            waitToStart(startLatch);
            commandService.acceptRequest(revi.getId(), crew.getId(), request1.getId());
        }, executor);
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            waitToStart(startLatch);
            commandService.acceptRequest(andong.getId(), crew.getId(), request2.getId());
        }, executor);
        stopWatch(TimeUnit.MILLISECONDS, () -> {
            startLatch.countDown();
            CompletableFuture.allOf(future1, future2).join();
        });
        executor.shutdown();

        // then
        assertThat(crewRepository.findById(crew.getId()).get().getCurrentSize()).isEqualTo(4);
    }

    @Test
    @DisplayName("Pessimistic Lock: 다수의 운영진이 수십 건의 가입 요청을 동시에 수락해도, 비관적 락을 통해 크루 인원수의 정합성이 완벽히 보장된다.")
    void accept2() {
        // given
        Member acceptor1 = memberRepository.save(createMember(1));
        Member acceptor2 = memberRepository.save(createMember(2));
        Member acceptor3 = memberRepository.save(createMember(3));
        Member acceptor4 = memberRepository.save(createMember(4));
        Member acceptor5 = memberRepository.save(createMember(5));
        Member acceptor6 = memberRepository.save(createMember(6));
        Crew crew = createCrew(acceptor1);
        crew.addCrewMember(
                createManagerCrewMember(crew, acceptor2),
                createManagerCrewMember(crew, acceptor3),
                createManagerCrewMember(crew, acceptor4),
                createManagerCrewMember(crew, acceptor5),
                createManagerCrewMember(crew, acceptor6)
        );
        Crew savedCrew = crewRepository.save(crew);

        List<Long> requestIds = IntStream.rangeClosed(7, 66)
                .mapToObj(i -> memberRepository.save(createMember(i)))
                .map(member -> crewRequestRepository.save(createCrewRequest(savedCrew, member)))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        lst -> lst.stream().map(CrewRequest::getId).toList()
                ));

        int chunkSize = 10;
        List<Member> acceptors = List.of(acceptor1, acceptor2, acceptor3, acceptor4, acceptor5, acceptor6);
        List<List<Long>> chunks = IntStream.range(0, (requestIds.size() + chunkSize - 1) / chunkSize)
                .mapToObj(i -> requestIds.subList(i * chunkSize, Math.min((i + 1) * chunkSize, requestIds.size())))
                .toList();

        // when
        ExecutorService executor = Executors.newFixedThreadPool(acceptors.size() * chunkSize);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<CompletableFuture<Void>> futures = new ArrayList<>(acceptors.size());
        for (int i = 0; i < acceptors.size(); i++) {
            Long acceptorId = acceptors.get(i).getId();
            chunks.get(i).forEach(requestId -> {
                futures.add(CompletableFuture.runAsync(() -> {
                    waitToStart(startLatch);
                    commandService.acceptRequest(acceptorId, crew.getId(), requestId);
                }, executor));
            });
        }
        stopWatch(TimeUnit.MILLISECONDS, () -> {
            startLatch.countDown();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        });
        executor.shutdown();

        // then
        Crew finalCrew = crewRepository.findById(savedCrew.getId()).orElseThrow();
        assertThat(finalCrew.getCurrentSize()).isEqualTo(66);
        assertThat(crewRequestRepository.count()).isZero();
    }

    private void waitToStart(CountDownLatch start) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private CrewMember createManagerCrewMember(Crew crew, Member member) {
        return CrewMemberFactory.manager(crew, member, LocalDateTime.now());
    }

    private CrewRequest createCrewRequest(Crew crew, Member andong) {
        return CrewRequest.of(crew, andong, LocalDateTime.now());
    }

    public void stopWatch(TimeUnit timeUnit, Runnable task) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            task.run();
        } finally {
            stopWatch.stop();
            double totalTime = stopWatch.getTotalTime(timeUnit);
            System.out.printf("Total Time: %d%s%n", (long) totalTime, getAbbreviation(timeUnit));
        }
    }

    private String getAbbreviation(TimeUnit unit) {
        return switch (unit) {
            case NANOSECONDS -> "ns";
            case MICROSECONDS -> "µs";
            case MILLISECONDS -> "ms";
            case SECONDS -> "s";
            case MINUTES -> "m";
            default -> unit.name().toLowerCase();
        };
    }
}
