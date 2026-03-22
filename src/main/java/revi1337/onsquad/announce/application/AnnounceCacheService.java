package revi1337.onsquad.announce.application;

import static revi1337.onsquad.common.constant.CacheConst.CREW_ANNOUNCE;
import static revi1337.onsquad.common.constant.CacheConst.CREW_ANNOUNCES;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import revi1337.onsquad.announce.application.dto.response.AnnounceResponse;
import revi1337.onsquad.announce.domain.entity.Announce;
import revi1337.onsquad.announce.domain.error.AnnounceBusinessException;
import revi1337.onsquad.announce.domain.error.AnnounceErrorCode;
import revi1337.onsquad.announce.domain.model.AnnounceReference;
import revi1337.onsquad.announce.domain.repository.AnnounceQueryDslRepository;
import revi1337.onsquad.crew_member.domain.entity.CrewMember;
import revi1337.onsquad.crew_member.domain.CrewRole;
import revi1337.onsquad.crew_member.domain.error.CrewMemberBusinessException;
import revi1337.onsquad.crew_member.domain.error.CrewMemberErrorCode;
import revi1337.onsquad.crew_member.domain.model.CrewMembers;
import revi1337.onsquad.crew_member.domain.repository.CrewMemberRepository;

/**
 * Service for orchestrating announcement cache operations with cross-domain data enrichment.
 * <p>
 * Beyond simple caching, this service merges announcement data with the author's {@link CrewRole} to minimize RDB round-trips and maximize performance for
 * high-traffic entry points.
 *
 * <h2>Key Caching Patterns</h2>
 * <ul>
 * <li><b>Cache-Aside (Lazy Loading):</b> Utilized in {@code getAnnounce} and {@code getDefaultAnnounces}.
 * Data is enriched with member roles and loaded only after a cache miss in the primary database.</li>
 * <li><b>Active Refresh (Write-Through):</b> Utilized in {@code putAnnounce} and {@code putDefaultAnnounceList}.
 * Forces a database fetch to sync the latest state and roles, ensuring immediate consistency.</li>
 * <li><b>Explicit Eviction:</b> Delegated to {@link AnnounceCacheEvictor} to support pinpoint
 * and broad-range invalidation beyond standard Spring Cache limitations.</li>
 * </ul>
 *
 * @see AnnounceCacheEvictor
 * @see AnnounceCacheEvictorFactory
 */
@Service
public class AnnounceCacheService {

    private static final String CACHE_MANAGER_NAME = "redisCacheManager";
    private static final int DEFAULT_FETCH_SIZE = 4;

    private final CrewMemberRepository crewMemberRepository;
    private final AnnounceQueryDslRepository announceQueryDslRepository;
    private final AnnounceCacheEvictor announceCacheEvictor;

    public AnnounceCacheService(
            CrewMemberRepository crewMemberRepository,
            AnnounceQueryDslRepository announceQueryDslRepository,
            @Qualifier(CACHE_MANAGER_NAME) CacheManager cacheManager,
            AnnounceCacheEvictorFactory cacheEvictorFactory
    ) {
        this.crewMemberRepository = crewMemberRepository;
        this.announceQueryDslRepository = announceQueryDslRepository;
        this.announceCacheEvictor = selectCacheEvictor(cacheManager, cacheEvictorFactory);
    }

    private AnnounceCacheEvictor selectCacheEvictor(CacheManager cacheManager, AnnounceCacheEvictorFactory cacheEvictorFactory) {
        return cacheEvictorFactory.findAvailableEvictor(cacheManager);
    }

    @Cacheable(cacheNames = CREW_ANNOUNCE, key = "'crew:' + #crewId + ':announce:' + #announceId", cacheManager = CACHE_MANAGER_NAME)
    public AnnounceResponse getAnnounce(Long crewId, Long announceId) {
        return getAnnounceResponse(crewId, announceId);
    }

    @Cacheable(cacheNames = CREW_ANNOUNCES, key = "'crew:' + #crewId", cacheManager = CACHE_MANAGER_NAME)
    public List<AnnounceResponse> getDefaultAnnounces(Long crewId) {
        return getAnnounceResponses(crewId);
    }

    @CachePut(cacheNames = CREW_ANNOUNCE, key = "'crew:' + #crewId + ':announce:' + #announceId", cacheManager = CACHE_MANAGER_NAME)
    public AnnounceResponse putAnnounce(Long crewId, Long announceId) {
        return getAnnounceResponse(crewId, announceId);
    }

    @CachePut(cacheNames = CREW_ANNOUNCES, key = "'crew:' + #crewId", cacheManager = CACHE_MANAGER_NAME)
    public List<AnnounceResponse> putDefaultAnnounceList(Long crewId) {
        return getAnnounceResponses(crewId);
    }

    public void evictAnnounce(Long crewId, Long announceId) {
        announceCacheEvictor.evictAnnounce(crewId, announceId);
    }

    public void evictAnnounces(Long crewId) {
        announceCacheEvictor.evictAnnounces(crewId);
    }

    public void evictAnnounces(List<Long> crewIds) {
        announceCacheEvictor.evictAnnounces(crewIds);
    }

    public void evictAnnouncesByReferences(List<AnnounceReference> references) {
        announceCacheEvictor.evictAnnouncesByReferences(references);
    }

    public void evictAnnounceLists(List<Long> crewIds) {
        announceCacheEvictor.evictAnnounceLists(crewIds);
    }

    private AnnounceResponse getAnnounceResponse(Long crewId, Long announceId) {
        Announce announce = announceQueryDslRepository.fetchByIdAndCrewId(announceId, crewId)
                .orElseThrow(() -> new AnnounceBusinessException.NotFound(AnnounceErrorCode.NOT_FOUND));
        CrewMember crewMember = crewMemberRepository.findByCrewIdAndMemberId(crewId, announce.getMember().getId())
                .orElseThrow(() -> new CrewMemberBusinessException.NotParticipant(CrewMemberErrorCode.NOT_PARTICIPANT));

        return AnnounceResponse.from(crewMember.getRole(), announce);
    }

    private List<AnnounceResponse> getAnnounceResponses(Long crewId) {
        List<Announce> announces = announceQueryDslRepository.fetchAllInDefaultByCrewId(crewId, DEFAULT_FETCH_SIZE);
        List<Long> writerIds = announces.stream()
                .map(announce -> announce.getMember().getId())
                .distinct()
                .toList();

        CrewMembers crewMembers = crewMemberRepository.findAllByCrewIdAndMemberIdIn(crewId, writerIds);
        Map<Long, CrewRole> memberRoleMap = crewMembers.splitRolesByMemberId();

        return announces.stream()
                .map(announce -> AnnounceResponse.from(memberRoleMap.get(announce.getMember().getId()), announce))
                .collect(Collectors.toList());
    }
}
