package revi1337.onsquad.squad_category.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;
import revi1337.onsquad.common.constant.CacheConst.CacheFormat;
import revi1337.onsquad.common.util.ObjectMapperUtils;
import revi1337.onsquad.infrastructure.storage.redis.RedisCacheEvictor;
import revi1337.onsquad.squad_category.domain.model.SimpleSquadCategory;
import revi1337.onsquad.squad_category.domain.model.SquadCategories;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadCategoryCacheService {

    private static final String SQUAD_CATEGORY_KEY_FORMAT = "squad:%s:categories";
    private static final Duration SQUAD_CATEGORY_TTL = Duration.ofHours(6);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper defaultObjectMapper;
    private final SquadCategoryAccessor squadCategoryAccessor;

    public SquadCategories getCategoriesBySquadIdIn(List<Long> squadIds) {
        List<String> computedKeys = generateCacheKeys(squadIds);
        List<String> serializedValues = stringRedisTemplate.opsForValue().multiGet(computedKeys);

        List<SimpleSquadCategory> totalCategories = new ArrayList<>();
        List<Long> missSquadIds = new ArrayList<>();
        classifyCacheResults(squadIds, missSquadIds, serializedValues, totalCategories);

        if (!missSquadIds.isEmpty()) {
            totalCategories.addAll(processCacheMiss(missSquadIds));
        }
        return new SquadCategories(totalCategories);
    }

    public void evictSquadCategories(List<Long> squadIds) {
        List<String> computedKeys = generateCacheKeys(squadIds);
        RedisCacheEvictor.unlinkKeys(stringRedisTemplate, computedKeys);
    }

    private String generateCacheKey(Long squadId) {
        String key = String.format(SQUAD_CATEGORY_KEY_FORMAT, squadId);
        return String.format(CacheFormat.SIMPLE, key);
    }

    private List<String> generateCacheKeys(List<Long> squadIds) {
        return squadIds.stream()
                .map(this::generateCacheKey)
                .toList();
    }

    private void classifyCacheResults(List<Long> squadIds, List<Long> missSquadIds, List<String> serializedValues, List<SimpleSquadCategory> totalCategories) {
        for (int i = 0; i < squadIds.size(); i++) {
            String json = (serializedValues != null) ? serializedValues.get(i) : null;
            SquadCategories cached = (json != null) ? ObjectMapperUtils.deserialize(defaultObjectMapper, json, SquadCategories.class) : null;
            if (cached != null) {
                totalCategories.addAll(cached.values());
            } else {
                missSquadIds.add(squadIds.get(i));
            }
        }
    }

    private List<SimpleSquadCategory> processCacheMiss(List<Long> missSquadIds) {
        SquadCategories missedCategories = squadCategoryAccessor.fetchCategoriesBySquadIdIn(missSquadIds);
        Map<Long, SquadCategories> splitGroup = missedCategories.splitBySquad();
        stringRedisTemplate.executePipelined((RedisCallback<Void>) connection -> {
            missSquadIds.forEach(missSquadId -> {
                String key = generateCacheKey(missSquadId);
                String value = ObjectMapperUtils.serializeToString(defaultObjectMapper, splitGroup.getOrDefault(missSquadId, new SquadCategories()));
                byte[] serializedKey = stringRedisTemplate.getStringSerializer().serialize(key);
                byte[] serializedValue = stringRedisTemplate.getStringSerializer().serialize(value);

                connection.stringCommands().set(serializedKey, serializedValue, Expiration.from(SQUAD_CATEGORY_TTL), SetOption.upsert());
            });
            return null;
        });

        return missedCategories.values();
    }
}
