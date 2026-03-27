package jabaclass.user.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.auth.filter.JwtAuthenticationFilter;
import jabaclass.auth.jwt.JwtProperties;
import jabaclass.auth.jwt.JwtProvider;
import jabaclass.auth.jwt.JwtTokenResolver;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	private final JwtProperties jwtProperties;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			);

		http.authorizeHttpRequests(auth -> auth
			.requestMatchers("/api/v1/auth/**").permitAll()
			.requestMatchers("/api/v1/users/**").permitAll()
			.requestMatchers("/api/v1/email/**").permitAll()
			.requestMatchers("/api/v1/auth/login").permitAll()
			.requestMatchers("/api/v1/auth/reissue").permitAll()
			.requestMatchers("/api/v1/deposits/validate").permitAll()
			.requestMatchers("/api/v1/deposits/use").permitAll()
			.requestMatchers("/swagger-ui/**").permitAll()
			.requestMatchers("/v3/api-docs/**").permitAll()
			.requestMatchers("/swagger-resources/**").permitAll()
			.anyRequest().authenticated()
		);

		http.addFilterBefore(
			new JwtAuthenticationFilter(jwtProvider(), tokenResolver(), objectMapper()),
			UsernamePasswordAuthenticationFilter.class
		);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public JwtProvider jwtProvider() {
		return new JwtProvider(jwtProperties);
	}

	@Bean
	public JwtTokenResolver tokenResolver() {
		return new JwtTokenResolver();
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}