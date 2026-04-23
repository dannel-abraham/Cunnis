package cu.dandroid.cunnis.data.model;

public enum ExpenseCategory {
    FEED("feed"),
    MEDICINE("medicine"),
    EQUIPMENT("equipment"),
    VETERINARY("veterinary"),
    TRANSPORT("transport"),
    OTHER("other");

    private final String value;

    ExpenseCategory(String value) { this.value = value; }

    public String getValue() { return value; }
}
