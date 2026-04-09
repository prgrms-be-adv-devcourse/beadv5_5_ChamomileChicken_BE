package jabaclass.apigateway.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.UUID;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;

import jabaclass.apigateway.exception.JwtAuthException;
import jabaclass.apigateway.exception.JwtErrorCode;

public class JwtProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_TYPE = "type";
    private static final int MIN_SECRET_LENGTH = 32;

    private final Key key;

    public JwtProvider(JwtProperties properties) {
        if (properties.getSecret().length() < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException("JWT secret은 최소 \" + MIN_SECRET_LENGTH + \"자 이상이어야 합니다.");
        }
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public UUID getUserId(Claims claims) {
        return UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
    }

    public boolean isAccessToken(Claims claims) {
        return TokenType.ACCESS.name().equals(claims.get(CLAIM_TYPE, String.class));
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new JwtAuthException(JwtErrorCode.EXPIRED_TOKEN);
        } catch (MalformedJwtException e) {
            throw new JwtAuthException(JwtErrorCode.MALFORMED_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new JwtAuthException(JwtErrorCode.UNSUPPORTED_TOKEN);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            throw new JwtAuthException(JwtErrorCode.INVALID_TOKEN);
        }
    }
}
