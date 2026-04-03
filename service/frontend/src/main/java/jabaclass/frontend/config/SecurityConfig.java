package jabaclass.frontend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final SessionAuthFilter sessionAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();

        http
            .csrf(csrf -> csrf
                // XSRF-TOKEN 쿠키로 토큰 관리 (JS에서 읽어 X-XSRF-TOKEN 헤더로 전송)
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/signup", "/signup/send-code", "/signup/verify-code", "/error", "/css/**", "/js/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .addFilterBefore(sessionAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendRedirect("/login")
                )
            );

        return http.build();
    }
}
