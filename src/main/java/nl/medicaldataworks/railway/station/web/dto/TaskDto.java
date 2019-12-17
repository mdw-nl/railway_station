package nl.medicaldataworks.railway.station.web.dto;

import lombok.Data;
import nl.medicaldataworks.railway.station.domain.CalculationStatus;

@Data
public class TaskDto {
    private Long id;
    private CalculationStatus calculationStatus;
    private String result;
    private Long station;
    private Long train;
}
