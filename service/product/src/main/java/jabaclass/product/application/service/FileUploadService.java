package jabaclass.product.application.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import jabaclass.product.common.config.FileUploadProperties;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import jabaclass.product.application.usecase.CompleteUploadUseCase;
import jabaclass.product.application.usecase.RequestUploadUseCase;
import jabaclass.product.application.usecase.ValidateFileUseCase;
import jabaclass.product.application.exception.FileException;
import jabaclass.product.domain.repository.FileRepository;
import jabaclass.product.domain.model.File;
import jabaclass.product.application.exception.FileErrorCode;
import jabaclass.product.domain.model.status.FileStatus;
import jabaclass.product.infrastructure.s3.S3Uploader;
import jabaclass.product.presentation.dto.request.UploadRequestDto;
import jabaclass.product.application.dto.FileConfirmResponse;
import jabaclass.product.presentation.dto.response.UploadResponseDto;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@Service
@RequiredArgsConstructor
public class FileUploadService implements RequestUploadUseCase, CompleteUploadUseCase, ValidateFileUseCase {

    private final FileUploadProperties fileUploadProperties;
    private final FileRepository fileRepository;
    private final S3Uploader s3Uploader;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UploadResponseDto requestUpload(UUID userId, UploadRequestDto request) {

        if (!fileUploadProperties.getAllowedContentTypes().contains(request.getContentType())) {
            throw new FileException(FileErrorCode.FILE_INVALID_TYPE);
        }

        UUID fileId = UUID.randomUUID();
        String storagePath = userId + "/" + fileId + "/" + request.getOriginalName();

        File file = File.builder()
                .id(fileId)
                .userId(userId)
                .originalName(request.getOriginalName())
                .storagePath(storagePath)
                .build();

        fileRepository.save(file);

        String uploadUrl = s3Uploader.generatePresignedUrl(storagePath, request.getContentType());

        return new UploadResponseDto(file.getId(), uploadUrl, storagePath);
    }

    @Override
    @Transactional(noRollbackFor = FileException.class)
    public void completeUpload(UUID fileId) {

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));

        if (file.getStatus() != FileStatus.PENDING) {
            throw new FileException(FileErrorCode.FILE_ALREADY_CONFIRMED);
        }

        HeadObjectResponse metadata = fetchMetadataOrFail(file);
        validateMetadata(file, metadata);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = FileException.class)
    public FileConfirmResponse validateAndConfirm(UUID fileId) {

        File file = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));

        if (file.getStatus() == FileStatus.SUCCESS) {
            return new FileConfirmResponse(file.getId(), file.getStoragePath());
        }

        if (file.getStatus() == FileStatus.PENDING) {
            HeadObjectResponse metadata = fetchMetadataOrFail(file);
            validateMetadata(file, metadata);
            return new FileConfirmResponse(file.getId(), file.getStoragePath());
        }

        throw new FileException(FileErrorCode.FILE_NOT_UPLOADED);
    }

    private HeadObjectResponse fetchMetadataOrFail(File file) {
        return s3Uploader.getMetadata(file.getStoragePath())
            .orElseThrow(() -> {
                file.confirmFail();
                return new FileException(FileErrorCode.FILE_NOT_UPLOADED);
            });
    }

    private void validateMetadata(File file, HeadObjectResponse metadata) {
        if (!fileUploadProperties.getAllowedContentTypes().contains(metadata.contentType())) {
            s3Uploader.deleteObject(file.getStoragePath());
            file.confirmFail();
            throw new FileException(FileErrorCode.FILE_INVALID_TYPE);
        }
        if (metadata.contentLength() > fileUploadProperties.getMaxFileSize()) {
            s3Uploader.deleteObject(file.getStoragePath());
            file.confirmFail();
            throw new FileException(FileErrorCode.FILE_SIZE_EXCEEDED);
        }
        file.confirmSuccess(metadata.contentLength());
    }

    // 24시간 이상 PENDING 상태 파일 정리 (매일 새벽 3시)
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupPendingFiles() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        try (Stream<File> files = fileRepository.streamByStatusAndCreatedAtBefore(FileStatus.PENDING, threshold)) {
            files.forEach(file -> {
                file.confirmFail();
                eventPublisher.publishEvent(new FileCleanupEvent(file.getStoragePath()));
            });
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFileCleanup(FileCleanupEvent event) {
        s3Uploader.deleteObject(event.storagePath());
    }
}
