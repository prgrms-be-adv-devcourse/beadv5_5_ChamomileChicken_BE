package jabastore.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

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
    // 예외처리는 exception 만들면서 수정할 계획
    public String reissueAccessToken(String refreshToken) {
        Claims claims = parseClaims(refreshToken);
        if (!TokenType.REFRESH.name().equals(claims.get(CLAIM_TYPE))) {
            throw new IllegalArgumentException("refresh token이 아닙니다.");
        }
        UUID userId = UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
        return generateAccessToken(userId);
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).get(CLAIM_USER_ID, String.class));
    }

    public Date getIssuedAt(String token) {
        return parseClaims(token).getIssuedAt();
    }

    public boolean isAccessToken(String token) {
        return TokenType.ACCESS.name().equals(parseClaims(token).get(CLAIM_TYPE));
    }

    public boolean isRefreshToken(String token) {
        return TokenType.REFRESH.name().equals(parseClaims(token).get(CLAIM_TYPE));
    }

    // 예외처리는 exception 만들면서 수정할 계획
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
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