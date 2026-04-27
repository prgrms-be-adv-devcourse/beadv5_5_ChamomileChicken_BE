package jabaclass.apigateway.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "gateway")
public record GatewayRulesProperties (List<WhitelistEntry> whitelist, List<RbacEntry> rbac) {

	public GatewayRulesProperties {
		whitelist = whitelist == null ? List.of() : whitelist;
		rbac = rbac == null ? List.of() : rbac;
	}

	public record WhitelistEntry(String method, String path) {}

	public record RbacEntry(String method, String path, String allowedRoles) {}
}
