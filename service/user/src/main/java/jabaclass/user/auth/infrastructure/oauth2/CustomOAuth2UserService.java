package jabaclass.user.auth.infrastructure.oauth2;

import jabaclass.user.user.domain.model.UserRole;
import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserRepository userRepository;

	@Override
	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(request);

		String registrationId = request.getClientRegistration().getRegistrationId();
		OAuthAttributes attributes = OAuthAttributes.of(registrationId, oAuth2User.getAttributes());

		User user = findOrRegister(attributes);

		return new CustomOAuth2User(user, oAuth2User.getAttributes());
	}

	private User findOrRegister(OAuthAttributes attributes) {
		return userRepository.findBySocialTypeAndSocialId(attributes.getSocialType(), attributes.getSocialId())
			.orElseGet(() -> register(attributes));
	}

	private User register(OAuthAttributes attributes) {
		userRepository.findByEmail(attributes.getEmail())
			.ifPresent(u -> {
				throw new OAuth2AuthenticationException(
					new OAuth2Error("INVALID_ERROR", "가입할 수 없는 이메일입니다. 다른 이메일을 사용해주세요.",
						null)
				);
			});

		return userRepository.save(
			User.builder()
				.name(attributes.getName())
				.email(attributes.getEmail())
				.socialType(attributes.getSocialType())
				.socialId(attributes.getSocialId())
				.role(UserRole.USER)
				.build()
		);
	}
}
