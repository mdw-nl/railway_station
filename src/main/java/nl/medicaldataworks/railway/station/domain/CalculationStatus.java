package nl.medicaldataworks.railway.station.domain;

import java.util.HashMap;
import java.util.Map;

public enum CalculationStatus {
    ERRORED(0),
    REQUESTED(1),
    IDLE(2),
    PROCESSING(3),
    COMPLETED(4);

    private final Integer id;
    private static Map map = new HashMap<>();

    CalculationStatus(final Integer id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return Integer.toString(id);
    }

    static {
        for (CalculationStatus calculationStatus : CalculationStatus.values()) {
            map.put(calculationStatus.id, calculationStatus);
        }
    }

    public static CalculationStatus valueOf(int calculationStatus) {
        return (CalculationStatus) map.get(calculationStatus);
    }

    public int getValue() {
        return id;
    }
}