package nl.medicaldataworks.railway.station.domain;

public enum CalculationStatus {
    COMPLETED("COMPLETED"),
    ERRORED("ERRORED"),
    PROCESSING("PROCESSING"),
    IDLE("IDLE"),
    REQUESTED("REQUESTED");

    private final String text;

    CalculationStatus(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}