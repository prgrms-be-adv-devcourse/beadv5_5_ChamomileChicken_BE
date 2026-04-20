package jabaclass.apigateway.domain.model;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Getter;

@Getter
@Table("gateway_whitelist")
public class WhitelistEntry {

	@Id
	private UUID id;

	private String method;

	private String pathPattern;

	private String description;

	private boolean enabled;
}
