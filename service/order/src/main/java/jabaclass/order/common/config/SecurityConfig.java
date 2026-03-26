package jabaclass.order.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jabaclass.auth.filter.JwtAuthenticationFilter;
import jabaclass.auth.jwt.JwtProperties;
import jabaclass.auth.jwt.JwtProvider;
import jabaclass.auth.jwt.JwtTokenResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public JwtProvider jwtProvider(JwtProperties jwtProperties) {
        return new JwtProvider(jwtProperties);
    }

    @Bean
    public JwtTokenResolver jwtTokenResolver() {
        return new JwtTokenResolver();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
        JwtProvider jwtProvider,
        JwtTokenResolver jwtTokenResolver,
        ObjectMapper objectMapper
    ) {
        return new JwtAuthenticationFilter(jwtProvider, jwtTokenResolver, objectMapper);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
