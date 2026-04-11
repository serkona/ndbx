package com.example.ndbx.controller;

import com.example.ndbx.model.Event;
import com.example.ndbx.model.User;
import com.example.ndbx.service.EventService;
import com.example.ndbx.service.SessionService;
import com.example.ndbx.service.UserService;
import com.example.ndbx.util.Constants;
import com.example.ndbx.util.CookieHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class UserController extends BaseController {

    private final UserService userService;
    private final EventService eventService;

    public UserController(UserService userService, EventService eventService, SessionService sessionService) {
        super(sessionService);
        this.userService = userService;
        this.eventService = eventService;
    }

    @PostMapping("/users")
    public ResponseEntity<?> register(@RequestBody Map<String, ?> body, HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        refreshSessionIfExists(sid, response);

        String fullName = requireStringBody(body, Constants.FLD_FULL_NAME);
        String username = requireStringBody(body, Constants.FLD_USERNAME);
        String password = requireStringBody(body, Constants.FLD_PASSWORD);

        if (userService.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(Constants.FLD_MESSAGE, Constants.MSG_USER_ALREADY_EXISTS));
        }

        User user = userService.registerUser(fullName, username, password);

        if (!sid.isEmpty() && sessionService.sessionExists(sid)) {
            sessionService.bindUserToSession(sid, user.getId());
        } else {
            sid = sessionService.createSession(user.getId());
            CookieHelper.setSessionCookie(response, sid, sessionService.getTtlSeconds());
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String id,
            @RequestParam(defaultValue = "10") String limit,
            @RequestParam(defaultValue = "0") String offset,
            HttpServletRequest request,
            HttpServletResponse response) {

        String sid = CookieHelper.extractSid(request);
        refreshSessionIfExists(sid, response);

        int limitInt = parseParamInt(limit, "limit");
        int offsetInt = parseParamInt(offset, "offset");

        if (limitInt == 0) {
            return ResponseEntity.ok(Map.of(Constants.FLD_USERS, List.of(), Constants.FLD_COUNT, 0));
        }

        if (id != null && !id.isEmpty()) {
            Optional<User> userOpt = userService.getUserById(id);
            return userOpt.map(
                    user -> ResponseEntity.ok(
                            Map.of(Constants.FLD_USERS, List.of(userService.userToMap(user)), Constants.FLD_COUNT, 1)
                    )
            ).orElseGet(
                    () -> ResponseEntity.ok(Map.of(Constants.FLD_USERS, List.of(), Constants.FLD_COUNT, 0))
            );
        }

        Page<User> page = userService.searchUsers(name, limitInt, offsetInt);
        List<Map<String, Object>> users = page.getContent().stream().map(userService::userToMap).toList();
        return ResponseEntity.ok(Map.of(Constants.FLD_USERS, users, Constants.FLD_COUNT, users.size()));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id,
                                         HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        refreshSessionIfExists(sid, response);

        Optional<User> userOpt = userService.getUserById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(Constants.FLD_MESSAGE, Constants.MSG_NOT_FOUND));
        }

        return ResponseEntity.ok(userService.userToMap(userOpt.get()));
    }

    @GetMapping("/users/{id}/events")
    public ResponseEntity<?> getUserEvents(@PathVariable String id,
                                           HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        refreshSessionIfExists(sid, response);

        Optional<User> userOpt = userService.getUserById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(Constants.FLD_MESSAGE, Constants.MSG_USER_NOT_FOUND));
        }

        List<Event> events = eventService.getEventsByUserId(id);
        List<Map<String, Object>> eventMaps = events.stream().map(eventService::eventToMap).toList();

        return ResponseEntity.ok(Map.of(Constants.FLD_EVENTS, eventMaps, Constants.FLD_COUNT, eventMaps.size()));
    }
}
