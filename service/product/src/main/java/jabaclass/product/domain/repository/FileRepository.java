package jabaclass.product.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.QueryHints;

import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;

import jabaclass.product.domain.model.File;
import jakarta.persistence.QueryHint;

import jabaclass.product.domain.model.status.FileStatus;

public interface FileRepository {

    File save(File file);

    Optional<File> findById(UUID fileId);

    List<File> findByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime threshold);

    @QueryHints(@QueryHint(name = HINT_FETCH_SIZE, value = "1000"))
    Stream<File> streamByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime threshold);
}
