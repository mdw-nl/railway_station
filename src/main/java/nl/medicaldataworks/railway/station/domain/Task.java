package nl.medicaldataworks.railway.station.domain;

import lombok.Data;

@Data
public class Task {
    private Train train;
    private CalculationStatus calculationStatus;
    private String result;
}
