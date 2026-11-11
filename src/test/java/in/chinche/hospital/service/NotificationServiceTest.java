package in.chinche.hospital.service;

import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.domain.Appointment;
import in.chinche.hospital.domain.Patient;
import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.domain.enums.ServiceType;
import in.chinche.hospital.domain.enums.VisitMode;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Test
    void sendsStatusEmailWithBilingualDetailsAndPdfReceipt() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        BookingReceiptPdfService receiptPdf = mock(BookingReceiptPdfService.class);
        AppProperties properties = new AppProperties();
        properties.getContact().setPhone("+91 70836 82563");
        NotificationService service = new NotificationService(mailSender, properties, receiptPdf);
        ReflectionTestUtils.setField(service, "fromAddress", "piyushkalbande111@gmail.com");

        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(receiptPdf.generate(any())).thenReturn("%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        Appointment appointment = appointment();

        service.sendStatusUpdate(appointment);

        verify(mailSender).send(message);
        verify(receiptPdf).generate(appointment);
        assertTrue(message.getSubject().contains("confirmed"));
        MimeMultipart parts = (MimeMultipart) message.getContent();
        assertEquals(2, parts.getCount());
        assertEquals(Part.ATTACHMENT, parts.getBodyPart(1).getDisposition());
        assertTrue(parts.getBodyPart(1).getFileName().endsWith("-receipt.pdf"));
    }

    private static Appointment appointment() {
        Patient patient = Patient.builder().fullName("Asha Patil").phone("9876543210")
                .email("asha@example.test").build();
        return Appointment.builder()
                .referenceNo("CHH-2026-000123")
                .patient(patient)
                .serviceType(ServiceType.OPD_CONSULTATION)
                .visitMode(VisitMode.IN_HOSPITAL)
                .appointmentDate(LocalDate.of(2026, 11, 3))
                .slotStart(LocalTime.of(10, 0))
                .slotEnd(LocalTime.of(10, 20))
                .status(AppointmentStatus.CONFIRMED)
                .amountPayable(new BigDecimal("500.00"))
                .build();
    }
}