package com.example.ndbx;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Service
public class SessionService {

    private static final String KEY_PREFIX = "sid:";
    private static final String UPDATED_AT = "updated_at";
    private static final int SID_BYTE_LENGTH = 16;
    private static final int SID_HEX_LENGTH = SID_BYTE_LENGTH * 2;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final long ttlSeconds;

    public SessionService(StringRedisTemplate redis,
                          @Value("${app.session.ttl}") long ttlSeconds) {
        this.redis = redis;
        this.ttlSeconds = ttlSeconds;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public boolean isValidSid(String sid) {
        return sid != null
                && sid.length() == SID_HEX_LENGTH
                && sid.chars().allMatch(c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'));
    }

    public boolean sessionExists(String sid) {
        return redis.hasKey(KEY_PREFIX + sid);
    }

    public String createSession() {
        String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        for (int attempt = 0; attempt < 5; attempt++) {
            String sid = generateSid();
            String key = KEY_PREFIX + sid;
            Boolean created = redis.opsForHash().putIfAbsent(key, "created_at", now);
            if (created) {
                redis.opsForHash().put(key, UPDATED_AT, now);
                redis.expire(key, Duration.ofSeconds(ttlSeconds));
                return sid;
            }
        }
        throw new RuntimeException("Failed to generate unique session id");
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
