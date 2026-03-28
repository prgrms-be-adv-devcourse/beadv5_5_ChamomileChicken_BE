package jabaclass.file.domain.repository;

import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;

import jabaclass.file.domain.model.File;
import jabaclass.file.domain.model.status.FileStatus;
import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.QueryHints;

public interface FileRepository {

    File save(File file);

    Optional<File> findById(UUID fileId);

    List<File> findByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime threshold);

    @QueryHints(@QueryHint(name = HINT_FETCH_SIZE, value = "1000"))
    Stream<File> streamByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime threshold);
}