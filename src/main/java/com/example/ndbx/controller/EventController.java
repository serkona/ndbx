package com.example.ndbx.controller;

import com.example.ndbx.model.Event;
import com.example.ndbx.repository.EventRepository;
import com.example.ndbx.service.SessionService;
import com.example.ndbx.util.CookieHelper;
import com.example.ndbx.util.OffsetScrollRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
public class EventController {

    private final EventRepository eventRepository;
    private final SessionService sessionService;

    public EventController(EventRepository eventRepository, SessionService sessionService) {
        this.eventRepository = eventRepository;
        this.sessionService = sessionService;
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

        String title = (String) body.get("title");
        String address = (String) body.get("address");
        String startedAt = (String) body.get("started_at");
        String finishedAt = (String) body.get("finished_at");
        String description = (String) body.get("description");

        if (title == null || title.trim().isEmpty()) {
            refreshSessionIfExists(sid, response);
            return badRequest("title");
        }
        if (address == null || address.trim().isEmpty()) {
            refreshSessionIfExists(sid, response);
            return badRequest("address");
        }
        if (startedAt == null || !isValidDate(startedAt)) {
            refreshSessionIfExists(sid, response);
            return badRequest("started_at");
        }
        if (finishedAt == null || !isValidDate(finishedAt)) {
            refreshSessionIfExists(sid, response);
            return badRequest("finished_at");
        }

        if (eventRepository.existsByTitle(title)) {
            refreshSessionIfExists(sid, response);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "event already exists"));
        }

        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        Event.Location location = new Event.Location();
        location.setAddress(address);
        event.setLocation(location);
        event.setCreatedAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        event.setCreatedBy(userId);
        event.setStartedAt(startedAt);
        event.setFinishedAt(finishedAt);

        event = eventRepository.save(event);

        sessionService.refreshSession(sid);
        CookieHelper.setSessionCookie(response, sid, sessionService.getTtlSeconds());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", event.getId()));
    }

    @GetMapping("/events")
    public ResponseEntity<?> getEvents(
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "10") String limit,
            @RequestParam(defaultValue = "0") String offset,
            HttpServletRequest request,
            HttpServletResponse response) {

        int limitInt;
        int offsetInt;
        try {
            limitInt = Integer.parseInt(limit);
            if (limitInt < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return badRequestParameter("limit");
        }

        try {
            offsetInt = Integer.parseInt(offset);
            if (offsetInt < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return badRequestParameter("offset");
        }

        String sid = CookieHelper.extractSid(request);
        if (!sid.isEmpty() && sessionService.sessionExists(sid)) {
            CookieHelper.setSessionCookie(response, sid, sessionService.getTtlSeconds());
        }

        if (limitInt == 0) {
            return ResponseEntity.ok(Map.of("events", List.of(), "count", 0));
        }

        Pageable pageRequest = new OffsetScrollRequest(offsetInt, limitInt);
        Page<Event> page;
        if (title != null && !title.trim().isEmpty()) {
            page = eventRepository.findByTitleLike(title, pageRequest);
        } else {
            page = eventRepository.findAll(pageRequest);
        }

        List<Map<String, Object>> events = page.getContent().stream().map(e -> Map.of(
                "id", e.getId(),
                "title", e.getTitle(),
                "description", e.getDescription() != null ? e.getDescription() : "",
                "location", Map.of("address", e.getLocation().getAddress()),
                "created_at", e.getCreatedAt(),
                "created_by", e.getCreatedBy(),
                "started_at", e.getStartedAt(),
                "finished_at", e.getFinishedAt()
        )).toList();

        return ResponseEntity.ok(Map.of("events", events, "count", events.size()));
    }

    private boolean isValidDate(String dateStr) {
        try {
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(dateStr);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private ResponseEntity<?> badRequest(String field) {
        return ResponseEntity.badRequest().body(Map.of("message", "invalid \"" + field + "\" field"));
    }

    private ResponseEntity<?> badRequestParameter(String param) {
        return ResponseEntity.badRequest().body(Map.of("message", "invalid \"" + param + "\" parameter"));
    }

    private void refreshSessionIfExists(String sid, HttpServletResponse response) {
        if (!sid.isEmpty() && sessionService.sessionExists(sid)) {
            sessionService.refreshSession(sid);
            CookieHelper.setSessionCookie(response, sid, sessionService.getTtlSeconds());
        }
    }
}