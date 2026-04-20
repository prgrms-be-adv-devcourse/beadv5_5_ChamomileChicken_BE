package jabaclass.settlement.infrastructure.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jabaclass.settlement.common.auth.CurrentUserArgumentResolver;
import jabaclass.settlement.common.auth.CurrentUserRoleArgumentResolver;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

	private final CurrentUserArgumentResolver currentUserArgumentResolver;
	private final CurrentUserRoleArgumentResolver currentUserRoleArgumentResolver;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentUserArgumentResolver);
		resolvers.add(currentUserRoleArgumentResolver);
	}
}
