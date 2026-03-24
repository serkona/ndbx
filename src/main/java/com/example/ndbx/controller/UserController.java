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

@RestController
public class UserController {

    private final UserRepository userRepository;
    private final SessionService sessionService;

    public UserController(UserRepository userRepository, SessionService sessionService) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    @PostMapping("/users")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body, HttpServletRequest request, HttpServletResponse response) {
        String fullName = body.get("full_name");
        String username = body.get("username");
        String password = body.get("password");
        String sid = CookieHelper.extractSid(request);

        if (fullName == null || fullName.trim().isEmpty()) {
            refreshSessionIfExists(sid, response);
            return badRequest("full_name");
        }
        if (username == null || username.trim().isEmpty()) {
            refreshSessionIfExists(sid, response);
            return badRequest("username");
        }
        if (password == null || password.trim().isEmpty()) {
            refreshSessionIfExists(sid, response);
            return badRequest("password");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            refreshSessionIfExists(sid, response);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "user already exists"));
        }

        User user = new User();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        user = userRepository.save(user);

        if (!sid.isEmpty() && sessionService.sessionExists(sid)) {
            sessionService.refreshSession(sid);
            sessionService.bindUserToSession(sid, user.getId());
        } else {
            sid = sessionService.createSession(user.getId());
        }
        CookieHelper.setSessionCookie(response, sid, sessionService.getTtlSeconds());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private ResponseEntity<?> badRequest(String field) {
        return ResponseEntity.badRequest().body(Map.of("message", "invalid \"" + field + "\" field"));
    }

    private void refreshSessionIfExists(String sid, HttpServletResponse response) {
        if (!sid.isEmpty() && sessionService.sessionExists(sid)) {
            sessionService.refreshSession(sid);
            CookieHelper.setSessionCookie(response, sid, sessionService.getTtlSeconds());
        }
    }
}