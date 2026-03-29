package jabaclass.file.infrastructure.persistence;

import jabaclass.file.domain.model.File;
import jabaclass.file.domain.model.status.FileStatus;
import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

public interface FileJpaRepository extends JpaRepository<File, UUID> {

    List<File> findByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime threshold);

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE, value = "1000"))
    Stream<File> streamByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime threshold);
}