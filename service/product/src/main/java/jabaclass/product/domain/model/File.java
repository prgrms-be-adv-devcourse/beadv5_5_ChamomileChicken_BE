package jabaclass.product.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.EntityListeners;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jabaclass.product.domain.model.status.FileStatus;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File {

    @Id
    @Column(name = "id", nullable = false, length = 50)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 50)
    private UUID userId;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "size")
    private Long size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public File(UUID id, UUID userId, String originalName, String storagePath, Long size) {
        this.id = id;
        this.userId = userId;
        this.originalName = originalName;
        this.storagePath = storagePath;
        this.size = size;
        this.status = FileStatus.PENDING;
    }

    public void confirmSuccess(long size) {
        this.status = FileStatus.SUCCESS;
        this.size = size;
    }

    public void confirmFail() {
        this.status = FileStatus.FAIL;
    }
}
