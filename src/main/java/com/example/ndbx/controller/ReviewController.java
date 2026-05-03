package com.example.ndbx.controller;

import com.example.ndbx.exception.ValidationException;
import com.example.ndbx.model.Event;
import com.example.ndbx.service.EventService;
import com.example.ndbx.service.ReviewService;
import com.example.ndbx.service.SessionService;
import com.example.ndbx.util.Constants;
import com.example.ndbx.util.CookieHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
public class ReviewController extends BaseController {

    private final ReviewService reviewService;
    private final EventService eventService;

    public ReviewController(ReviewService reviewService, EventService eventService, SessionService sessionService) {
        super(sessionService);
        this.reviewService = reviewService;
        this.eventService = eventService;
    }

    @PostMapping("/events/{" + Constants.PV_EVENT_ID + "}/reviews")
    public ResponseEntity<?> createReview(
            @PathVariable(Constants.PV_EVENT_ID) String eventId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request, HttpServletResponse response) {

        String sid = CookieHelper.extractSid(request);
        if (sid.isEmpty() || !sessionService.sessionExists(sid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = sessionService.getUserId(sid);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Event> eventOpt = eventService.getEventById(eventId);
        if (eventOpt.isEmpty()) {
            refreshSessionIfExists(sid, response);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(Constants.FLD_MESSAGE, "Event not found"));
        }

        String comment = requireStringBody(body, Constants.FLD_COMMENT);
        if (comment.length() > 300) {
            throw new ValidationException(Constants.FLD_COMMENT);
        }

        byte rating = requireRating(body);

        refreshSessionIfExists(sid, response);

        if (reviewService.reviewExists(eventId, userId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(Constants.FLD_MESSAGE, "Already exists"));
        }

        UUID id = reviewService.createReview(eventId, eventOpt.get().getTitle(), userId, comment, rating);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(Constants.FLD_ID, id.toString()));
    }

    @GetMapping("/events/{" + Constants.PV_EVENT_ID + "}/reviews")
    public ResponseEntity<?> getReviews(
            @PathVariable(Constants.PV_EVENT_ID) String eventId,
            @RequestParam(defaultValue = Constants.PARAM_DEFAULT_LIMIT) String limit,
            @RequestParam(defaultValue = Constants.PARAM_DEFAULT_OFFSET) String offset,
            HttpServletRequest request, HttpServletResponse response) {

        String sid = CookieHelper.extractSid(request);
        refreshSessionIfExists(sid, response);

        int limitInt = parseParamInt(limit, Constants.PARAM_LIMIT);
        int offsetInt = parseParamInt(offset, Constants.PARAM_OFFSET);

        if (limitInt == 0) {
            return ResponseEntity.ok(Map.of(Constants.FLD_REVIEWS, List.of(), Constants.FLD_COUNT, 0));
        }

        List<Map<String, Object>> reviews = reviewService.getReviews(eventId, limitInt, offsetInt);
        return ResponseEntity.ok(Map.of(Constants.FLD_REVIEWS, reviews, Constants.FLD_COUNT, reviews.size()));
    }

    @PatchMapping("/events/{" + Constants.PV_EVENT_ID + "}/reviews/{" + Constants.PV_REVIEW_ID + "}")
    public ResponseEntity<?> updateReview(
            @PathVariable(Constants.PV_EVENT_ID) String eventId,
            @PathVariable(Constants.PV_REVIEW_ID) String reviewId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request, HttpServletResponse response) {

        String sid = CookieHelper.extractSid(request);
        if (sid.isEmpty() || !sessionService.sessionExists(sid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = sessionService.getUserId(sid);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Event> eventOpt = eventService.getEventById(eventId);
        if (eventOpt.isEmpty()) {
            refreshSessionIfExists(sid, response);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(Constants.FLD_MESSAGE, "Event not found"));
        }

        String comment = null;
        if (body.containsKey(Constants.FLD_COMMENT)) {
            comment = requireStringBody(body, Constants.FLD_COMMENT);
            if (comment.length() > 300) {
                throw new ValidationException(Constants.FLD_COMMENT);
            }
        }

        Byte rating = null;
        if (body.containsKey(Constants.FLD_RATING)) {
            rating = requireRating(body);
        }

        UUID parsedReviewId;
        try {
            parsedReviewId = UUID.fromString(reviewId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(Constants.FLD_MESSAGE, "Event not found"));
        }

        refreshSessionIfExists(sid, response);

        boolean updated = reviewService.updateReview(eventId, eventOpt.get().getTitle(), userId, parsedReviewId, comment, rating);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(Constants.FLD_MESSAGE, "Event not found"));
        }

        return ResponseEntity.noContent().build();
    }

    private byte requireRating(Map<String, Object> body) {
        Object ratingObj = body.get(Constants.FLD_RATING);
        if (!(ratingObj instanceof Integer)) {
            throw new ValidationException(Constants.FLD_RATING);
        }
        int r = (Integer) ratingObj;
        if (r < 1 || r > 5) {
            throw new ValidationException(Constants.FLD_RATING);
        }
        return (byte) r;
    }
}