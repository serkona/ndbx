package com.example.ndbx.controller;

import com.example.ndbx.exception.ValidationException;
import com.example.ndbx.model.Event;
import com.example.ndbx.model.User;
import com.example.ndbx.service.EventService;
import com.example.ndbx.service.ReactionService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class UserController extends BaseController {

    private final UserService userService;
    private final EventService eventService;
    private final ReactionService reactionService;

    public UserController(UserService userService, EventService eventService,
                          ReactionService reactionService, SessionService sessionService) {
        super(sessionService);
        this.userService = userService;
        this.eventService = eventService;
        this.reactionService = reactionService;
    }

    @PostMapping("/users")
    public ResponseEntity<?> register(@RequestBody Map<String, ?> body, HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        refreshSessionIfExists(sid, response);

        String fullName = requireStringBody(body, Constants.FLD_FULL_NAME);
        String username = requireStringBody(body, Constants.FLD_USERNAME);
        String password = requireStringBody(body, Constants.FLD_PASSWORD);

        if (userService.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(Constants.FLD_MESSAGE, "user already exists"));
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
            @RequestParam(defaultValue = Constants.PARAM_DEFAULT_LIMIT) String limit,
            @RequestParam(defaultValue = Constants.PARAM_DEFAULT_OFFSET) String offset,
            HttpServletRequest request,
            HttpServletResponse response) {

        String sid = CookieHelper.extractSid(request);
        refreshSessionIfExists(sid, response);

        int limitInt = parseParamInt(limit, Constants.PARAM_LIMIT);
        int offsetInt = parseParamInt(offset, Constants.PARAM_OFFSET);

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
                                           @RequestParam(required = false) String title,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) String city,
                                           @RequestParam(name = Constants.PARAM_PRICE_FROM, required = false) String priceFrom,
                                           @RequestParam(name = Constants.PARAM_PRICE_TO, required = false) String priceTo,
                                           @RequestParam(name = Constants.PARAM_DATE_FROM, required = false) String dateFrom,
                                           @RequestParam(name = Constants.PARAM_DATE_TO, required = false) String dateTo,
                                           @RequestParam(defaultValue = Constants.PARAM_DEFAULT_LIMIT) String limit,
                                           @RequestParam(defaultValue = Constants.PARAM_DEFAULT_OFFSET) String offset,
                                           @RequestParam(required = false) String include,
                                           HttpServletRequest request, HttpServletResponse response) {
        String sid = CookieHelper.extractSid(request);
        refreshSessionIfExists(sid, response);

        Optional<User> userOpt = userService.getUserById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(Constants.FLD_MESSAGE, "User not found"));
        }

        int limitInt = parseParamInt(limit, Constants.PARAM_LIMIT);
        int offsetInt = parseParamInt(offset, Constants.PARAM_OFFSET);

        if (category != null && !eventService.isValidCategory(category)) {
            throw new ValidationException(Constants.FLD_CATEGORY);
        }

        Integer priceFromInt = parseOptionalParamInt(priceFrom, Constants.PARAM_PRICE_FROM);
        Integer priceToInt = parseOptionalParamInt(priceTo, Constants.PARAM_PRICE_TO);
        LocalDate dateFromParsed = parseOptionalParamDate(dateFrom, Constants.PARAM_DATE_FROM);
        LocalDate dateToParsed = parseOptionalParamDate(dateTo, Constants.PARAM_DATE_TO);

        if (limitInt == 0) {
            return ResponseEntity.ok(Map.of(Constants.FLD_EVENTS, List.of(), Constants.FLD_COUNT, 0));
        }

        Page<Event> page = eventService.searchEvents(title, null, category, city, userOpt.get().getUsername(),
                priceFromInt, priceToInt, dateFromParsed, dateToParsed, limitInt, offsetInt);

        boolean withReactions = Constants.FLD_REACTIONS.equals(include);
        List<Map<String, Object>> eventMaps = page.getContent().stream()
            .map(e -> eventService.eventToMap(e, withReactions ? reactionService.getReactions(e.getTitle()) : null))
            .toList();

        return ResponseEntity.ok(Map.of(Constants.FLD_EVENTS, eventMaps, Constants.FLD_COUNT, page.getTotalElements()));
    }
}
