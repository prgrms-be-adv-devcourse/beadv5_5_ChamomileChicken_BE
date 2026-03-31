package jabaclass.frontend.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.frontend.client.FileServiceClient;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileProxyController {

	private final FileServiceClient fileServiceClient;

	@PostMapping("/upload-request")
	public ResponseEntity<Map> requestUpload(@RequestBody Map<String, String> body, HttpSession session) {
		String accessToken = (String)session.getAttribute("accessToken");
		Map data = fileServiceClient.requestUpload(body.get("originalName"), accessToken);
		return ResponseEntity.ok(data);
	}

	@PatchMapping("/{fileId}/complete")
	public ResponseEntity<Void> completeUpload(@PathVariable UUID fileId, HttpSession session) {
		String accessToken = (String)session.getAttribute("accessToken");
		fileServiceClient.completeUpload(fileId, accessToken);
		return ResponseEntity.ok().build();
	}
}
