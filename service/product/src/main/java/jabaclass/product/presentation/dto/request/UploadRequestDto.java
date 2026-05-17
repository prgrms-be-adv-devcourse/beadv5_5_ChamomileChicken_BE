package jabaclass.product.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UploadRequestDto {

    @NotBlank(message = "파일명을 입력해주세요")
    private String originalName;

    @NotBlank(message = "파일 형식을 입력해주세요")
    private String contentType;
}
