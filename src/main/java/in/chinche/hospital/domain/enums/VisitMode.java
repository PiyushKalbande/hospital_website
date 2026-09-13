package in.chinche.hospital.domain.enums;

/** Where the consultation physically happens. */
public enum VisitMode {
    IN_HOSPITAL("At hospital", "रुग्णालयात"),
    VIDEO("Video call", "व्हिडिओ कॉल"),
    HOME_VISIT("Home visit", "घरी भेट");

    private final String label;
    private final String labelMr;

    VisitMode(String label, String labelMr) {
        this.label = label;
        this.labelMr = labelMr;
    }

    public String getLabel() { return label; }
    public String getLabelMr() { return labelMr; }
}
