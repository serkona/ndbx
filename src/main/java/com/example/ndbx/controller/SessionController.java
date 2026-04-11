package com.example.ndbx.controller;

import com.example.ndbx.service.SessionService;
import com.example.ndbx.util.CookieHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController extends BaseController {

    public SessionController(SessionService sessionService) {
        super(sessionService);
    }

    @PostMapping("/session")
    public ResponseEntity<Void> session(HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        if (!sid.isEmpty() && sessionService.sessionExists(sid)) {
            refreshSessionIfExists(sid, response);
            return ResponseEntity.ok().build();
        }

        String newSid = sessionService.createSession();
        CookieHelper.setSessionCookie(response, newSid, sessionService.getTtlSeconds());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
