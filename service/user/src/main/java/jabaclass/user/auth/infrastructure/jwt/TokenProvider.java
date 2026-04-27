package jabaclass.user.auth.infrastructure.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jabaclass.user.auth.application.exception.AuthErrorCode;
import jabaclass.user.auth.application.exception.AuthException;
import jabaclass.user.user.domain.model.UserRole;

@Component
public class TokenProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";

    private final Key key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public TokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity
    ) {
        if (secret.length() < 32) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    public String generateAccessToken(UUID userId, UserRole role) {
        return generate(userId, role.name(), accessTokenValidity, TokenType.ACCESS);
    }

    public String generateRefreshToken(UUID userId, UserRole role) {
        return generate(userId, role.name(),  refreshTokenValidity, TokenType.REFRESH);
    }

    private String generate(UUID userId, String role, long validity, TokenType tokenType) {
        Date now = new Date();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, userId.toString())
                .claim(CLAIM_TYPE, tokenType.name())
                .claim(CLAIM_ROLE, role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validity))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
