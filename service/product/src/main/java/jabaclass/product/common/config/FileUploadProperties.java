package jabaclass.product.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "file.upload")
@Getter
@Setter
public class FileUploadProperties {
	private List<String> allowedContentTypes;
	private long maxFileSize;
}
