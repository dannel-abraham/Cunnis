package cu.dandroid.cunnis.data.model;

public enum ExitReason {
    NATURAL_DEATH("natural_death"),
    DISEASE("disease"),
    SOLD("sold"),
    SLAUGHTERED("slaughtered"),
    ACCIDENT("accident"),
    OTHER("other");

    private final String value;

    ExitReason(String value) { this.value = value; }

    public String getValue() { return value; }
}
