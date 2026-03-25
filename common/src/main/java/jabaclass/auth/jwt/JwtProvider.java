package jabaclass.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jabaclass.auth.exception.JwtAuthException;
import jabaclass.auth.exception.JwtErrorCode;

public class JwtProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_TYPE = "type";

    private final Key key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = properties.getAccessTokenValidity();
        this.refreshTokenValidity = properties.getRefreshTokenValidity();
    }

    public String generateAccessToken(UUID userId) {
        return generate(userId, accessTokenValidity, TokenType.ACCESS);
    }

    public String generateRefreshToken(UUID userId) {
        return generate(userId, refreshTokenValidity, TokenType.REFRESH);
    }

    // DB 일치 여부 확인은 user-service AuthService 책임
    public String reissueAccessToken(String refreshToken) {
        Claims claims = parseClaims(refreshToken);
        if (!TokenType.REFRESH.name().equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtAuthException(JwtErrorCode.INVALID_TOKEN);
        }
        UUID userId = UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
        return generateAccessToken(userId);
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).get(CLAIM_USER_ID, String.class));
    }

    public UUID getUserId(Claims claims) {
        return UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
    }

    public Date getIssuedAt(String token) {
        return parseClaims(token).getIssuedAt();
    }

    public boolean isAccessToken(String token) {
        return TokenType.ACCESS.name().equals(
                parseClaims(token).get(CLAIM_TYPE, String.class)
        );
    }

    public boolean isAccessToken(Claims claims) {
        return TokenType.ACCESS.name().equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isRefreshToken(String token) {
        return TokenType.REFRESH.name().equals(
                parseClaims(token).get(CLAIM_TYPE, String.class)
        );
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