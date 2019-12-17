package nl.medicaldataworks.railway.station.web.dto;

import nl.medicaldataworks.railway.station.domain.CalculationStatus;

import java.util.Date;

public class TaskDto {
    private Long id;
    private Date creationTimestamp;
    private TrainDto trainDto;
    private CalculationStatus calculationStatus;
    private String result;
    private String clientId;
    private String ownerName;
}
