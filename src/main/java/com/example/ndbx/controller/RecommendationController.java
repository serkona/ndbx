package com.example.ndbx.controller;

import com.example.ndbx.service.RecommendationService;
import com.example.ndbx.service.SessionService;
import com.example.ndbx.util.Constants;
import com.example.ndbx.util.CookieHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class RecommendationController extends BaseController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService,
                                    SessionService sessionService) {
        super(sessionService);
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations(HttpServletRequest request,
                                                HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        if (sid.isEmpty() || !sessionService.sessionExists(sid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = sessionService.getUserId(sid);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        refreshSessionIfExists(sid, response);

        List<Map<String, Object>> events = recommendationService.getRecommendations(userId);
        return ResponseEntity.ok(Map.of(Constants.FLD_EVENTS, events));
    }
}