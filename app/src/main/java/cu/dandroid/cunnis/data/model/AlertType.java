package cu.dandroid.cunnis.data.model;

public enum AlertType {
    ESTRUS_PREDICTION("estrus_prediction"),
    GESTATION_DUE("gestation_due"),
    WEANING("weaning"),
    REPRODUCTIVE_AGE("reproductive_age"),
    WEIGHT_CHECK("weight_check"),
    VACCINATION_DUE("vaccination_due"),
    CUSTOM("custom");

    private final String value;

    AlertType(String value) { this.value = value; }

    public String getValue() { return value; }
}
