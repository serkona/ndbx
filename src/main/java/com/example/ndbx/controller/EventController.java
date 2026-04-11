package com.example.ndbx.controller;

import com.example.ndbx.exception.ValidationException;
import com.example.ndbx.model.Event;
import com.example.ndbx.service.EventService;
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

@RestController
public class EventController extends BaseController {

    private final EventService eventService;

    public EventController(EventService eventService, SessionService sessionService) {
        super(sessionService);
        this.eventService = eventService;
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
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(Constants.FLD_MESSAGE, Constants.MSG_EVENT_ALREADY_EXISTS));
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
                    .body(Map.of(Constants.FLD_MESSAGE, Constants.MSG_EVENT_NOT_FOUND_OR_NOT_ORGANIZER));
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<?> getEventById(@PathVariable String id,
                                          HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        refreshSessionIfExists(sid, response);

        Optional<Event> eventOpt = eventService.getEventById(id);
        if (eventOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(Constants.FLD_MESSAGE, Constants.MSG_NOT_FOUND));
        }

        return ResponseEntity.ok(eventService.eventToMap(eventOpt.get()));
    }

    @GetMapping("/events")
    public ResponseEntity<?> getEvents(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String user,
            @RequestParam(name = "price_from", required = false) String priceFrom,
            @RequestParam(name = "price_to", required = false) String priceTo,
            @RequestParam(name = "date_from", required = false) String dateFrom,
            @RequestParam(name = "date_to", required = false) String dateTo,
            @RequestParam(defaultValue = "10") String limit,
            @RequestParam(defaultValue = "0") String offset,
            HttpServletRequest request,
            HttpServletResponse response) {

        String sid = CookieHelper.extractSid(request);
        refreshSessionIfExists(sid, response);

        int limitInt = parseParamInt(limit, "limit");
        int offsetInt = parseParamInt(offset, "offset");

        if (category != null && !eventService.isValidCategory(category)) {
            throw new ValidationException(Constants.FLD_CATEGORY);
        }

        Integer priceFromInt = parseOptionalParamInt(priceFrom, "price_from");
        Integer priceToInt = parseOptionalParamInt(priceTo, "price_to");
        LocalDate dateFromParsed = parseOptionalParamDate(dateFrom, "date_from");
        LocalDate dateToParsed = parseOptionalParamDate(dateTo, "date_to");

        if (limitInt == 0) {
            return ResponseEntity.ok(Map.of(Constants.FLD_EVENTS, List.of(), Constants.FLD_COUNT, 0));
        }

        Page<Event> page = eventService.searchEvents(title, id, category, city, user,
                priceFromInt, priceToInt, dateFromParsed, dateToParsed, limitInt, offsetInt);

        List<Map<String, Object>> eventMaps = page.getContent().stream().map(eventService::eventToMap).toList();

        return ResponseEntity.ok(Map.of(Constants.FLD_EVENTS, eventMaps, Constants.FLD_COUNT, page.getTotalElements()));
    }
}
