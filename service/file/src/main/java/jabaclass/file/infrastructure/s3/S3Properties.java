package jabaclass.file.infrastructure.s3;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@Getter
@ConfigurationProperties(prefix = "spring.cloud.aws.s3")
public class S3Properties {
    private final String bucket;
    private final long presignedUrlExpiration;

    @ConstructorBinding
    public S3Properties(String bucket, Long presignedUrlExpiration) {
        this.bucket = bucket;
        this.presignedUrlExpiration = presignedUrlExpiration;
    }
}
