package jabaclass.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jabaclass.auth.exception.JwtAuthException;
import jabaclass.auth.exception.JwtErrorCode;

public class JwtProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_TYPE = "type";

    private final Key key;

    public JwtProvider(JwtProperties properties) {
        if (properties.getSecret().length() < 32) {
            throw new IllegalArgumentException("JWT secret은 최소 32자 이상이어야 합니다.");
        }
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    // 외부에서 token String만 있을 때 사용
    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).get(CLAIM_USER_ID, String.class));
    }

    // filter에서 parseClaims 중복 호출 방지용
    public UUID getUserId(Claims claims) {
        return UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
    }

    // lastLogoutAt 기능 도입시 필요
    public Date getIssuedAt(String token) {
        return parseClaims(token).getIssuedAt();
    }

    // 외부에서 token String만 있을 때 사용
    public boolean isAccessToken(String token) {
        return TokenType.ACCESS.name().equals(
                parseClaims(token).get(CLAIM_TYPE, String.class)
        );
    }

    public boolean isRefreshToken(Claims claims) {
        return TokenType.REFRESH.name().equals(claims.get(CLAIM_TYPE, String.class));
    }

    // filter에서 parseClaims 중복 호출 방지용
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