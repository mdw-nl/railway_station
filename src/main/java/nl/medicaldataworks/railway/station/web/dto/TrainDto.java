package nl.medicaldataworks.railway.station.web.dto;

import nl.medicaldataworks.railway.station.domain.CalculationStatus;

public class TrainDto {

    private Long id;
    private String name;
    private String dockerImageUrl;
    private String ownerName;
    private CalculationStatus calculationStatus;
}
