package jabaclass.file.presentation.dto.response;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FileConfirmResponse {

    private final UUID fileId;

    private final String storagePath;
}
