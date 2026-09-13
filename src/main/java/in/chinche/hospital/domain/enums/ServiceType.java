package in.chinche.hospital.domain.enums;

/** The kind of care a visitor is booking through the widget. */
public enum ServiceType {
    OPD_CONSULTATION("OPD Consultation", "ओ.पी.डी. तपासणी", "In-hospital outpatient consultation"),
    TELE_CONSULTATION("Video Consultation", "व्हिडिओ सल्ला", "Consult from home over video call"),
    HEALTH_CHECKUP("Health Check-up", "आरोग्य तपासणी", "Preventive full-body packages"),
    LAB_TEST("Lab Test", "प्रयोगशाळा तपासणी", "Blood, urine, radiology and pathology"),
    SECOND_OPINION("Second Opinion", "दुसरा अभिप्राय", "Expert review of an existing diagnosis"),
    HOME_CARE("Home Care Visit", "घरी भेट", "Nursing, physiotherapy or doctor at home"),
    SURGERY_PLANNING("Surgery Planning", "शस्त्रक्रिया नियोजन", "Pre-surgical evaluation and scheduling");

    private final String label;
    private final String labelMr;
    private final String description;

    ServiceType(String label, String labelMr, String description) {
        this.label = label;
        this.labelMr = labelMr;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getLabelMr() { return labelMr; }
    public String getDescription() { return description; }
}
