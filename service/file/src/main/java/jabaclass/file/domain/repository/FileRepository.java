package jabaclass.file.domain.repository;

import jabaclass.file.domain.model.File;
import jabaclass.file.domain.model.status.FileStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository {

    File save(File file);

    Optional<File> findById(UUID fileId);

    List<File> findByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime threshold);
}