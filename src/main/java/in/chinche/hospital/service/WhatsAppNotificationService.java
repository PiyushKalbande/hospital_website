package in.chinche.hospital.service;

import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.domain.Appointment;
import in.chinche.hospital.domain.Patient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class WhatsAppNotificationService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final AppProperties.WhatsApp settings;
    private final RestClient restClient;

    public WhatsAppNotificationService(AppProperties properties, RestClient.Builder builder) {
        this.settings = properties.getWhatsapp();
        validateSettings();
        this.restClient = builder.clone().baseUrl(settings.getBaseUrl()).build();
    }

    @Async("notificationExecutor")
    public void sendDoctorEmergencyReschedule(Appointment appointment) {
        if (!settings.isEnabled()) return;
        Patient patient = appointment.getPatient();
        if (patient == null || !patient.isConsentSms()) {
            log.info("Skipped emergency WhatsApp notice for reference {} (no messaging consent).",
                    appointment.getReferenceNo());
            return;
        }

        String recipient = normalizedPhone(patient.getPhone());
        if (recipient == null) {
            log.warn("Skipped emergency WhatsApp notice for reference {} (invalid patient phone).",
                    appointment.getReferenceNo());
            return;
        }

        List<Map<String, String>> parameters = List.of(
                Map.of("type", "text", "text", appointment.getPatientNameDisplay()),
                Map.of("type", "text", "text", appointment.getReferenceNo()),
                Map.of("type", "text", "text", appointment.getAppointmentDate().format(DATE_FORMAT)),
                Map.of("type", "text", "text", appointment.getSlotStart().format(TIME_FORMAT)));
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", recipient,
                "type", "template",
                "template", Map.of(
                        "name", settings.getTemplateName(),
                        "language", Map.of("code", settings.getLanguageCode()),
                        "components", List.of(Map.of("type", "body", "parameters", parameters))));

        try {
            restClient.post()
                    .uri("/{version}/{phoneNumberId}/messages", settings.getApiVersion(), settings.getPhoneNumberId())
                    .headers(headers -> headers.setBearerAuth(settings.getAccessToken()))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Emergency WhatsApp notice accepted by provider for booking {}.", appointment.getReferenceNo());
        } catch (RestClientException ex) {
            log.warn("Emergency WhatsApp notice failed for booking {} ({}).",
                    appointment.getReferenceNo(), ex.getClass().getSimpleName());
        }
    }

    private void validateSettings() {
        if (!settings.isEnabled()) return;
        URI endpoint = URI.create(settings.getBaseUrl());
        if (!"https".equalsIgnoreCase(endpoint.getScheme())
                || !"graph.facebook.com".equalsIgnoreCase(endpoint.getHost())
                || endpoint.getUserInfo() != null || endpoint.getPort() != -1) {
            throw new IllegalArgumentException("WhatsApp API base URL must be https://graph.facebook.com.");
        }
        if (settings.getPhoneNumberId() == null || settings.getPhoneNumberId().isBlank()
                || settings.getAccessToken() == null || settings.getAccessToken().isBlank()
                || settings.getTemplateName() == null || settings.getTemplateName().isBlank()) {
            throw new IllegalArgumentException(
                    "WhatsApp is enabled but phone-number ID, access token, or approved template name is missing.");
        }
    }

    private static String normalizedPhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 10) digits = "91" + digits;
        return digits.length() >= 11 && digits.length() <= 15 ? digits : null;
    }
}