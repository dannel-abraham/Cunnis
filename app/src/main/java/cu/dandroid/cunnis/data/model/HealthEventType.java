package cu.dandroid.cunnis.data.model;

public enum HealthEventType {
    CHECKUP("checkup"),
    VACCINATION("vaccination"),
    DEWORMING("deworming"),
    ILLNESS("illness"),
    INJURY("injury"),
    SURGERY("surgery"),
    OTHER("other");

    private final String value;

    HealthEventType(String value) { this.value = value; }

    public String getValue() { return value; }
}
