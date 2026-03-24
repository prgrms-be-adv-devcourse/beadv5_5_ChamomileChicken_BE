package jabaclass.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	// TODO 임의로 작동되게 해놓은 것이기 때문에, 추후 작업하시면서 수정해야함
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
		return httpSecurity.httpBasic(HttpBasicConfigurer::disable)
			.formLogin(FormLoginConfigurer::disable)
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth ->
				auth.requestMatchers("/api/**").permitAll()
					.requestMatchers("/swagger-ui/**").permitAll()
					.requestMatchers("/swagger-ui.html").permitAll()
					.requestMatchers("/v3/**").permitAll()
			)
			.build();
	}

}
