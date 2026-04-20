package jabaclass.apigateway.config;

import java.util.List;

import lombok.Getter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@Getter
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    private final List<String> allowedOrigins;

    @ConstructorBinding
    public CorsProperties(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
