package in.chinche.hospital.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.chinche.hospital.domain.enums.ServiceType;
import in.chinche.hospital.domain.enums.VisitMode;
import in.chinche.hospital.dto.response.DoctorDto;
import in.chinche.hospital.dto.response.HealthPackageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Serialises everything the booking widget needs at boot time into one JSON
 * island, so the page paints its first interactive state without a round trip.
 * Live slot availability is still fetched per doctor from {@code /api/v1}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingWidgetDataService {

    /** i18n keys the widget looks up at runtime, resolved server-side for the active locale. */
    private static final Map<String, String> I18N_KEYS = new LinkedHashMap<>();

    static {
        I18N_KEYS.put("step", "booking.step");
        I18N_KEYS.put("next", "booking.next");
        I18N_KEYS.put("back", "booking.back");
        I18N_KEYS.put("confirm", "booking.confirm");
        I18N_KEYS.put("newBooking", "booking.new");
        I18N_KEYS.put("loading", "common.loading");
        I18N_KEYS.put("noSlots", "booking.no.slots");
        I18N_KEYS.put("morning", "booking.slot.morning");
        I18N_KEYS.put("afternoon", "booking.slot.afternoon");
        I18N_KEYS.put("evening", "booking.slot.evening");
        I18N_KEYS.put("full", "booking.slot.unavailable");
        I18N_KEYS.put("doctor", "nav.doctors");
        I18N_KEYS.put("choosePackage", "nav.packages");
        I18N_KEYS.put("summaryTitle", "booking.summary");
        I18N_KEYS.put("service", "booking.step.service");
        I18N_KEYS.put("date", "common.page");
        I18N_KEYS.put("time", "common.page");
        I18N_KEYS.put("centre", "nav.locations");
        I18N_KEYS.put("patient", "booking.patient");
        I18N_KEYS.put("fee", "booking.fee");
        I18N_KEYS.put("successTitle", "booking.success.title");
        I18N_KEYS.put("reference", "booking.success.reference");
        I18N_KEYS.put("successNote", "booking.success.note");
        I18N_KEYS.put("print", "booking.success.print");
        I18N_KEYS.put("whatsapp", "booking.success.whatsapp");
        I18N_KEYS.put("years", "common.experience");
        I18N_KEYS.put("teleconsult", "common.teleconsult");
        I18N_KEYS.put("selectToContinue", "common.required");
    }

    private final DoctorService doctors;
    private final CatalogService catalogue;
    private final MessageSource messages;
    private final ObjectMapper objectMapper;

    /** One JSON document: doctors, packages, service types, visit modes and UI strings. */
    public String buildJson(Locale locale) {
        List<DoctorDto> doctorRows = doctors.allDtos();
        List<HealthPackageDto> packageRows = catalogue.allPackageDtos();

        Map<String, String> i18n = new LinkedHashMap<>();
        I18N_KEYS.forEach((jsKey, messageKey) -> i18n.put(jsKey, messages.getMessage(messageKey, null, messageKey, locale)));
        i18n.put("slots", "mr".equals(locale.getLanguage()) ? "वेळा" : "slots");
        i18n.put("tests", "mr".equals(locale.getLanguage()) ? "तपासण्या" : "tests");
        i18n.put("copied", "mr".equals(locale.getLanguage()) ? "कॉपी झाले" : "Copied");
        i18n.put("of", "mr".equals(locale.getLanguage()) ? "/" : "of");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("doctors", doctorRows);
        root.put("packages", packageRows);
        root.put("serviceTypes", Arrays.stream(ServiceType.values()).map(st -> Map.of(
                "value", st.name(),
                "label", st.getLabel(),
                "labelMr", st.getLabelMr(),
                "description", st.getDescription()
        )).toList());
        root.put("visitModes", Arrays.stream(VisitMode.values()).map(vm -> Map.of(
                "value", vm.name(),
                "label", vm.getLabel(),
                "labelMr", vm.getLabelMr()
        )).toList());
        root.put("i18n", i18n);
        root.put("marathi", "mr".equals(locale.getLanguage()));

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            log.error("Could not serialise the booking widget data island", ex);
            return "{\"doctors\":[],\"packages\":[],\"serviceTypes\":[],\"visitModes\":[],\"i18n\":{}}";
        }
    }
}
