package com.example.ndbx.controller;

import com.example.ndbx.model.User;
import com.example.ndbx.repository.UserRepository;
import com.example.ndbx.service.SessionService;
import com.example.ndbx.util.CookieHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
public class AuthController {

    private final UserRepository userRepository;
    private final SessionService sessionService;

    public AuthController(UserRepository userRepository, SessionService sessionService) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request, HttpServletResponse response) {
        String username = body.get("username");
        String password = body.get("password");
        String sid = CookieHelper.extractSid(request);

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            refreshSessionIfExists(sid, response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid credentials"));
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !BCrypt.checkpw(password, userOpt.get().getPasswordHash())) {
            refreshSessionIfExists(sid, response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid credentials"));
        }

        User user = userOpt.get();
        if (!sid.isEmpty() && sessionService.sessionExists(sid)) {
            sessionService.refreshSession(sid);
            sessionService.bindUserToSession(sid, user.getId());
        } else {
            sid = sessionService.createSession(user.getId());
        }
        CookieHelper.setSessionCookie(response, sid, sessionService.getTtlSeconds());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        if (sid.isEmpty() || !sessionService.sessionExists(sid) || sessionService.getUserId(sid) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        sessionService.deleteSession(sid);
        CookieHelper.clearSessionCookie(response);
        return ResponseEntity.noContent().build();
    }

    private void refreshSessionIfExists(String sid, HttpServletResponse response) {
        if (!sid.isEmpty() && sessionService.sessionExists(sid)) {
            sessionService.refreshSession(sid);
            CookieHelper.setSessionCookie(response, sid, sessionService.getTtlSeconds());
        }
    }
}