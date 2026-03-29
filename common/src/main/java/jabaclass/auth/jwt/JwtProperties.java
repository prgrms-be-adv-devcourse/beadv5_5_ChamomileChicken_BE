package jabaclass.auth.jwt;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@Getter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private final String secret;

    @ConstructorBinding
    public JwtProperties(String secret) {
        this.secret = secret;
    }
}