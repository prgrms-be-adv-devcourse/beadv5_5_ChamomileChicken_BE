package jabaclass.auth.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jabaclass.auth.exception.JwtAuthException;
import jabaclass.auth.exception.JwtErrorCode;
import jabaclass.auth.jwt.JwtProvider;
import jabaclass.auth.jwt.JwtTokenResolver;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final JwtTokenResolver tokenResolver;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtProvider jwtProvider,
                                   JwtTokenResolver tokenResolver,
                                   ObjectMapper objectMapper) {
        this.jwtProvider = jwtProvider;
        this.tokenResolver = tokenResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = tokenResolver.resolveToken(request);

        // 토큰 없으면 조용히 통과 (permitAll 요청 고려)
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtProvider.parseClaims(token);

            if (!jwtProvider.isAccessToken(claims)) {
                writeErrorResponse(response, JwtErrorCode.INVALID_TOKEN);
                return;
            }

            UUID userId = jwtProvider.getUserId(claims);

            // ROLE은 Redis 도입 후 처리 예정
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.emptyList()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtAuthException e) {
            writeErrorResponse(response, e.getErrorCode());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeErrorResponse(HttpServletResponse response,
                                    JwtErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(
                        Map.of("message", errorCode.getMessage())
                )
        );
    }
}