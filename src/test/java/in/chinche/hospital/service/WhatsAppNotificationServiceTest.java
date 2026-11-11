package in.chinche.hospital.service;

import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.domain.Appointment;
import in.chinche.hospital.domain.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WhatsAppNotificationServiceTest {

    @Test
    void sendsApprovedTemplateOnlyForPatientsWithMessagingConsent() {
        AppProperties properties = new AppProperties();
        properties.getWhatsapp().setEnabled(true);
        properties.getWhatsapp().setPhoneNumberId("123456789");
        properties.getWhatsapp().setAccessToken("test-token");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://graph.facebook.com/v22.0/123456789/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(jsonPath("$.messaging_product").value("whatsapp"))
                .andExpect(jsonPath("$.to").value("919876543210"))
                .andExpect(jsonPath("$.template.name").value("doctor_emergency_reschedule"))
                .andRespond(withSuccess());
        WhatsAppNotificationService service = new WhatsAppNotificationService(properties, builder);

        service.sendDoctorEmergencyReschedule(appointment(true));

        server.verify();
    }

    @Test
    void skipsPatientsWithoutMessagingConsent() {
        AppProperties properties = new AppProperties();
        properties.getWhatsapp().setEnabled(true);
        properties.getWhatsapp().setPhoneNumberId("123456789");
        properties.getWhatsapp().setAccessToken("test-token");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WhatsAppNotificationService service = new WhatsAppNotificationService(properties, builder);

        service.sendDoctorEmergencyReschedule(appointment(false));

        server.verify();
    }

    @Test
    void refusesToEnableProviderWithoutCredentials() {
        AppProperties properties = new AppProperties();
        properties.getWhatsapp().setEnabled(true);

        assertThrows(IllegalArgumentException.class,
                () -> new WhatsAppNotificationService(properties, RestClient.builder()));
    }

    private static Appointment appointment(boolean consent) {
        Patient patient = Patient.builder().fullName("Asha Patil")
                .phone("+91 98765 43210").consentSms(consent).build();
        Appointment appointment = mock(Appointment.class);
        when(appointment.getPatient()).thenReturn(patient);
        when(appointment.getPatientNameDisplay()).thenReturn("Asha Patil");
        when(appointment.getReferenceNo()).thenReturn("CHH-2026-000123");
        when(appointment.getAppointmentDate()).thenReturn(LocalDate.of(2026, 11, 5));
        when(appointment.getSlotStart()).thenReturn(LocalTime.of(15, 0));
        return appointment;
    }
}