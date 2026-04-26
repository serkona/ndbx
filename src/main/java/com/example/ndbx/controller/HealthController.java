package com.example.ndbx.controller;

import com.example.ndbx.service.SessionService;
import com.example.ndbx.util.CookieHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController extends BaseController {

    public HealthController(SessionService sessionService) {
        super(sessionService);
    }

    @GetMapping("/health")
    public Map<String, String> health(HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        refreshSessionIfExists(sid, response);
        return Map.of("status", "ok");
    }
}
