package ru.yandex.practicum.oauth0.rs.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.yandex.practicum.oauth0.rs.RsProperties;
import ru.yandex.practicum.oauth0.rs.util.JwtUtil;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class BearerTokenFilter extends OncePerRequestFilter {

    private final RsProperties props;
    private final JwtUtil jwtUtil;

    public BearerTokenFilter(RsProperties props) {
        this.props = props;
        this.jwtUtil = new JwtUtil(props.getAuthSecret(), props.getClockSkewSec());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        Map<String, Object> payload;
        try {
            payload = jwtUtil.verifyAndDecode(token);
        } catch (JwtUtil.TokenException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return;
        }

        String aud = (String) payload.get("aud");
        if (!props.getAudience().equals(aud)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid audience");
            return;
        }

        List<String> scopes = (List<String>) payload.get("scopes");
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (path.startsWith("/api/payments")) {
            if ("GET".equals(method)) {
                if (scopes == null || !scopes.contains("payments:read")) {
                    sendError(response, HttpServletResponse.SC_FORBIDDEN, "Insufficient scope: payments:read required");
                    return;
                }
            } else if ("POST".equals(method)) {
                if (scopes == null || !scopes.contains("payments:write")) {
                    sendError(response, HttpServletResponse.SC_FORBIDDEN,
                            "Insufficient scope: payments:write required");
                    return;
                }
            }
        }

        request.setAttribute("tokenData", payload);
        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/health");
    }
}