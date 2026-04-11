package com.example.ndbx.controller;

import com.example.ndbx.service.EventService;
import com.example.ndbx.service.ReactionService;
import com.example.ndbx.service.SessionService;
import com.example.ndbx.util.Constants;
import com.example.ndbx.util.CookieHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ReactionController extends BaseController {

    private final ReactionService reactionService;
    private final EventService eventService;

    public ReactionController(ReactionService reactionService, EventService eventService, SessionService sessionService) {
        super(sessionService);
        this.reactionService = reactionService;
        this.eventService = eventService;
    }

    @PostMapping("/events/{" + Constants.PV_EVENT_ID + "}/like")
    public ResponseEntity<?> like(@PathVariable(Constants.PV_EVENT_ID) String eventId,
                                  HttpServletRequest request, HttpServletResponse response) {
        return react(eventId, true, request, response);
    }

    @PostMapping("/events/{" + Constants.PV_EVENT_ID + "}/dislike")
    public ResponseEntity<?> dislike(@PathVariable(Constants.PV_EVENT_ID) String eventId,
                                     HttpServletRequest request, HttpServletResponse response) {
        return react(eventId, false, request, response);
    }

    private ResponseEntity<?> react(String eventId, boolean like,
                                    HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        if (sid.isEmpty() || !sessionService.sessionExists(sid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = sessionService.getUserId(sid);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (eventService.getEventById(eventId).isEmpty()) {
            refreshSessionIfExists(sid, response);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(Constants.FLD_MESSAGE, "Event not found"));
        }

        refreshSessionIfExists(sid, response);

        if (like) {
            reactionService.setLike(eventId, userId);
        } else {
            reactionService.setDislike(eventId, userId);
        }

        return ResponseEntity.noContent().build();
    }
}
