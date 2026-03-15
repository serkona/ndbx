package com.example.ndbx;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private static final String COOKIE_NAME = "X-Session-Id";

    private final SessionService sessionService;

    public HealthController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/health")
    public Map<String, String> health(HttpServletRequest request, HttpServletResponse response) {
        String sid = extractSid(request);
        if (!sid.isEmpty() && sessionService.sessionExists(sid)) {
            Cookie cookie = new Cookie(COOKIE_NAME, sid);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(sessionService.getTtlSeconds());
            response.addCookie(cookie);
        }
        return Map.of("status", "ok");
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
}
