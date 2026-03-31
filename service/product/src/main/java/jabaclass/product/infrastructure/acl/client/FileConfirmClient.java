package jabaclass.product.infrastructure.acl.client;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.common.exception.CommonErrorCode;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileConfirmClient {

    private final RestTemplate restTemplate;

    @Value("${internal.file.url}")
    private String fileModuleUrl;

    @Value("${internal.api.key}")
    private String internalApiKey;

    public List<FileConfirmResponse> confirmBulk(List<UUID> fileIds) {
        String url = fileModuleUrl + "/api/internal/files/confirm/bulk";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-INTERNAL-KEY", internalApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<UUID>> entity = new HttpEntity<>(fileIds, headers);

        try {
            ResponseEntity<FileConfirmBulkApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    FileConfirmBulkApiResponse.class
            );

            FileConfirmBulkApiResponse body = response.getBody();
            if (body == null || body.data() == null) {
                throw new BusinessException(CommonErrorCode.FILE_CONFIRM_FAIL);
            }
            return body.data();

        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessException(CommonErrorCode.FILE_NOT_FOUND);
        } catch (HttpClientErrorException e) {
            throw new BusinessException(CommonErrorCode.FILE_CONFIRM_FAIL);
        } catch (ResourceAccessException e) {
            throw new BusinessException(CommonErrorCode.FILE_MODULE_UNAVAILABLE);
        }
    }
}
