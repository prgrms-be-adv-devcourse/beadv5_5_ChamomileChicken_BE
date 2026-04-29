package jabaclass.product.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * @Scheduled를 쓰려면 스케줄링 기능을 켜는 설정
 * */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
}
