package in.chinche.hospital.service;

import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.domain.Appointment;
import in.chinche.hospital.domain.Patient;
import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.domain.enums.ServiceType;
import in.chinche.hospital.domain.enums.VisitMode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingReceiptPdfServiceTest {

    @Test
    void createsReadableStructuredPdfReceipt() throws Exception {
        AppProperties properties = new AppProperties();
        BookingReceiptPdfService service = new BookingReceiptPdfService(properties);
        Appointment appointment = mock(Appointment.class);
        Patient patient = mock(Patient.class);
        when(appointment.getReferenceNo()).thenReturn("CHH-2026-000123");
        when(appointment.getPatient()).thenReturn(patient);
        when(patient.getFullName()).thenReturn("आशा पाटील");
        when(patient.getPhone()).thenReturn("9876543210");
        when(appointment.getPatientNameDisplay()).thenReturn("आशा पाटील");
        when(appointment.getPatientPhoneDisplay()).thenReturn("9876543210");
        when(appointment.getServiceType()).thenReturn(ServiceType.OPD_CONSULTATION);
        when(appointment.getDoctor()).thenReturn(null);
        when(appointment.getAppointmentDate()).thenReturn(LocalDate.of(2026, 11, 3));
        when(appointment.getSlotStart()).thenReturn(LocalTime.of(10, 0));
        when(appointment.getSlotEnd()).thenReturn(LocalTime.of(10, 20));
        when(appointment.getFacility()).thenReturn(null);
        when(appointment.getVisitMode()).thenReturn(VisitMode.IN_HOSPITAL);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.PENDING);
        when(appointment.getAmountPayable()).thenReturn(new BigDecimal("500.00"));
        when(appointment.getPaymentMode()).thenReturn("PAY_AT_HOSPITAL");

        byte[] pdf = service.generate(appointment);

        assertTrue(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF"));
        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertEquals(1, document.getNumberOfPages());
            assertTrue(text.contains("APPOINTMENT RECEIPT"));
            assertTrue(text.contains("CHH-2026-000123"));
            assertTrue(text.toLowerCase(java.util.Locale.ROOT).contains("patila"));
            assertFalse(text.contains("??"));
            assertFalse(text.contains("null"));
            assertTrue(text.contains("9876543210"));
            assertTrue(text.contains("INR 500.00"), text);
        }
    }
}