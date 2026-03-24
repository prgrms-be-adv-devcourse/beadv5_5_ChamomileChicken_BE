package jabaclass.product.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        // 본인 모듈 명으로 바꿔주세요.
                        .title("Product API")
                        .description("Product Swagger API Docs")
                        .version("v1"));
    }
}
