package jabaclass.product.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.auth.filter.JwtAuthenticationFilter;
import jabaclass.auth.jwt.JwtProperties;
import jabaclass.auth.jwt.JwtProvider;
import jabaclass.auth.jwt.JwtTokenResolver;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
// Could not autowire. No beans of 'JwtProperties' type found 에러 해결
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	private final JwtProperties jwtProperties;

	// TODO 임의로 작동되게 해놓은 것이기 때문에, 추후 작업하시면서 수정해야함
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
		return httpSecurity.httpBasic(HttpBasicConfigurer::disable)
			.formLogin(FormLoginConfigurer::disable)
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth ->
				auth.requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/v1/products/*/reviews/user").authenticated()
					.requestMatchers("/swagger-ui/**")
					.permitAll()
					.requestMatchers("/swagger-ui.html")
					.permitAll()
					.requestMatchers("/v3/**")
					.permitAll()
					.anyRequest().authenticated()
			)
			.addFilterBefore(
				new JwtAuthenticationFilter(new JwtProvider(jwtProperties), new JwtTokenResolver(), new ObjectMapper()),
				UsernamePasswordAuthenticationFilter.class
			)
			.build();
	}

}
