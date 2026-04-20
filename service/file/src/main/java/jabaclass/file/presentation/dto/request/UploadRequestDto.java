package jabaclass.file.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UploadRequestDto {

    @NotBlank(message = "파일명을 입력해주세요")
    private String originalName;
}
