package nl.medicaldataworks.railway.station.web.dto;

import lombok.Data;
import nl.medicaldataworks.railway.station.domain.CalculationStatus;

@Data
public class TrainDto {
    private Long id;
    private String name;
    private String dockerImageUrl;
    private CalculationStatus calculationStatus;
    private Long currentIteration;
    private Long clientTaskCount;
}
