package com.coagent4u.app.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;

/**
 * Filter to log REST API requests and responses professionally.
 * Captures Method, URI, Headers, and Bodies for both request and response.
 */
@Component
public class RestApiLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("com.coagent4u.rest_api");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (isAsyncDispatch(request)) {
            filterChain.doFilter(request, response);
        } else {
            doFilterWrapped(new ContentCachingRequestWrapper(request), new ContentCachingResponseWrapper(response), filterChain);
        }
    }

    protected void doFilterWrapped(@NonNull ContentCachingRequestWrapper request, @NonNull ContentCachingResponseWrapper response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequestResponse(request, response, duration);
            response.copyBodyToResponse();
        }
    }

    private void logRequestResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {
        StringBuilder msg = new StringBuilder();
        msg.append("\n================ REST API TRANSACTION ================\n");
        msg.append(String.format("Method      : %s\n", request.getMethod()));
        msg.append(String.format("URI         : %s\n", request.getRequestURI()));
        if (request.getQueryString() != null) {
            msg.append(String.format("Query String: %s\n", request.getQueryString()));
        }
        
        msg.append("Request Headers: \n");
        Collections.list(request.getHeaderNames()).forEach(headerName -> 
                msg.append(String.format("  %s: %s\n", headerName, request.getHeader(headerName))));

        String requestBody = getContent(request.getContentAsByteArray(), request.getCharacterEncoding());
        if (!requestBody.isEmpty()) {
            msg.append("Request Body: \n").append(requestBody).append("\n");
        }

        msg.append("------------------------------------------------------\n");
        msg.append(String.format("Status      : %d\n", response.getStatus()));
        msg.append(String.format("Duration    : %d ms\n", duration));
        
        msg.append("Response Headers: \n");
        response.getHeaderNames().forEach(headerName -> 
                msg.append(String.format("  %s: %s\n", headerName, response.getHeader(headerName))));

        String responseBody = getContent(response.getContentAsByteArray(), response.getCharacterEncoding());
        if (!responseBody.isEmpty()) {
            msg.append("Response Body: \n").append(responseBody).append("\n");
        }
        msg.append("======================================================");

        log.info(msg.toString());
    }

    private String getContent(byte[] content, String contentEncoding) {
        if (content == null || content.length == 0) {
            return "";
        }
        try {
            return new String(content, contentEncoding != null ? contentEncoding : "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "[Binary Content]";
        }
    }
}
