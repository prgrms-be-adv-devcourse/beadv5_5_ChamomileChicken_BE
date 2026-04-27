package jabaclass.apigateway.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "gateway")
public record GatewayRulesProperties (
	@Valid List<WhitelistEntry> whitelist,
	@Valid List<RbacEntry> rbac) {

	public GatewayRulesProperties {
		whitelist = whitelist == null ? List.of() : whitelist;
		rbac = rbac == null ? List.of() : rbac;
	}

	public record WhitelistEntry(@NotBlank String method, @NotBlank String path) {}

	public record RbacEntry(@NotBlank String method, @NotBlank String path, List<String> allowedRoles) {
		public RbacEntry {
			allowedRoles = allowedRoles == null ? List.of() : allowedRoles;
		}
	}
}
