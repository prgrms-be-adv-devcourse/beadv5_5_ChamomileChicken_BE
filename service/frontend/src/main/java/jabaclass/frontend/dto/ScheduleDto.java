package jabaclass.frontend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class ScheduleDto {
    private UUID id;
    private UUID productId;
    private LocalDate scheduleDt;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private int maxCapacity;
}