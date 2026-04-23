package cu.dandroid.cunnis.data.model;

public enum RabbitStatus {
    ACTIVE("active"),
    DEAD("dead"),
    SOLD("sold"),
    SLAUGHTERED("slaughtered"),
    TRANSFERRED("transferred");

    private final String value;

    RabbitStatus(String value) { this.value = value; }

    public String getValue() { return value; }
}
