package jabaclass.product.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import jakarta.persistence.QueryHint;

import jabaclass.product.domain.model.File;
import jabaclass.product.domain.model.status.FileStatus;

public interface FileJpaRepository extends JpaRepository<File, UUID> {

    List<File> findByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime threshold);

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE, value = "1000"))
    Stream<File> streamByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime threshold);
}
