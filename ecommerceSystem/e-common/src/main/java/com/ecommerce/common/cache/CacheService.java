package com.ecommerce.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnClass(RedisOperations.class)
public class CacheService {
    private static final Duration CACHE_THROUGH_TTL = Duration.ofMinutes(5);
    private static final String NULL_SENTINEL = "__CACHE_NULL__";
    private static final String HASH_EMPTY_MARKER = "__HASH_EMPTY__";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || NULL_SENTINEL.equals(json)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, type));
        } catch (Exception e) {
            log.warn("缓存读取失败 key={}", key, e);
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<java.util.List<T>> getList(String key, Class<T> elementType) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || NULL_SENTINEL.equals(json)) {
                return Optional.empty();
            }
            JavaType type = objectMapper.getTypeFactory()
                    .constructCollectionType(java.util.List.class, elementType);
            return Optional.of(objectMapper.readValue(json, type));
        } catch (Exception e) {
            log.warn("缓存读取失败 key={}", key, e);
            return Optional.empty();
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("缓存写入失败 key={}", key, e);
        }
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void expire(String key, Duration ttl) {
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }

    public <T> T getOrLoad(String key, Duration ttl, Class<T> type, Supplier<T> loader) {
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }
        T result = loader.get();
        try {
            if (result != null) {
                set(key, result, ttl);
            } else {
                redisTemplate.opsForValue().set(key, NULL_SENTINEL, CACHE_THROUGH_TTL);
            }
        } catch (Exception e) {
            log.error("Redis写入失败，已降级跳过缓存写入, key={}", key, e);
        }
        return result;
    }

    public <T> java.util.List<T> getOrLoadList(String key, Duration ttl, Class<T> elementType,
                                                Supplier<java.util.List<T>> loader) {
        Optional<java.util.List<T>> cached = getList(key, elementType);
        if (cached.isPresent()) {
            return cached.get();
        }
        java.util.List<T> result = loader.get();
        try {
            if (result != null && !result.isEmpty()) {
                set(key, result, ttl);
            } else {
                redisTemplate.opsForValue().set(key, NULL_SENTINEL, CACHE_THROUGH_TTL);
            }
        } catch (Exception e) {
            log.error("Redis写入失败，已降级跳过缓存写入, key={}", key, e);
        }
        return result;
    }

    // ==================== Hash ====================

    public <T> Optional<T> hGet(String key, String field, Class<T> type) {
        try {
            String json = (String) redisTemplate.opsForHash().get(key, field);
            if (json == null || NULL_SENTINEL.equals(json)) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, type));
        } catch (Exception e) {
            log.warn("Hash读取失败 key={} field={}", key, field, e);
            return Optional.empty();
        }
    }

    public <T> Map<String, T> hGetAll(String key, Class<T> type) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<String, T> result = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                String k = entry.getKey().toString();
                if (HASH_EMPTY_MARKER.equals(k)) continue;
                String val = (String) entry.getValue();
                if (NULL_SENTINEL.equals(val)) continue;
                T v = objectMapper.readValue(val, type);
                result.put(k, v);
            } catch (Exception e) {
                log.warn("Hash反序列化失败 key={} field={}", key, entry.getKey(), e);
            }
        }
        return result;
    }

    public void hSet(String key, String field, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForHash().put(key, field, json);
        } catch (JsonProcessingException e) {
            log.warn("Hash写入失败 key={} field={}", key, field, e);
        }
    }

    public void hSet(String key, String field, Object value, Duration ttl) {
        hSet(key, field, value);
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }

    public void hSetAll(String key, Map<String, ?> map, Duration ttl) {
        Map<String, String> serialized = new HashMap<>();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            try {
                serialized.put(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
            } catch (JsonProcessingException e) {
                log.warn("Hash批量序列化失败 key={} field={}", key, entry.getKey(), e);
            }
        }
        if (!serialized.isEmpty()) {
            redisTemplate.opsForHash().putAll(key, serialized);
            if (ttl != null) {
                redisTemplate.expire(key, ttl);
            }
        }
    }

    public void hDelete(String key, String... fields) {
        redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }

    public void hDel(String key, String field) {
        redisTemplate.opsForHash().delete(key, field);
    }

    public <T> T hGetOrLoad(String key, String field, Duration ttl, Class<T> type,
                            Supplier<T> loader) {
        Optional<T> cached = hGet(key, field, type);
        if (cached.isPresent()) return cached.get();
        T result = loader.get();
        try {
            if (result != null) {
                hSet(key, field, result);
                if (ttl != null) {
                    redisTemplate.expire(key, ttl);
                }
            } else {
                redisTemplate.opsForHash().put(key, field, NULL_SENTINEL);
                redisTemplate.expire(key, CACHE_THROUGH_TTL);
            }
        } catch (Exception e) {
            log.error("Redis写入失败，已降级跳过缓存写入, key={} field={}", key, field, e);
        }
        return result;
    }

    public <T> Map<String, T> hMGet(String key, Set<String> fields, Class<T> type) {
        Map<String, T> result = new LinkedHashMap<>();
        if (fields == null || fields.isEmpty()) return result;
        List<Object> values = redisTemplate.opsForHash().multiGet(key, new ArrayList<>(fields));
        if (values == null) return result;
        int i = 0;
        for (String field : fields) {
            Object val = values.get(i++);
            if (val != null) {
                try {
                    String json = (String) val;
                    if (!NULL_SENTINEL.equals(json)) {
                        result.put(field, objectMapper.readValue(json, type));
                    }
                } catch (Exception e) {
                    log.warn("Hash反序列化失败 key={} field={}", key, field, e);
                }
            }
        }
        return result;
    }

    /**
     * 批量读取 Hash 字段，未命中时通过 loader 回源。
     * loader 必须返回 <b>所有</b> 请求的 key：value 为 null 表示该 key 在数据库不存在，会缓存空值防穿透。
     */
    public <T> Map<String, T> hMGetOrLoad(String key, Set<String> fields, Duration ttl,
                                          Class<T> type,
                                          java.util.function.Function<Set<String>, Map<String, T>> loader) {
        Map<String, T> cached = hMGet(key, fields, type);
        Set<String> missed = new HashSet<>(fields);
        missed.removeAll(cached.keySet());
        if (!missed.isEmpty()) {
            Map<String, T> loaded = loader.apply(missed);
            if (loaded != null) {
                Map<String, String> toCache = new HashMap<>();
                for (Map.Entry<String, T> entry : loaded.entrySet()) {
                    if (entry.getValue() != null) {
                        cached.put(entry.getKey(), entry.getValue());
                        try {
                            toCache.put(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
                        } catch (JsonProcessingException e) {
                            log.warn("Hash序列化失败 key={} field={}", key, entry.getKey(), e);
                        }
                    } else {
                        try {
                            redisTemplate.opsForHash().put(key, entry.getKey(), NULL_SENTINEL);
                        } catch (Exception e) {
                            log.error("Redis写入失败，已降级跳过空值缓存, key={} field={}", key, entry.getKey(), e);
                        }
                    }
                }
                if (!toCache.isEmpty()) {
                    try {
                        redisTemplate.opsForHash().putAll(key, toCache);
                    } catch (Exception e) {
                        log.error("Redis写入失败，已降级跳过缓存写入, key={}", key, e);
                    }
                }
                try {
                    redisTemplate.expire(key, ttl != null ? ttl : CACHE_THROUGH_TTL);
                } catch (Exception e) {
                    log.error("Redis设置过期时间失败, key={}", key, e);
                }
            }
        }
        return cached;
    }

    public <T> Map<String, T> hGetAllOrLoad(String key, Duration ttl, Class<T> type,
                                            Supplier<Map<String, T>> loader) {
        Map<String, T> cached = hGetAll(key, type);
        if (!cached.isEmpty()) return cached;
        if (Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(key, HASH_EMPTY_MARKER))) {
            return Collections.emptyMap();
        }
        Map<String, T> result = loader.get();
        if (result != null && !result.isEmpty()) {
            try {
                hSetAll(key, result, ttl);
            } catch (Exception e) {
                log.error("Redis写入失败，已降级跳过缓存写入, key={}", key, e);
            }
        } else {
            try {
                redisTemplate.opsForHash().put(key, HASH_EMPTY_MARKER, NULL_SENTINEL);
                redisTemplate.expire(key, CACHE_THROUGH_TTL);
            } catch (Exception e) {
                log.error("Redis写入失败，已降级跳过空值缓存, key={}", key, e);
            }
        }
        return result != null ? result : Collections.emptyMap();
    }

    // ==================== ZSet ====================

    public void zAdd(String key, Object value, double score) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForZSet().add(key, json, score);
        } catch (JsonProcessingException e) {
            log.warn("ZSet写入失败 key={}", key, e);
        }
    }

    public <T> List<T> zRange(String key, long start, long end, Class<T> type) {
        Set<String> members = redisTemplate.opsForZSet().range(key, start, end);
        return deserializeSet(members, type);
    }

    public <T> List<T> zRevRange(String key, long start, long end, Class<T> type) {
        Set<String> members = redisTemplate.opsForZSet().reverseRange(key, start, end);
        return deserializeSet(members, type);
    }

    public <T> List<T> zRangeByScore(String key, double min, double max, Class<T> type) {
        Set<String> members = redisTemplate.opsForZSet().rangeByScore(key, min, max);
        return deserializeSet(members, type);
    }

    public void zRemove(String key, Object... members) {
        String[] jsons = new String[members.length];
        try {
            for (int i = 0; i < members.length; i++) {
                jsons[i] = objectMapper.writeValueAsString(members[i]);
            }
        } catch (JsonProcessingException e) {
            log.warn("ZSet删除序列化失败 key={}", key, e);
            return;
        }
        redisTemplate.opsForZSet().remove(key, (Object[]) jsons);
    }

    public Long zCard(String key) {
        Long card = redisTemplate.opsForZSet().zCard(key);
        if (card != null && card == 1) {
            // 可能是空值哨兵，检查一下
            Set<String> members = redisTemplate.opsForZSet().range(key, 0, 0);
            if (members != null && members.size() == 1 && NULL_SENTINEL.equals(members.iterator().next())) {
                return 0L;
            }
        }
        return card;
    }

    public Double zScore(String key, Object member) {
        try {
            String json = objectMapper.writeValueAsString(member);
            return redisTemplate.opsForZSet().score(key, json);
        } catch (JsonProcessingException e) {
            log.warn("ZSet score查询失败 key={}", key, e);
            return null;
        }
    }

    public void zIncrementScore(String key, Object member, double delta) {
        try {
            String json = objectMapper.writeValueAsString(member);
            redisTemplate.opsForZSet().incrementScore(key, json, delta);
        } catch (JsonProcessingException e) {
            log.warn("ZSet增量失败 key={}", key, e);
        }
    }

    public void zExpire(String key, Duration ttl) {
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }

    public <T> List<T> zRevRangeOrLoad(String key, long start, long end, Duration ttl,
                                       Class<T> type, Supplier<List<T>> loader,
                                       java.util.function.Function<T, Double> scoreFn) {
        Long card = zCard(key);
        if (card != null && card > 0) {
            return zRevRange(key, start, end, type);
        }
        List<T> list = loader.get();
        if (list != null && !list.isEmpty()) {
            try {
                for (T item : list) {
                    zAdd(key, item, scoreFn.apply(item));
                }
                zExpire(key, ttl);
            } catch (Exception e) {
                log.error("Redis写入失败，已降级跳过缓存写入, key={}", key, e);
            }
        } else {
            try {
                redisTemplate.opsForZSet().add(key, NULL_SENTINEL, 0);
                zExpire(key, CACHE_THROUGH_TTL);
            } catch (Exception e) {
                log.error("Redis写入失败，已降级跳过空值缓存, key={}", key, e);
            }
        }
        return zRevRange(key, start, end, type);
    }

    // ==================== Set ====================

    public void sAdd(String key, Object... values) {
        String[] jsons = new String[values.length];
        try {
            for (int i = 0; i < values.length; i++) {
                jsons[i] = objectMapper.writeValueAsString(values[i]);
            }
        } catch (JsonProcessingException e) {
            log.warn("Set写入序列化失败 key={}", key, e);
            return;
        }
        redisTemplate.opsForSet().add(key, jsons);
    }

    public <T> Set<T> sMembers(String key, Class<T> type) {
        Set<String> members = redisTemplate.opsForSet().members(key);
        if (members == null) return Collections.emptySet();
        return members.stream()
                .map(json -> deserializeQuietly(json, type))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public boolean sIsMember(String key, Object member) {
        try {
            String json = objectMapper.writeValueAsString(member);
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, json));
        } catch (JsonProcessingException e) {
            log.warn("Set isMember失败 key={}", key, e);
            return false;
        }
    }

    public void sRemove(String key, Object... members) {
        String[] jsons = new String[members.length];
        try {
            for (int i = 0; i < members.length; i++) {
                jsons[i] = objectMapper.writeValueAsString(members[i]);
            }
        } catch (JsonProcessingException e) {
            log.warn("Set删除序列化失败 key={}", key, e);
            return;
        }
        redisTemplate.opsForSet().remove(key, (Object[]) jsons);
    }

    public Long sCard(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        if (size != null && size == 1) {
            Set<String> members = redisTemplate.opsForSet().members(key);
            if (members != null && members.size() == 1 && NULL_SENTINEL.equals(members.iterator().next())) {
                return 0L;
            }
        }
        return size;
    }

    public <T> Set<T> sInter(String key1, String key2, Class<T> type) {
        Set<String> members = redisTemplate.opsForSet().intersect(key1, key2);
        if (members == null) return Collections.emptySet();
        return members.stream()
                .map(json -> deserializeQuietly(json, type))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public <T> Set<T> sUnion(String key1, String key2, Class<T> type) {
        Set<String> members = redisTemplate.opsForSet().union(key1, key2);
        if (members == null) return Collections.emptySet();
        return members.stream()
                .map(json -> deserializeQuietly(json, type))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public void sExpire(String key, Duration ttl) {
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }

    public <T> Set<T> sMembersOrLoad(String key, Duration ttl, Class<T> type,
                                     Supplier<Set<T>> loader) {
        Long card = sCard(key);
        if (card != null && card > 0) {
            return sMembers(key, type);
        }
        Set<T> result = loader.get();
        if (result != null && !result.isEmpty()) {
            try {
                sAdd(key, result.toArray());
                sExpire(key, ttl);
            } catch (Exception e) {
                log.error("Redis写入失败，已降级跳过缓存写入, key={}", key, e);
            }
        } else {
            try {
                redisTemplate.opsForSet().add(key, NULL_SENTINEL);
                sExpire(key, CACHE_THROUGH_TTL);
            } catch (Exception e) {
                log.error("Redis写入失败，已降级跳过空值缓存, key={}", key, e);
            }
        }
        return result != null ? result : Collections.emptySet();
    }

    // ==================== private helpers ====================

    private <T> List<T> deserializeSet(Set<String> members, Class<T> type) {
        if (members == null) return Collections.emptyList();
        return members.stream()
                .map(json -> deserializeQuietly(json, type))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private <T> T deserializeQuietly(String json, Class<T> type) {
        try {
            if (NULL_SENTINEL.equals(json)) return null;
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("反序列化失败 type={}", type.getSimpleName(), e);
            return null;
        }
    }
}
