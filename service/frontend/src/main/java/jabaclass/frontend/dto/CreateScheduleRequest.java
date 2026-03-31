package jabaclass.frontend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateScheduleRequest {
    private String scheduleDt;
    private String startTime;
    private String endTime;
    private String status = "AVAILABLE";
}
