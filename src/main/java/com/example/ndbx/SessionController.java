package com.example.ndbx;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    private static final String COOKIE_NAME = "X-Session-Id";

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/session")
    public ResponseEntity<Void> session(HttpServletRequest request, HttpServletResponse response) {
        String sid = extractSid(request);
        if (!sid.isEmpty() && sessionService.sessionExists(sid)) {
            sessionService.refreshSession(sid);
            setSessionCookie(response, sid);
            return ResponseEntity.ok().build();
        }

        String newSid = sessionService.createSession();
        setSessionCookie(response, newSid);
        return ResponseEntity.status(201).build();
    }

    private String extractSid(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return "";
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return "";
    }

    private void setSessionCookie(HttpServletResponse response, String sid) {
        Cookie cookie = new Cookie(COOKIE_NAME, sid);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(sessionService.getTtlSeconds());
        response.addCookie(cookie);
    }
}
