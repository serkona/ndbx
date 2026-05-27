package com.example.ndbx.service;

import com.example.ndbx.model.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {

    private static final String REDIS_KEY_FORMAT = "user:%s:recomms";
    private static final String REDIS_FLD_EVENTS = "events";

    private final Neo4jService neo4jService;
    private final EventService eventService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final int ttlSeconds;

    public RecommendationService(
            Neo4jService neo4jService,
            EventService eventService,
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${app.recommendations.ttl}") int ttlSeconds) {
        this.neo4jService = neo4jService;
        this.eventService = eventService;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttlSeconds = ttlSeconds;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecommendations(String userId) {
        String key = String.format(REDIS_KEY_FORMAT, userId);

        Object cached = redis.opsForHash().get(key, REDIS_FLD_EVENTS);
        if (cached != null) {
            try {
                List<?> raw = objectMapper.readValue(cached.toString(), List.class);
                return raw.stream()
                        .map(o -> (Map<String, Object>) o)
                        .toList();
            } catch (JsonProcessingException ignored) {
            }
        }

        List<String> eventIds = neo4jService.getRecommendedEventIds(userId);

        Map<String, Event> byTitle = new LinkedHashMap<>();
        for (String id : eventIds) {
            eventService.getEventById(id).ifPresent(event -> {
                String title = event.getTitle();
                Event existing = byTitle.get(title);
                if (existing == null || isEarlier(event.getStartedAt(), existing.getStartedAt())) {
                    byTitle.put(title, event);
                }
            });
        }

        List<Map<String, Object>> result = byTitle.values().stream()
                .map(eventService::eventToMap)
                .toList();

        try {
            String json = objectMapper.writeValueAsString(result);
            redis.opsForHash().put(key, REDIS_FLD_EVENTS, json);
            redis.expire(key, Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException ignored) {
        }

        return result;
    }

    private boolean isEarlier(String a, String b) {
        if (a == null) return false;
        if (b == null) return true;
        return a.compareTo(b) < 0;
    }
}