package in.chinche.hospital.domain.enums;

/** Scale of a physical campus in the hospital network. */
public enum FacilityType {
    MAIN_HOSPITAL("Multi-speciality Hospital", "बहुविध रुग्णालय"),
    CLINIC("Clinic", "दवाखाना"),
    DIAGNOSTIC_CENTRE("Diagnostic Centre", "निदान केंद्र"),
    PHARMACY("Pharmacy", "औषधालय");

    private final String label;
    private final String labelMr;

    FacilityType(String label, String labelMr) {
        this.label = label;
        this.labelMr = labelMr;
    }

    public String getLabel() { return label; }
    public String getLabelMr() { return labelMr; }
}
