package jabaclass.file.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalApiFilter extends OncePerRequestFilter {

    @Value("${internal.api.key}")
    private String internalKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // internal API만 검사
        if (uri.startsWith("/api/internal/")) {

            String headerKey = request.getHeader("X-INTERNAL-KEY");

            if (headerKey == null || !headerKey.equals(internalKey)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid internal API key");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
