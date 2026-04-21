package jabaclass.product.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {
	String jwtSchemeName = "JWT";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.servers(List.of(new Server().url("/")))
			.addSecurityItem(new SecurityRequirement().addList(jwtSchemeName))
			.components(new Components()
				.addSecuritySchemes(jwtSchemeName,
					new SecurityScheme()
						.name("Authorization")
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")
				)
			)
			.info(new Info()
				// 본인 모듈 명으로 바꿔주세요.
				.title("Product API")
				.description("Product Swagger API Docs")
				.version("v1"));
	}
}
