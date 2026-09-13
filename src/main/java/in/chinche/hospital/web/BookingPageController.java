package in.chinche.hospital.web;

import in.chinche.hospital.domain.enums.ServiceType;
import in.chinche.hospital.domain.enums.VisitMode;
import in.chinche.hospital.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

/**
 * Hosts the multi-step booking widget and the booking-tracking page.
 * The page shell is server-rendered; every step's data is fetched from /api/v1 so
 * the widget can also be embedded on partner sites or in the mobile app.
 */
@Controller
@RequiredArgsConstructor
public class BookingPageController {

    private final CatalogService catalogue;
    private final DoctorService doctorService;
    private final FacilityService facilityService;
    private final SlotAvailabilityService availability;
    private final BookingWidgetDataService bookingWidgetData;

    @GetMapping({"/book", "/appointment"})
    public String book(@RequestParam(required = false) String service,
                       @RequestParam(required = false) Long doctorId,
                       @RequestParam(required = false) Long specialityId,
                       @RequestParam(required = false) Long packageId,
                       Model model) {
        model.addAttribute("bookingDataJson", bookingWidgetData.buildJson(
                org.springframework.context.i18n.LocaleContextHolder.getLocale()));
        model.addAttribute("serviceTypes", Arrays.asList(ServiceType.values()));
        model.addAttribute("visitModes", Arrays.asList(VisitMode.values()));
        model.addAttribute("specialities", catalogue.allSpecialities());
        model.addAttribute("facilities", facilityService.all());
        model.addAttribute("doctors", doctorService.allDtos());
        model.addAttribute("packages", catalogue.allPackageDtos());
        model.addAttribute("labTests", catalogue.allLabTests());
        model.addAttribute("calendarStart", availability.today());
        model.addAttribute("calendarEnd", availability.lastBookableDate());
        model.addAttribute("presetService", service);
        model.addAttribute("presetDoctorId", doctorId);
        model.addAttribute("presetSpecialityId", specialityId);
        model.addAttribute("presetPackageId", packageId);
        model.addAttribute("pageTitle", "Book an Appointment Online | Chinche Hospital Katol");
        model.addAttribute("metaDescription", "Book an OPD consultation, video consultation, health check-up or lab test at Chinche "
                + "Hospital, Katol in under two minutes. Real-time slot availability.");
        return "pages/book";
    }

    @GetMapping("/track/{reference}")
    public String track(@PathVariable String reference, Model model) {
        model.addAttribute("reference", reference);
        model.addAttribute("pageTitle", "Booking " + reference + " | Chinche Hospital");
        return "pages/track";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("pageTitle", "Download Your Reports | Chinche Hospital");
        return "pages/reports";
    }

    @GetMapping("/blood-bank")
    public String bloodBank(Model model) {
        model.addAttribute("facilities", facilityService.all());
        model.addAttribute("pageTitle", "Blood Bank & Donor Registration | Chinche Hospital Katol");
        return "pages/blood-bank";
    }
}
