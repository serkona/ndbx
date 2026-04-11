package com.example.ndbx.controller;

import com.example.ndbx.model.User;
import com.example.ndbx.service.SessionService;
import com.example.ndbx.service.UserService;
import com.example.ndbx.util.Constants;
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
public class AuthController extends BaseController {

    private final UserService userService;

    public AuthController(UserService userService, SessionService sessionService) {
        super(sessionService);
        this.userService = userService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, ?> body, HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        refreshSessionIfExists(sid, response);

        Object userObj = body.get(Constants.FLD_USERNAME);
        Object passObj = body.get(Constants.FLD_PASSWORD);
        if (!(userObj instanceof String username) || username.trim().isEmpty() ||
            !(passObj instanceof String password) || password.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(Constants.FLD_MESSAGE, Constants.MSG_INVALID_CREDENTIALS));
        }

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty() || !BCrypt.checkpw(password, userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(Constants.FLD_MESSAGE, Constants.MSG_INVALID_CREDENTIALS));
        }

        User user = userOpt.get();
        if (!sid.isEmpty() && sessionService.sessionExists(sid)) {
            sessionService.bindUserToSession(sid, user.getId());
        } else {
            sid = sessionService.createSession(user.getId());
            CookieHelper.setSessionCookie(response, sid, sessionService.getTtlSeconds());
        }

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
}
