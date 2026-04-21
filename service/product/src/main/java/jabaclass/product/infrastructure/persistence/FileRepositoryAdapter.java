package jabaclass.product.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import jabaclass.product.domain.model.status.FileStatus;
import jabaclass.product.domain.model.File;
import jabaclass.product.domain.repository.FileRepository;

@Repository
@RequiredArgsConstructor
public class FileRepositoryAdapter implements FileRepository {

    private final FileJpaRepository fileJpaRepository;

    @Override
    public File save(File file) {
        return fileJpaRepository.save(file);
    }

    @Override
    public Optional<File> findById(UUID fileId) {
        return fileJpaRepository.findById(fileId);
    }

    @Override
    public List<File> findByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime threshold) {
        return fileJpaRepository.findByStatusAndCreatedAtBefore(status, threshold);
    }

    @Override
    public Stream<File> streamByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime threshold) {
        return fileJpaRepository.streamByStatusAndCreatedAtBefore(status, threshold);
    }
}
