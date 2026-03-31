package jabaclass.frontend.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FileServiceClient {

	private final RestTemplate restTemplate;

	@Value("${services.file-url}")
	private String fileUrl;

	@Value("${internal.api.key}")
	private String internalApiKey;

	public Map requestUpload(String originalName, String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.set("Content-Type", "application/json");
		HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("originalName", originalName), headers);
		Map response = restTemplate.postForObject(fileUrl + "/api/v1/files/upload-request", entity, Map.class);
		return (Map)response.get("data");
	}

	// storagePath 목록 → presigned GET URL 맵 반환
	public Map<String, String> getPresignedGetUrls(List<String> storagePaths) {
		if (storagePaths == null || storagePaths.isEmpty()) return Map.of();
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-INTERNAL-KEY", internalApiKey);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<List<String>> entity = new HttpEntity<>(storagePaths, headers);
		Map response = restTemplate.postForObject(
			fileUrl + "/api/internal/files/presigned-urls", entity, Map.class);
		List<Map<String, String>> data = (List<Map<String, String>>) response.get("data");
		Map<String, String> result = new HashMap<>();
		if (data != null) {
			data.forEach(item -> result.put(item.get("storagePath"), item.get("presignedUrl")));
		}
		return result;
	}

	public void completeUpload(UUID fileId, String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		HttpEntity<Void> entity = new HttpEntity<>(headers);
		restTemplate.exchange(
			fileUrl + "/api/v1/files/" + fileId + "/complete",
			HttpMethod.PATCH,
			entity,
			Map.class
		);
	}
}
