package jabaclass.file.domain.model;

import jabaclass.file.common.model.BaseEntity;
import jabaclass.file.domain.model.status.FileStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File extends BaseEntity {

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

    @Builder
    public File(UUID userId, String originalName, String storagePath, Long size) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.originalName = originalName;
        this.storagePath = storagePath;
        this.size = size;
        this.status = FileStatus.PENDING;
    }

    public void confirmSuccess() {
        this.status = FileStatus.SUCCESS;
    }

    public void confirmFail() {
        this.status = FileStatus.FAIL;
    }
}
