package com.example.ndbx.service;

import com.example.ndbx.model.Event;
import com.example.ndbx.model.EventReview;
import com.example.ndbx.model.EventReviewKey;
import com.example.ndbx.repository.EventRepository;
import com.example.ndbx.repository.EventReviewRepository;
import com.example.ndbx.util.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class ReviewService {

    private static final String REDIS_REVIEWS_KEY_FORMAT = "event:%s:reviews";
    private static final String REDIS_FLD_COUNT = "count";
    private static final String REDIS_FLD_RATING = "rating";

    private final EventReviewRepository reviewRepository;
    private final EventRepository eventRepository;
    private final StringRedisTemplate redis;
    private final int reviewTtlSeconds;

    public ReviewService(
            EventReviewRepository reviewRepository,
            EventRepository eventRepository,
            StringRedisTemplate redis,
            @Value("${app.event.reviews.ttl}") int reviewTtlSeconds) {
        this.reviewRepository = reviewRepository;
        this.eventRepository = eventRepository;
        this.redis = redis;
        this.reviewTtlSeconds = reviewTtlSeconds;
    }

    public UUID createReview(String eventId, String eventTitle, String userId, String comment, byte rating) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        EventReview review = new EventReview();
        review.setKey(new EventReviewKey(eventId, userId));
        review.setId(id);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        reviewRepository.save(review);
        refreshRedisCache(eventTitle);
        return id;
    }

    public boolean reviewExists(String eventId, String userId) {
        return reviewRepository.findById(new EventReviewKey(eventId, userId)).isPresent();
    }

    public List<Map<String, Object>> getReviews(String eventId, int limit, int offset) {
        List<EventReview> all = reviewRepository.findByEventId(eventId);
        int from = Math.min(offset, all.size());
        int to = Math.min(from + limit, all.size());
        return all.subList(from, to).stream().map(this::reviewToMap).toList();
    }

    public int countReviews(String eventId) {
        return reviewRepository.findByEventId(eventId).size();
    }

    public boolean updateReview(String eventId, String eventTitle, String userId, UUID reviewId,
                                String comment, Byte rating) {
        Optional<EventReview> opt = reviewRepository.findById(new EventReviewKey(eventId, userId));
        if (opt.isEmpty()) return false;

        EventReview review = opt.get();
        if (!review.getId().equals(reviewId)) return false;

        if (comment != null) review.setComment(comment);
        if (rating != null) review.setRating(rating);
        review.setUpdatedAt(Instant.now());
        reviewRepository.save(review);
        refreshRedisCache(eventTitle);
        return true;
    }

    public Map<String, Object> getReviewsSummary(String eventTitle) {
        String cacheKey = reviewsCacheKey(eventTitle);
        String countStr = (String) redis.opsForHash().get(cacheKey, REDIS_FLD_COUNT);
        String ratingStr = (String) redis.opsForHash().get(cacheKey, REDIS_FLD_RATING);
        if (countStr != null && ratingStr != null) {
            return buildSummaryMap(Long.parseLong(countStr), Double.parseDouble(ratingStr));
        }
        return computeAndCacheSummary(eventTitle);
    }

    private Map<String, Object> computeAndCacheSummary(String eventTitle) {
        String cacheKey = reviewsCacheKey(eventTitle);
        List<String> eventIds = eventRepository.findByTitle(eventTitle)
                .stream().map(Event::getId).toList();
        long count = 0;
        long ratingSum = 0;
        for (String eid : eventIds) {
            for (EventReview r : reviewRepository.findByEventId(eid)) {
                count++;
                ratingSum += r.getRating();
            }
        }
        double avgRating = count > 0
                ? BigDecimal.valueOf((double) ratingSum / count).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        writeToRedis(cacheKey, count, avgRating);
        return buildSummaryMap(count, avgRating);
    }

    private void refreshRedisCache(String eventTitle) {
        computeAndCacheSummary(eventTitle);
    }

    private void writeToRedis(String cacheKey, long count, double rating) {
        redis.opsForHash().putAll(cacheKey, Map.of(
                REDIS_FLD_COUNT, String.valueOf(count),
                REDIS_FLD_RATING, String.valueOf(rating)));
        redis.expire(cacheKey, Duration.ofSeconds(reviewTtlSeconds));
    }

    private Map<String, Object> buildSummaryMap(long count, double rating) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(Constants.FLD_COUNT, count);
        map.put(Constants.FLD_RATING, rating);
        return map;
    }

    private String reviewsCacheKey(String eventTitle) {
        return String.format(REDIS_REVIEWS_KEY_FORMAT,
                DigestUtils.md5DigestAsHex(eventTitle.getBytes(StandardCharsets.UTF_8)));
    }

    private Map<String, Object> reviewToMap(EventReview r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(Constants.FLD_ID, r.getId().toString());
        map.put(Constants.PV_EVENT_ID, r.getKey().getEventId());
        map.put(Constants.FLD_COMMENT, r.getComment());
        map.put(Constants.FLD_CREATED_AT, r.getCreatedAt().toString());
        map.put(Constants.FLD_CREATED_BY, r.getKey().getCreatedBy());
        map.put(Constants.FLD_RATING, (int) r.getRating());
        map.put(Constants.FLD_UPDATED_AT, r.getUpdatedAt().toString());
        return map;
    }
}