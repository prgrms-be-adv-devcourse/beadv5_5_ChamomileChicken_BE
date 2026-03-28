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
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileUploadService implements RequestUploadUseCase, CompleteUploadUseCase, ValidateFileUseCase {

    private final FileRepository fileRepository;
    private final S3Uploader s3Uploader;

    @Override
    @Transactional
    public UploadResponseDto requestUpload(UUID userId, UploadRequestDto request) {

        // 1. storagePath 생성 (userId/fileId/originalName 구조)
        UUID fileId = UUID.randomUUID();
        String storagePath = userId + "/" + fileId + "/" + request.getOriginalName();

        // 2. PENDING 상태로 DB 저장
        File file = File.builder()
                .userId(userId)
                .originalName(request.getOriginalName())
                .storagePath(storagePath)
                .build();

        fileRepository.save(file);

        // 3. Pre-signed URL 생성
        String uploadUrl = s3Uploader.generatePresignedUrl(storagePath);

        return new UploadResponseDto(file.getId(), uploadUrl, storagePath);
    }

    @Override
    @Transactional
    public void completeUpload(UUID fileId) {

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));

        // 이미 처리된 파일이면 예외
        if (file.getStatus() != FileStatus.PENDING) {
            throw new FileException(FileErrorCode.FILE_ALREADY_CONFIRMED);
        }

        // S3 실체 확인
        if (!s3Uploader.doesObjectExist(file.getStoragePath())) {
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

        // SUCCESS 상태면 바로 통과
        if (file.getStatus() == FileStatus.SUCCESS) {
            return;
        }

        // PENDING이면 S3 재확인
        if (file.getStatus() == FileStatus.PENDING) {
            if (!s3Uploader.doesObjectExist(file.getStoragePath())) {
                throw new FileException(FileErrorCode.FILE_NOT_UPLOADED);
            }
            file.confirmSuccess();
            return;
        }

        // FAIL이면 에러
        throw new FileException(FileErrorCode.FILE_NOT_UPLOADED);
    }

    // 24시간 이상 PENDING 상태 파일 정리 (매일 새벽 3시)
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupPendingFiles() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        fileRepository.findByStatusAndCreatedAtBefore(FileStatus.PENDING, threshold)
                .forEach(file -> {
                    s3Uploader.deleteObject(file.getStoragePath());
                    // DB에서는 삭제하지 않고 FAIL로 마킹
                    file.confirmFail();
                });
    }
}