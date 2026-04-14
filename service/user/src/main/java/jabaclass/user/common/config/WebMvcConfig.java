package jabaclass.user.common.config;

import java.util.List;

import jabaclass.user.common.auth.CurrentUserRoleArgumentResolver;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jabaclass.user.common.auth.CurrentUserArgumentResolver;

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
