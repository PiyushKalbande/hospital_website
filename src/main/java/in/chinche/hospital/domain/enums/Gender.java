package in.chinche.hospital.domain.enums;

public enum Gender {
    MALE("Male", "पुरुष"),
    FEMALE("Female", "स्त्री"),
    OTHER("Other", "इतर"),
    UNDISCLOSED("Prefer not to say", "सांगू इच्छित नाही");

    private final String label;
    private final String labelMr;

    Gender(String label, String labelMr) {
        this.label = label;
        this.labelMr = labelMr;
    }

    public String getLabel() { return label; }
    public String getLabelMr() { return labelMr; }
}
