package jabaclass.user.auth.infrastructure.oauth2;

import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

public record OAuth2AuthorizationRequestDto(
	String authorizationUri,
	String clientId,
	String redirectUri,
	Set<String> scopes,
	String state,
	Map<String, Object> additionalParameters,
	Map<String, Object> attributes
) {
	public static OAuth2AuthorizationRequestDto from(
		org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest request) {
		return new OAuth2AuthorizationRequestDto(
			request.getAuthorizationUri(),
			request.getClientId(),
			request.getRedirectUri(),
			request.getScopes(),
			request.getState(),
			request.getAdditionalParameters(),
			request.getAttributes()
		);
	}

	public OAuth2AuthorizationRequest toRequest() {
		return OAuth2AuthorizationRequest
			.authorizationCode()
			.authorizationUri(authorizationUri)
			.clientId(clientId)
			.redirectUri(redirectUri)
			.scopes(scopes)
			.state(state)
			.additionalParameters(additionalParameters)
			.attributes(attributes)
			.build();
	}
}
