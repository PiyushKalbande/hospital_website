package in.chinche.hospital.service;

import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.domain.Appointment;
import in.chinche.hospital.domain.Enquiry;
import in.chinche.hospital.domain.enums.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
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
    private final BookingReceiptPdfService receiptPdfService;

    @Value("${chinche.mail.from:dadakalbande111@gmail.com}")
    private String fromAddress;

    @Async("notificationExecutor")
    public void sendBookingConfirmation(Appointment a) {
        sendAppointmentEmail(a, "Booking request received",
            "Your appointment request has been received. The hospital will contact you if confirmation is needed.");
        }

        @Async("notificationExecutor")
        public void sendStatusUpdate(Appointment a) {
        String status = a.getStatus().getLabel();
        sendAppointmentEmail(a, "Appointment " + status.toLowerCase(Locale.ROOT), statusMessage(a.getStatus()));
    }

    @Async("notificationExecutor")
    public void sendCancellation(Appointment a) {
        sendAppointmentEmail(a, "Appointment cancelled",
            "Your booking has been cancelled. Contact the hospital if you need help with another booking.");
    }

    @Async("notificationExecutor")
    public void sendDoctorEmergencyReschedule(Appointment appointment) {
        sendAppointmentEmail(appointment, "Please choose a new appointment time",
                "Due to an unexpected doctor emergency, your original appointment time is no longer available. "
                        + "Please choose another available slot or call the hospital. We apologise for the inconvenience.");
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
            log.warn("Email not delivered for subject '{}' ({}): {}", subject,
                    ex.getClass().getSimpleName(), rootCauseMessage(ex));
        }
    }

    private void sendAppointmentEmail(Appointment a, String subject, String message) {
        String to = a.getPatient().getEmail();
        if (to == null || to.isBlank() || !to.contains("@")) {
            log.debug("Skipped booking email (no valid address). Reference: {}", a.getReferenceNo());
            return;
        }

        byte[] receipt = null;
        try {
            receipt = receiptPdfService.generate(a);
        } catch (Exception ex) {
            log.warn("Could not generate receipt PDF for {} ({}); sending acknowledgement without attachment.",
                    a.getReferenceNo(), ex.getClass().getSimpleName());
        }

        try {
            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject + " — " + a.getReferenceNo() + " | Chinche Hospital");
            helper.setText(plainAppointmentBody(a, message), appointmentHtml(a, subject, message));
            if (receipt != null) {
                helper.addAttachment(a.getReferenceNo() + "-receipt.pdf",
                        new ByteArrayResource(receipt), "application/pdf");
            }
            mailSender.send(mail);
            log.info("Booking email queued to {} — {}", to, a.getReferenceNo());
        } catch (Exception ex) {
            log.warn("Booking email not delivered for reference {} ({}): {}", a.getReferenceNo(),
                ex.getClass().getSimpleName(), rootCauseMessage(ex));
        }
    }

    private String plainAppointmentBody(Appointment a, String message) {
        return "नमस्कार " + a.getPatientNameDisplay() + ",\n\n" + message
                + "\n\nBooking reference: " + a.getReferenceNo()
                + "\nService: " + a.getServiceType().getLabel() + " / " + a.getServiceType().getLabelMr()
                + "\nDoctor: " + doctorName(a)
                + "\nCentre: " + facilityName(a)
                + "\nDate: " + a.getAppointmentDate().format(DATE_FMT)
                + "\nTime: " + a.getSlotStart().format(TIME_FMT) + " - " + a.getSlotEnd().format(TIME_FMT)
                + "\nStatus: " + a.getStatus().getLabel() + " / " + a.getStatus().getLabelMr()
                + "\n\nFor help, call " + contactPhone()
                + " and quote the booking reference.\n\nChinche Hospital, Katol";
    }

    private String appointmentHtml(Appointment a, String subject, String message) {
        StringBuilder rows = new StringBuilder();
        appendRow(rows, "Booking reference", a.getReferenceNo());
        appendRow(rows, "Service", a.getServiceType().getLabel() + " / " + a.getServiceType().getLabelMr());
        appendRow(rows, "Doctor", doctorName(a));
        appendRow(rows, "Centre", facilityName(a));
        appendRow(rows, "Date", a.getAppointmentDate().format(DATE_FMT));
        appendRow(rows, "Time", a.getSlotStart().format(TIME_FMT) + " - " + a.getSlotEnd().format(TIME_FMT));
        appendRow(rows, "Status / स्थिती", a.getStatus().getLabel() + " / " + a.getStatus().getLabelMr());
        String name = escape(a.getPatientNameDisplay());
        return "<!doctype html><html><body style=\"margin:0;background:#f3f6f5;font-family:Arial,sans-serif;color:#1c2b30\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"><tr><td align=\"center\" style=\"padding:28px 12px\">"
                + "<table role=\"presentation\" width=\"600\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:600px;background:#fff;border:1px solid #dce5e1\">"
                + "<tr><td style=\"padding:24px 28px;background:#156b59;color:#fff\"><div style=\"font-size:20px;font-weight:bold\">CHINCHE HOSPITAL</div>"
                + "<div style=\"font-size:12px;margin-top:5px\">Katol, Nagpur district, Maharashtra</div></td></tr>"
                + "<tr><td style=\"padding:26px 28px 12px\"><div style=\"font-size:20px;font-weight:bold\">" + escape(subject)
                + "</div><p style=\"line-height:1.55\">Namaskar " + name + ",<br>" + escape(message)
                + "</p></td></tr><tr><td style=\"padding:0 28px 24px\"><table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border-collapse:collapse\">"
                + rows + "</table></td></tr><tr><td style=\"padding:16px 28px;background:#f3f6f5;font-size:12px;line-height:1.5\">"
                + "A PDF receipt is attached. For assistance, call " + escape(contactPhone())
                + " and quote your booking reference.<br>— Chinche Hospital, Katol (चींचे हॉस्पिटल, कटोल)</td></tr></table>"
                + "</td></tr></table></body></html>";
    }

    private static void appendRow(StringBuilder rows, String label, String value) {
        rows.append("<tr><th align=\"left\" style=\"width:36%;padding:10px 8px;border-bottom:1px solid #dce5e1;color:#5c6c70;font-size:12px\">")
                .append(escape(label)).append("</th><td style=\"padding:10px 8px;border-bottom:1px solid #dce5e1;font-size:13px;font-weight:bold\">")
                .append(escape(value)).append("</td></tr>");
    }

    private static String statusMessage(AppointmentStatus status) {
        return switch (status) {
            case PENDING -> "Your booking is waiting for hospital confirmation.";
            case CONFIRMED -> "Your appointment is confirmed by the hospital.";
            case CHECKED_IN -> "Your visit has been marked as checked in.";
            case COMPLETED -> "Your visit has been marked as completed.";
            case CANCELLED -> "Your booking has been cancelled. Contact the hospital if this is unexpected.";
            case RESCHEDULED -> "Your booking needs a new time. Please contact the hospital to agree on a slot.";
            case NO_SHOW -> "The booking was marked as a no-show. Contact the hospital if this is incorrect.";
        };
    }

    private static String doctorName(Appointment a) {
        return a.getDoctor() == null ? "Care team" : a.getDoctor().getName();
    }

    private static String facilityName(Appointment a) {
        if (a.getFacility() != null) return a.getFacility().getName();
        return "Chinche Hospital, Katol";
    }

    private String contactPhone() {
        return props.getContact().getPhone() == null ? "the hospital" : props.getContact().getPhone();
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String rootCauseMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
