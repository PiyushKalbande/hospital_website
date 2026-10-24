package in.chinche.hospital.service;

import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.domain.Appointment;
import in.chinche.hospital.domain.Enquiry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Outbound notifications. Async and fully failure-tolerant: in development there is
 * usually no SMTP listener, so every send is wrapped and degraded to a log line
 * rather than propagating and breaking the booking transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final JavaMailSender mailSender;
    private final AppProperties props;

    @Value("${chinche.mail.from:dadakalbande111@gmail.com}")
    private String fromAddress;

    @Async("notificationExecutor")
    public void sendBookingConfirmation(Appointment a) {
        String doctorLine = a.getDoctor() == null ? "Care team" : a.getDoctor().getName()
                + (a.getDoctor().getDesignation() == null ? "" : ", " + a.getDoctor().getDesignation());
        String facilityLine = a.getFacility() == null ? props.getContact().getAddressLine1() + ", "
                + props.getContact().getAddressLine2() : a.getFacility().getName() + ", " + a.getFacility().getAddressLine();

        String body = """
                नमस्कार %s,

                तुमची अपॉइंटमेंट यशस्वीरित्या नोंदवली गेली आहे. / Your appointment has been received.

                Booking reference : %s
                Service           : %s (%s)
                Doctor            : %s
                Centre            : %s
                Date              : %s
                Time              : %s – %s
                Status            : %s

                %s

                Need to change something? Call %s or reply to this email quoting your reference number.

                — Chinche Hospital, Katol (चींचे हॉस्पिटल, कटोल)
                """.formatted(
                a.getPatient().getFullName(), a.getReferenceNo(), a.getServiceType().getLabel(),
                a.getServiceType().getLabelMr(), doctorLine, facilityLine,
                a.getAppointmentDate().format(DATE_FMT),
                a.getSlotStart().format(TIME_FMT), a.getSlotEnd().format(TIME_FMT),
                a.getStatus().getLabel(),
                "Please carry a photo ID and any previous reports.",
                props.getContact().getPhone());

        send(a.getPatient().getEmail(), "Appointment " + a.getReferenceNo() + " — Chinche Hospital, Katol", body);
    }

    @Async("notificationExecutor")
    public void sendCancellation(Appointment a) {
        String body = """
                Booking %s has been cancelled as requested.
                If this was a mistake, call %s and we will rebook you at the earliest available slot.

                — Chinche Hospital, Katol
                """.formatted(a.getReferenceNo(), props.getContact().getPhone());
        send(a.getPatient().getEmail(), "Cancelled: booking " + a.getReferenceNo(), body);
    }

    @Async("notificationExecutor")
    public void sendEnquiryAcknowledgement(Enquiry e) {
        String body = """
                नमस्कार %s,

                तुमचा संदेश आम्हाला मिळाला आहे. आमची टीम २४ तासांच्या आत संपर्क साधेल.
                We have received your message and our care team will respond within one working day.

                Subject : %s
                Helpline: %s

                — Chinche Hospital, Katol
                """.formatted(e.getName(), e.getSubject() == null ? e.getEnquiryType() : e.getSubject(),
                props.getContact().getPhone());
        send(e.getEmail(), "We received your message — Chinche Hospital", body);
    }

    private void send(String to, String subject, String body) {
        if (to == null || to.isBlank() || !to.contains("@")) {
            log.debug("Skipped email (no valid address). Subject: {}", subject);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email queued to {} — {}", to, subject);
        } catch (Exception ex) {
            // No SMTP sink in dev, or a transient failure: log and move on.
            log.warn("Email to {} not delivered ({}): {}", to, ex.getClass().getSimpleName(), subject);
        }
    }
}
