package jabaclass.file.infrastructure.persistence;

import jabaclass.file.domain.model.File;
import jabaclass.file.domain.model.status.FileStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileJpaRepository extends JpaRepository<File, UUID> {

    List<File> findByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime threshold);
}