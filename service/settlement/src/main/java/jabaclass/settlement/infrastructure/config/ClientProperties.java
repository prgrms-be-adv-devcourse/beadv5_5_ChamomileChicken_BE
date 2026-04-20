package jabaclass.settlement.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clients")
public record ClientProperties(
	Endpoint user
) {
	public record Endpoint(
		String baseUrl
	) {
	}
}
