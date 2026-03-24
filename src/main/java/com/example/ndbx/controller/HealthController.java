package com.example.ndbx.controller;

import com.example.ndbx.service.SessionService;
import com.example.ndbx.util.CookieHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final SessionService sessionService;

    public HealthController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/health")
    public Map<String, String> health(HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        if (!sid.isEmpty() && sessionService.sessionExists(sid)) {
            CookieHelper.setSessionCookie(response, sid, sessionService.getTtlSeconds());
        }
        return Map.of("status", "ok");
    }
}