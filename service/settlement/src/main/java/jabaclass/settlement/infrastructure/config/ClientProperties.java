package jabaclass.settlement.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clients")
public record ClientProperties(
	Endpoint payment,
	Endpoint order,
	Endpoint product,
	Endpoint user,
	Endpoint transfer
) {
	public record Endpoint(
		String baseUrl
	) {
	}
}