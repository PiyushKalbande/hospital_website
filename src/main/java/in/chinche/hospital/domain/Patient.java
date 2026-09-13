package in.chinche.hospital.domain;

import in.chinche.hospital.domain.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * A person receiving care. Phone is the natural key for walk-in/tele flows, so a
 * unique index on (phone) de-duplicates repeat visitors while keeping the record
 * addressable from any channel.
 */
@Entity
@Table(name = "patient", indexes = {
        @Index(name = "idx_patient_phone", columnList = "phone", unique = true),
        @Index(name = "idx_patient_email", columnList = "email")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Patient extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(length = 30, nullable = false)
    private String phone;

    @Column(length = 160)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 10)
    private Integer age;

    @Column(length = 300)
    private String address;

    @Column(length = 120)
    private String city;

    @Column(length = 20)
    private String pincode;

    /** Consent to receive WhatsApp/SMS reminders about the appointment. */
    @Column(name = "consent_sms")
    private boolean consentSms = true;

    @Column(name = "consent_email")
    private boolean consentEmail = true;

    @Column(length = 400)
    private String allergies;

    @Column(name = "medical_notes", columnDefinition = "TEXT")
    private String medicalNotes;
}
