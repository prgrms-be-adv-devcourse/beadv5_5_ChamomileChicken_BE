package jabaclass.apigateway.domain.model;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Getter;

@Getter
@Table("gateway_route_policy")
public class RoutePolicy {

	@Id
	private UUID id;

	private String method;

	private String pathPattern;

	private String allowedRoles;

	private String description;

	private boolean enabled;
}
