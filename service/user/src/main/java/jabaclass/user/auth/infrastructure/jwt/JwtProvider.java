package jabaclass.user.auth.infrastructure.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.UUID;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jabaclass.user.auth.application.exception.AuthErrorCode;
import jabaclass.user.auth.application.exception.AuthException;

@Component
public class JwtProvider {

	private static final String CLAIM_USER_ID = "userId";
	private static final String CLAIM_TYPE = "type";
	private static final String CLAIM_ROLE = "role";

	private final Key key;

	public JwtProvider(@Value("${jwt.secret}") String secret) {
		if (secret.length() < 32) {
			throw new AuthException(AuthErrorCode.INVALID_TOKEN);
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public Claims parseClaims(String token) {
		try {
			return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody();
		} catch (JwtException | IllegalArgumentException e) {
			throw new AuthException(AuthErrorCode.INVALID_TOKEN);
		}
	}

	public UUID getUserId(Claims claims) {
		return UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
	}

	public boolean isRefreshToken(Claims claims) {
		return TokenType.REFRESH.name().equals(claims.get(CLAIM_TYPE, String.class));
	}

	public String getRole(Claims claims) {
		return claims.get(CLAIM_ROLE, String.class);
	}
}
