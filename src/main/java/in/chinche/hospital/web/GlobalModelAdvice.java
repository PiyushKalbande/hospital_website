package in.chinche.hospital.web;

import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.service.CatalogService;
import in.chinche.hospital.service.ContentService;
import in.chinche.hospital.service.DoctorService;
import in.chinche.hospital.service.FacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Populates the shared chrome (nav specialities, facility switcher, quick links,
 * contact block) for every Thymeleaf view, so templates never repeat the lookups.
 */
@ControllerAdvice(basePackages = "in.chinche.hospital.web")
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final CatalogService catalogue;
    private final FacilityService facilityService;
    private final DoctorService doctorService;
    private final ContentService contentService;
    private final AppProperties props;

    @ModelAttribute
    public void injectGlobalModel(Model model) {
        if (model.containsAttribute("navSpecialities")) return;
        model.addAttribute("navSpecialities", catalogue.allSpecialities());
        model.addAttribute("navFacilities", facilityService.all());
        model.addAttribute("navQuickLinks", contentService.patientCornerLinks());
        model.addAttribute("contact", props.getContact());
        model.addAttribute("hospitalName", "Chinche Hospital");
        model.addAttribute("hospitalNameMr", "चींचे हॉस्पिटल");
        model.addAttribute("tagline", "Multi-speciality care for Katol & Vidarbha since 1998");
        model.addAttribute("doctorCount", doctorService.count());
        model.addAttribute("appVersion", "1.0.0");
    }
}
