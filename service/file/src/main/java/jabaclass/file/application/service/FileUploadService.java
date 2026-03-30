package jabaclass.file.application.service;

import jabaclass.file.application.usecase.CompleteUploadUseCase;
import jabaclass.file.application.usecase.RequestUploadUseCase;
import jabaclass.file.application.usecase.ValidateFileUseCase;
import jabaclass.file.common.exception.FileErrorCode;
import jabaclass.file.common.exception.FileException;
import jabaclass.file.domain.model.File;
import jabaclass.file.domain.model.status.FileStatus;
import jabaclass.file.domain.repository.FileRepository;
import jabaclass.file.infrastructure.s3.S3Uploader;
import jabaclass.file.presentation.dto.request.UploadRequestDto;
import jabaclass.file.presentation.dto.response.UploadResponseDto;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class FileUploadService implements RequestUploadUseCase, CompleteUploadUseCase, ValidateFileUseCase {

    private final FileRepository fileRepository;
    private final S3Uploader s3Uploader;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UploadResponseDto requestUpload(UUID userId, UploadRequestDto request) {

        UUID fileId = UUID.randomUUID();
        String storagePath = userId + "/" + fileId + "/" + request.getOriginalName();

        File file = File.builder()
                .id(fileId)
                .userId(userId)
                .originalName(request.getOriginalName())
                .storagePath(storagePath)
                .build();

        fileRepository.save(file);

        String uploadUrl = s3Uploader.generatePresignedUrl(storagePath);

        return new UploadResponseDto(file.getId(), uploadUrl, storagePath);
    }

    @Override
    @Transactional
    public void completeUpload(UUID fileId) {

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));

        if (file.getStatus() != FileStatus.PENDING) {
            throw new FileException(FileErrorCode.FILE_ALREADY_CONFIRMED);
        }

        if (!s3Uploader.existsInS3(file.getStoragePath())) {
            file.confirmFail();
            throw new FileException(FileErrorCode.FILE_NOT_UPLOADED);
        }

        file.confirmSuccess();
    }

    @Override
    @Transactional
    public void validateAndConfirm(UUID fileId) {

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));

        if (file.getStatus() == FileStatus.SUCCESS) {
            return;
        }

        if (file.getStatus() == FileStatus.PENDING) {
            if (!s3Uploader.existsInS3(file.getStoragePath())) {
                throw new FileException(FileErrorCode.FILE_NOT_UPLOADED);
            }
            file.confirmSuccess();
            return;
        }

        throw new FileException(FileErrorCode.FILE_NOT_UPLOADED);
    }

    // 24시간 이상 PENDING 상태 파일 정리 (매일 새벽 3시)
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupPendingFiles() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        try (Stream<File> files = fileRepository.streamByStatusAndCreatedAtBefore(FileStatus.PENDING, threshold)) {
            files.forEach(file -> {
                file.confirmFail();  // DB 상태 변경
                eventPublisher.publishEvent(new FileCleanupEvent(file.getStoragePath()));
            });
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFileCleanup(FileCleanupEvent event) {
        s3Uploader.deleteObject(event.storagePath());
    }
}
