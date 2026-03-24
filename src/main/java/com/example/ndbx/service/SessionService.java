package com.example.ndbx.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Service
public class SessionService {

    private static final String KEY_PREFIX = "sid:";
    private static final String CREATED_AT = "created_at";
    private static final String UPDATED_AT = "updated_at";
    private static final int SID_BYTE_LENGTH = 16;
    private static final int SID_HEX_LENGTH = SID_BYTE_LENGTH * 2;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final int ttlSeconds;
    private final RedisScript<Long> createSessionScript;

    public SessionService(StringRedisTemplate redis,
                          @Value("${app.session.ttl}") int ttlSeconds) {
        this.redis = redis;
        this.ttlSeconds = ttlSeconds;
        this.createSessionScript = new DefaultRedisScript<>(
            "if redis.call('HSETNX', KEYS[1], '" + CREATED_AT + "', ARGV[1]) == 1 then " +
                        "redis.call('HSET', KEYS[1], '" + UPDATED_AT + "', ARGV[1]); " +
                        "redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
                        "return 1; " +
                        "else return 0; end"
            , Long.class
        );
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }


    public boolean sessionExists(String sid) {
        return redis.hasKey(KEY_PREFIX + sid);
    }

    public String createSession() {
        return createSession(null);
    }

    public String createSession(String userId) {
        String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        for (int attempt = 0; attempt < 5; attempt++) {
            String sid = generateSid();
            String key = KEY_PREFIX + sid;
            Long result = redis.execute(createSessionScript,
                    Collections.singletonList(key),
                    now,
                    String.valueOf(ttlSeconds));
            if (result == 1L) {
                if (userId != null) {
                    redis.opsForHash().put(key, "user_id", userId);
                }
                return sid;
            }
        }
        throw new RuntimeException("Failed to generate unique session id");
    }

    public void bindUserToSession(String sid, String userId) {
        String key = KEY_PREFIX + sid;
        redis.opsForHash().put(key, "user_id", userId);
    }

    public String getUserId(String sid) {
        if (sid == null || sid.isEmpty()) return null;
        Object userId = redis.opsForHash().get(KEY_PREFIX + sid, "user_id");
        return userId != null ? userId.toString() : null;
    }

    public void deleteSession(String sid) {
        if (sid != null && !sid.isEmpty()) {
            redis.delete(KEY_PREFIX + sid);
        }
    }

    public void refreshSession(String sid) {
        String key = KEY_PREFIX + sid;
        String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        redis.opsForHash().put(key, UPDATED_AT, now);
        redis.expire(key, Duration.ofSeconds(ttlSeconds));
    }

    private String generateSid() {
        byte[] bytes = new byte[SID_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(SID_HEX_LENGTH);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
