package jabaclass.file.presentation.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UploadResponseDto {

    private UUID fileId;
    private String uploadUrl;
    private String storagePath;
}
