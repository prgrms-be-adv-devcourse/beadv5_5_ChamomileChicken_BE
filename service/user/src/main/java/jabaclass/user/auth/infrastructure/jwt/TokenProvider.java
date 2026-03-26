package jabaclass.user.auth.infrastructure.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jabaclass.auth.jwt.TokenType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class TokenProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_TYPE = "type";

    private final Key key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public TokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    public String generateAccessToken(UUID userId) {
        return generate(userId, accessTokenValidity, TokenType.ACCESS);
    }

    public String generateRefreshToken(UUID userId) {
        return generate(userId, refreshTokenValidity, TokenType.REFRESH);
    }

    private String generate(UUID userId, long validity, TokenType tokenType) {
        Date now = new Date();
        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId.toString())
                .claim(CLAIM_TYPE, tokenType.name())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validity))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}