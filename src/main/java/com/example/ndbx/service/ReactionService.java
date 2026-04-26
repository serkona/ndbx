package com.example.ndbx.service;

import com.example.ndbx.model.Event;
import com.example.ndbx.model.EventReaction;
import com.example.ndbx.model.EventReactionKey;
import com.example.ndbx.repository.EventReactionRepository;
import com.example.ndbx.repository.EventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReactionService {
    private static final String REDIS_REACTIONS_KEY_FORMAT = "event:%s:reactions";
    private static final String FLD_LIKES = "likes";
    private static final String FLD_DISLIKES = "dislikes";
    private static final byte LIKE_VALUE = 1;
    private static final byte DISLIKE_VALUE = -1;

    private final EventReactionRepository reactionRepository;
    private final EventRepository eventRepository;
    private final StringRedisTemplate redis;
    private final int likeTtlSeconds;

    public ReactionService(
            EventReactionRepository reactionRepository,
            EventRepository eventRepository,
            StringRedisTemplate redis,
            @Value("${app.like.ttl}") int likeTtlSeconds) {
        this.reactionRepository = reactionRepository;
        this.eventRepository = eventRepository;
        this.redis = redis;
        this.likeTtlSeconds = likeTtlSeconds;
    }

    public void setLike(String eventId, String userId) {
        saveReaction(eventId, userId, LIKE_VALUE);
    }

    public void setDislike(String eventId, String userId) {
        saveReaction(eventId, userId, DISLIKE_VALUE);
    }

    public Map<String, Object> getReactions(String eventTitle) {
        String cacheKey = reactionsCacheKey(eventTitle);
        long likes = 0;
        long dislikes = 0;
        for (String eid : eventRepository.findByTitle(eventTitle).stream().map(Event::getId).toList()) {
            for (EventReaction r : reactionRepository.findByEventId(eid)) {
                if (r.getLikeValue() == LIKE_VALUE) {
                    likes++;
                } else if (r.getLikeValue() == DISLIKE_VALUE) {
                    dislikes++;
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(FLD_LIKES, likes);
        result.put(FLD_DISLIKES, dislikes);
        if (likes > 0 || dislikes > 0) {
            writeHashToRedis(cacheKey, likes, dislikes);
        } else {
            redis.delete(cacheKey);
        }
        return result;
    }

    private void saveReaction(String eventId, String userId, byte likeValue) {
        EventReaction reaction = new EventReaction();
        reaction.setKey(new EventReactionKey(eventId, userId));
        reaction.setLikeValue(likeValue);
        reaction.setCreatedAt(Instant.now());
        reactionRepository.save(reaction);
        eventRepository.findById(eventId).ifPresent(event ->
                refreshRedisCacheForTitle(event.getTitle()));
    }

    private void refreshRedisCacheForTitle(String eventTitle) {
        String cacheKey = reactionsCacheKey(eventTitle);
        long likes = 0;
        long dislikes = 0;
        for (String eid : eventRepository.findByTitle(eventTitle).stream().map(Event::getId).toList()) {
            for (EventReaction r : reactionRepository.findByEventId(eid)) {
                if (r.getLikeValue() == LIKE_VALUE) {
                    likes++;
                } else if (r.getLikeValue() == DISLIKE_VALUE) {
                    dislikes++;
                }
            }
        }
        if (likes > 0 || dislikes > 0) {
            writeHashToRedis(cacheKey, likes, dislikes);
        } else {
            redis.delete(cacheKey);
        }
    }

    private void writeHashToRedis(String cacheKey, long likes, long dislikes) {
        redis.opsForHash().putAll(cacheKey, Map.of(
                FLD_LIKES, String.valueOf(likes),
                FLD_DISLIKES, String.valueOf(dislikes)));
        redis.expire(cacheKey, Duration.ofSeconds(likeTtlSeconds));
    }

    private String reactionsCacheKey(String eventTitle) {
        return String.format(REDIS_REACTIONS_KEY_FORMAT,
                DigestUtils.md5DigestAsHex(eventTitle.getBytes(StandardCharsets.UTF_8)));
    }
}
