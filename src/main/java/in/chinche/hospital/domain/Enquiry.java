package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

/** Contact-form / callback-request submission. */
@Entity
@Table(name = "enquiry", indexes = {
        @Index(name = "idx_enquiry_status", columnList = "status"),
        @Index(name = "idx_enquiry_created", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Enquiry extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 30)
    private String phone;

    @Column(length = 160)
    private String email;

    /** GENERAL | CALLBACK | INTERNATIONAL | FEEDBACK | COMPLAINT | CAREER */
    @Column(name = "enquiry_type", length = 30)
    private String enquiryType = "GENERAL";

    @Column(length = 200)
    private String subject;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    /** NEW | IN_PROGRESS | CLOSED */
    @Column(length = 20, nullable = false)
    private String status = "NEW";

    @Column(name = "assigned_to", length = 120)
    private String assignedTo;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(name = "source_page", length = 200)
    private String sourcePage;
}
