package jabaclass.user.common.apidocs;

import java.util.Collections;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

	private static final String APPLICATION_JSON = "application/json";

	@Bean
	public OpenAPI openAPI() {
		SecurityScheme bearerScheme = new SecurityScheme()
			.type(SecurityScheme.Type.HTTP)
			.scheme("bearer")
			.bearerFormat("JWT")
			.in(SecurityScheme.In.HEADER)
			.name("Authorization");

		SecurityRequirement bearerRequirement = new SecurityRequirement().addList("bearerAuth");

		Info info = new Info()
			.version("1.0.0")
			.title("User API")
			.description("User API 문서");

		return new OpenAPI()
			.components(new Components().addSecuritySchemes("bearerAuth", bearerScheme))
			.security(Collections.singletonList(bearerRequirement))
			.info(info);
	}

	@Bean
	public OperationCustomizer securityErrorCustomizer() {
		return (operation, handlerMethod) -> {
			Schema<?> unauthorizedSchema = new Schema<>()
				.type("object")
				.addProperty("status", new Schema<>().type("string").example("UNAUTHORIZED"))
				.addProperty("message", new Schema<>().type("string").example("인증이 필요합니다."))
				.addProperty("data", new Schema<>().nullable(true).example(null));

			operation.getResponses().addApiResponse("401", new ApiResponse()
				.description("Unauthorized")
				.content(new Content().addMediaType(
					APPLICATION_JSON,
					new MediaType().schema(unauthorizedSchema)
				)));

			Schema<?> forbiddenSchema = new Schema<>()
				.type("object")
				.addProperty("status", new Schema<>().type("string").example("FORBIDDEN"))
				.addProperty("message", new Schema<>().type("string").example("접근 권한이 없습니다."))
				.addProperty("data", new Schema<>().nullable(true).example(null));

			operation.getResponses().addApiResponse("403", new ApiResponse()
				.description("Forbidden")
				.content(new Content().addMediaType(
					APPLICATION_JSON,
					new MediaType().schema(forbiddenSchema)
				)));

			return operation;
		};
	}
}