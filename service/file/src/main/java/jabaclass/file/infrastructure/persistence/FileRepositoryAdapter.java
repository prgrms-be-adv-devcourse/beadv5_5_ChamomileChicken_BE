package jabaclass.file.infrastructure.persistence;

import jabaclass.file.domain.model.File;
import jabaclass.file.domain.model.status.FileStatus;
import jabaclass.file.domain.repository.FileRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}