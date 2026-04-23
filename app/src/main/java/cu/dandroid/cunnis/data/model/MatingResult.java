package cu.dandroid.cunnis.data.model;

public enum MatingResult {
    PENDING("pending"),
    SUCCESSFUL("successful"),
    UNSUCCESSFUL("unsuccessful");

    private final String value;

    MatingResult(String value) { this.value = value; }

    public String getValue() { return value; }
}
