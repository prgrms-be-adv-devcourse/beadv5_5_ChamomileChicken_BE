package jabaclass.apigateway.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.List;

@Getter
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    private final List<String> allowedOrigins;

    @ConstructorBinding
    public CorsProperties(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
