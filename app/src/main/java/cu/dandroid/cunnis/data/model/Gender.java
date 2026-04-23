package cu.dandroid.cunnis.data.model;

public enum Gender {
    MALE("male"),
    FEMALE("female"),
    UNKNOWN("unknown");

    private final String value;

    Gender(String value) { this.value = value; }

    public String getValue() { return value; }
}
