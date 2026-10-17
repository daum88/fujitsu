package com.deliveryfee.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a correlation id to every request. The id is taken from the incoming {@code X-Request-Id}
 * header when present (so callers can trace across systems), otherwise a new one is generated. It
 * is exposed three ways: as the {@code X-Request-Id} response header, in the log via MDC, and in
 * {@code problem+json} error bodies so a client error can be matched to a log line.
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String ATTRIBUTE = "requestId";
    public static final String MDC_KEY = "requestId";

    private static final int MAX_REQUEST_ID_LENGTH = 128;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(HEADER));
        MDC.put(MDC_KEY, requestId);
        request.setAttribute(ATTRIBUTE, requestId);
        response.setHeader(HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * Reuses a caller-supplied correlation id after sanitizing it (control characters removed,
     * length bounded) so it cannot be used for log or header injection; generates a UUID otherwise.
     */
    private static String resolveRequestId(String header) {
        if (header == null || header.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String sanitized = header.replaceAll("[^A-Za-z0-9._-]", "");
        if (sanitized.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sanitized.length() <= MAX_REQUEST_ID_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_REQUEST_ID_LENGTH);
    }
}
