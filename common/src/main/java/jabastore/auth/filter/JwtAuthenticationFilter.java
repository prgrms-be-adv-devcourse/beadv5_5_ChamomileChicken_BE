package jabastore.auth.filter;

import jabastore.auth.exception.JwtAuthException;
import jabastore.auth.exception.JwtErrorCode;
import jabastore.auth.jwt.JwtProvider;
import jabastore.auth.jwt.JwtTokenResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final JwtTokenResolver tokenResolver;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserDetailsService userDetailsService,
                                   JwtTokenResolver tokenResolver, ObjectMapper objectMapper) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
        this.tokenResolver = tokenResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = tokenResolver.resolveToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!jwtProvider.isAccessToken(token)) {
                writeErrorResponse(response, JwtErrorCode.INVALID_TOKEN);
                return;
            }

            UUID userId = jwtProvider.getUserId(token);

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(userId.toString());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
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
