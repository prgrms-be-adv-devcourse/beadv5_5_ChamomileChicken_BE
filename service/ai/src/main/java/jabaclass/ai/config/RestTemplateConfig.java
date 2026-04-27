package jabaclass.ai.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

	@Value("${openai.http.connect-timeout:2s}")
	private Duration connectTimeout;

	@Value("${openai.http.read-timeout:8s}")
	private Duration readTimeout;

	@Bean
	public RestTemplate restTemplate() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout((int)connectTimeout.toMillis());
		requestFactory.setReadTimeout((int)readTimeout.toMillis());
		return new RestTemplate(requestFactory);
	}
}
