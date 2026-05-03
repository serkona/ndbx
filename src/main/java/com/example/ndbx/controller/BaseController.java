package com.example.ndbx.controller;

import com.example.ndbx.exception.ValidationException;
import com.example.ndbx.service.SessionService;
import com.example.ndbx.util.CookieHelper;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseController {

    private static final String DATE_QUERY_PATTERN = "yyyyMMdd";

    protected final SessionService sessionService;

    protected BaseController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    protected void refreshSessionIfExists(String sid, HttpServletResponse response) {
        if (sid != null && !sid.isEmpty() && sessionService.sessionExists(sid)) {
            sessionService.refreshSession(sid);
            CookieHelper.setSessionCookie(response, sid, sessionService.getTtlSeconds());
        }
    }

    protected String requireStringBody(Map<String, ?> body, String field) {
        Object val = body.get(field);
        if (!(val instanceof String) || ((String) val).trim().isEmpty()) {
            throw new ValidationException(field);
        }
        return (String) val;
    }

    protected String requireDateBody(Map<String, ?> body, String field) {
        String val = requireStringBody(body, field);
        try {
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(val);
        } catch (DateTimeParseException e) {
            throw new ValidationException(field);
        }
        return val;
    }

    protected int parseParamInt(String paramValue, String paramName) {
        try {
            int val = Integer.parseInt(paramValue);
            if (val < 0) throw new ValidationException(paramName);
            return val;
        } catch (NumberFormatException e) {
            throw new ValidationException(paramName);
        }
    }

    protected Integer parseOptionalParamInt(String paramValue, String paramName) {
        if (paramValue == null) return null;
        return parseParamInt(paramValue, paramName);
    }

    protected LocalDate parseOptionalParamDate(String paramValue, String paramName) {
        if (paramValue == null) return null;
        try {
            return LocalDate.parse(paramValue, DateTimeFormatter.ofPattern(DATE_QUERY_PATTERN));
        } catch (DateTimeParseException e) {
            throw new ValidationException(paramName);
        }
    }

    protected Set<String> parseIncludes(String include) {
        if (include == null || include.isBlank()) return Set.of();
        return Arrays.stream(include.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
