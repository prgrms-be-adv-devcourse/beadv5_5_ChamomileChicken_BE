package jabaclass.user.auth.infrastructure.oauth2;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

import jabaclass.user.user.domain.model.SocialType;

@Getter
@Builder
public class OAuthAttributes {

	private String name;
	private String email;
	private String socialId;
	private SocialType socialType;

	public static OAuthAttributes of(String registrationId, Map<String, Object> attributes) {
		return switch (registrationId) {
			case "google" -> ofGoogle(attributes);
			case "kakao"  -> ofKakao(attributes);
			case "naver"  -> ofNaver(attributes);
			default -> throw new IllegalArgumentException("지원하지 않는 provider: " + registrationId);
		};
	}

	private static OAuthAttributes ofGoogle(Map<String, Object> attributes) {
		return OAuthAttributes.builder()
			.name((String) attributes.get("name"))
			.email((String) attributes.get("email"))
			.socialId((String) attributes.get("sub"))
			.socialType(SocialType.GOOGLE)
			.build();
	}

	@SuppressWarnings("unchecked")
	private static OAuthAttributes ofKakao(Map<String, Object> attributes) {
		Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
		Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

		return OAuthAttributes.builder()
			.name((String)profile.get("nickname"))
			.email((String)kakaoAccount.get("email"))
			.socialId((String.valueOf(attributes.get("id"))))
			.socialType(SocialType.KAKAO)
			.build();
	}

	@SuppressWarnings("unchecked")
	private static OAuthAttributes ofNaver(Map<String, Object> attributes) {
		Map<String, Object> response = (Map<String, Object>)attributes.get("response");

		return OAuthAttributes.builder()
			.name((String)response.get("name"))
			.email((String)response.get("email"))
			.socialId((String)response.get("id"))
			.socialType(SocialType.NAVER)
			.build();
	}
}
