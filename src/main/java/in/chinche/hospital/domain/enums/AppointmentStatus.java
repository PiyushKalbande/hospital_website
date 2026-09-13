package in.chinche.hospital.domain.enums;

/** Lifecycle of a booking. Ordered roughly by progression through the workflow. */
public enum AppointmentStatus {
    PENDING("Pending confirmation", "प्रलंबित"),
    CONFIRMED("Confirmed", "निश्चित"),
    CHECKED_IN("Checked in", "दाखल"),
    COMPLETED("Completed", "पूर्ण"),
    CANCELLED("Cancelled", "रद्द"),
    RESCHEDULED("Rescheduled", "पुन्हा नियोजित"),
    NO_SHOW("No show", "अनुपस्थित");

    private final String label;
    private final String labelMr;

    AppointmentStatus(String label, String labelMr) {
        this.label = label;
        this.labelMr = labelMr;
    }

    public String getLabel() { return label; }
    public String getLabelMr() { return labelMr; }
}
