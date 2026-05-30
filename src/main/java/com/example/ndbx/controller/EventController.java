package com.example.ndbx.controller;

import com.example.ndbx.exception.ValidationException;
import com.example.ndbx.model.Event;
import com.example.ndbx.service.EventService;
import com.example.ndbx.service.ReactionService;
import com.example.ndbx.service.ReviewService;
import com.example.ndbx.service.SessionService;
import com.example.ndbx.util.Constants;
import com.example.ndbx.util.CookieHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
public class EventController extends BaseController {

    private final EventService eventService;
    private final ReactionService reactionService;
    private final ReviewService reviewService;

    public EventController(EventService eventService, ReactionService reactionService,
                           ReviewService reviewService, SessionService sessionService) {
        super(sessionService);
        this.eventService = eventService;
        this.reactionService = reactionService;
        this.reviewService = reviewService;
    }

    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@RequestBody Map<String, Object> body, HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        if (sid.isEmpty() || !sessionService.sessionExists(sid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = sessionService.getUserId(sid);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        refreshSessionIfExists(sid, response);

        String title = requireStringBody(body, Constants.FLD_TITLE);
        String address = requireStringBody(body, Constants.FLD_ADDRESS);
        String startedAt = requireDateBody(body, Constants.FLD_STARTED_AT);
        String finishedAt = requireDateBody(body, Constants.FLD_FINISHED_AT);
        String description = (String) body.get(Constants.FLD_DESCRIPTION);

        if (eventService.existsByTitle(title)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(Constants.FLD_MESSAGE, "event already exists"));
        }

        Event event = eventService.createEvent(title, description, address, startedAt, finishedAt, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(Constants.FLD_ID, event.getId()));
    }

    @PatchMapping("/events/{id}")
    public ResponseEntity<?> patchEvent(@PathVariable String id, @RequestBody Map<String, Object> body,
                                        HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        if (sid.isEmpty() || !sessionService.sessionExists(sid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = sessionService.getUserId(sid);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        refreshSessionIfExists(sid, response);

        if (body.containsKey(Constants.FLD_CATEGORY)) {
            Object catObj = body.get(Constants.FLD_CATEGORY);
            if (!(catObj instanceof String) || !eventService.isValidCategory((String) catObj)) {
                throw new ValidationException(Constants.FLD_CATEGORY);
            }
        }

        if (body.containsKey(Constants.FLD_PRICE)) {
            Object priceObj = body.get(Constants.FLD_PRICE);
            if (!(priceObj instanceof Number)) {
                throw new ValidationException(Constants.FLD_PRICE);
            }
            double d = ((Number) priceObj).doubleValue();
            if (d < 0 || d != Math.floor(d)) {
                throw new ValidationException(Constants.FLD_PRICE);
            }
        }

        if (body.containsKey(Constants.FLD_CITY)) {
            Object cityObj = body.get(Constants.FLD_CITY);
            if (cityObj != null && !(cityObj instanceof String)) {
                throw new ValidationException(Constants.FLD_CITY);
            }
        }

        boolean updated = eventService.updateEvent(id, userId, body);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(Constants.FLD_MESSAGE,  "Not found. Be sure that event exists and you are the organizer"));
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<?> getEventById(@PathVariable String id,
                                          @RequestParam(required = false) String include,
                                          HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        refreshSessionIfExists(sid, response);

        Optional<Event> eventOpt = eventService.getEventById(id);
        if (eventOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(Constants.FLD_MESSAGE, Constants.MSG_NOT_FOUND));
        }

        Event event = eventOpt.get();
        Set<String> includes = parseIncludes(include);
        return ResponseEntity.ok(eventService.eventToMap(event,
                includes.contains(Constants.FLD_REACTIONS) ? reactionService.getReactions(event.getTitle()) : null,
                includes.contains(Constants.FLD_REVIEWS) ? reviewService.getReviewsSummary(event.getTitle()) : null));
    }

    @GetMapping("/events")
    public ResponseEntity<?> getEvents(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String user,
            @RequestParam(name = Constants.PARAM_PRICE_FROM, required = false) String priceFrom,
            @RequestParam(name = Constants.PARAM_PRICE_TO, required = false) String priceTo,
            @RequestParam(name = Constants.PARAM_DATE_FROM, required = false) String dateFrom,
            @RequestParam(name = Constants.PARAM_DATE_TO, required = false) String dateTo,
            @RequestParam(defaultValue = Constants.PARAM_DEFAULT_LIMIT) String limit,
            @RequestParam(defaultValue = Constants.PARAM_DEFAULT_OFFSET) String offset,
            @RequestParam(required = false) String include,
            HttpServletRequest request,
            HttpServletResponse response) {

        String sid = CookieHelper.extractSid(request);
        refreshSessionIfExists(sid, response);

        int limitInt = parseParamInt(limit, Constants.PARAM_LIMIT);
        int offsetInt = parseParamInt(offset, Constants.PARAM_OFFSET);

        if (category != null && !eventService.isValidCategory(category)) {
            throw new ValidationException(Constants.FLD_CATEGORY);
        }

        Integer priceFromInt = parseOptionalParamInt(priceFrom, Constants.PARAM_PRICE_FROM);
        Integer priceToInt = parseOptionalParamInt(priceTo, Constants.PARAM_PRICE_TO);
        LocalDate dateFromParsed = parseOptionalParamDate(dateFrom, Constants.PARAM_DATE_FROM);
        LocalDate dateToParsed = parseOptionalParamDate(dateTo, Constants.PARAM_DATE_TO);

        if (limitInt == 0) {
            return ResponseEntity.ok(Map.of(Constants.FLD_EVENTS, List.of(), Constants.FLD_COUNT, 0));
        }

        Page<Event> page = eventService.searchEvents(title, id, category, city, user,
                priceFromInt, priceToInt, dateFromParsed, dateToParsed, limitInt, offsetInt);

        Set<String> includes = parseIncludes(include);
        List<Map<String, Object>> eventMaps = page.getContent().stream()
            .map(e -> eventService.eventToMap(e,
                    includes.contains(Constants.FLD_REACTIONS) ? reactionService.getReactions(e.getTitle()) : null,
                    includes.contains(Constants.FLD_REVIEWS) ? reviewService.getReviewsSummary(e.getTitle()) : null))
            .toList();

        return ResponseEntity.ok(Map.of(Constants.FLD_EVENTS, eventMaps, Constants.FLD_COUNT, page.getTotalElements()));
    }
}
