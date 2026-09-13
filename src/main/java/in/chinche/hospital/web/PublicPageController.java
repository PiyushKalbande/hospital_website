package in.chinche.hospital.web;

import in.chinche.hospital.domain.*;
import in.chinche.hospital.domain.enums.ContentStatus;
import in.chinche.hospital.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** Server-rendered public site. Every route here is SEO-indexable HTML. */
@Controller
@RequiredArgsConstructor
public class PublicPageController {

    private final CatalogService catalogue;
    private final DoctorService doctorService;
    private final FacilityService facilityService;
    private final ContentService contentService;
    private final SearchService searchService;
    private final EnquiryService enquiryService;
    private final in.chinche.hospital.service.BookingWidgetDataService bookingWidgetData;

    // ------------------------------------------------------------ home

    @GetMapping({"/", "/home", "/index"})
    public String home(Model model) {
        model.addAttribute("heroSlides", contentService.heroSlides());
        model.addAttribute("specialities", catalogue.allSpecialities());
        model.addAttribute("featuredSpecialities", catalogue.featuredSpecialities());
        model.addAttribute("procedures", catalogue.topProcedures());
        model.addAttribute("conditions", catalogue.topConditions());
        model.addAttribute("facilities", facilityService.all());
        model.addAttribute("featuredDoctors", doctorService.featuredDtos());
        model.addAttribute("heroQuickLinks", contentService.heroQuickLinks());
        model.addAttribute("announcements", contentService.latestAnnouncements());
        model.addAttribute("blogs", contentService.latestBlogs());
        model.addAttribute("stories", contentService.featuredStories());
        model.addAttribute("testimonials", contentService.testimonials());
        model.addAttribute("awards", contentService.awards());
        model.addAttribute("featuredPackages", catalogue.featuredPackages());
        model.addAttribute("stats", buildStats());
        addBookingWidgetModel(model);
        model.addAttribute("pageTitle", "Multispeciality Hospital in Katol, Nagpur | चींचे हॉस्पिटल");
        model.addAttribute("metaDescription", "Chinche Hospital, Katol (Nagpur) — 120-bed multispeciality hospital with 24×7 emergency, "
                + "18 specialities, dialysis, maternity and cashless insurance. Book appointments and health check-ups online.");
        return "pages/index";
    }

    /** Model attributes shared by every page that embeds the booking widget. */
    private void addBookingWidgetModel(Model model) {
        model.addAttribute("serviceTypes", java.util.Arrays.asList(in.chinche.hospital.domain.enums.ServiceType.values()));
        model.addAttribute("visitModes", java.util.Arrays.asList(in.chinche.hospital.domain.enums.VisitMode.values()));
        model.addAttribute("doctors", doctorService.allDtos());
        model.addAttribute("packages", catalogue.allPackageDtos());
        model.addAttribute("specialities", catalogue.allSpecialities());
        model.addAttribute("facilities", facilityService.all());
        model.addAttribute("bookingDataJson",
                bookingWidgetData.buildJson(org.springframework.context.i18n.LocaleContextHolder.getLocale()));
    }

    private java.util.Map<String, Object> buildStats() {
        return java.util.Map.of(
                "beds", 120,
                "specialities", catalogue.allSpecialities().size(),
                "doctors", doctorService.count(),
                "facilities", facilityService.all().size(),
                "yearsOfService", 27);
    }

    // ------------------------------------------------------------ specialities

    @GetMapping("/specialities")
    public String specialities(Model model) {
        model.addAttribute("specialities", catalogue.allSpecialities());
        model.addAttribute("pageTitle", "Specialities & Centres of Excellence | Chinche Hospital Katol");
        model.addAttribute("metaDescription", "18 clinical specialities at Chinche Hospital, Katol — cardiology, orthopaedics, maternity, "
                + "paediatrics, neurology, nephrology and dialysis, surgery, ENT, ophthalmology and more.");
        return "pages/specialities";
    }

    @GetMapping("/specialities/{slug}")
    public String specialityDetail(@PathVariable String slug, Model model) {
        Speciality speciality = catalogue.specialityBySlug(slug);
        List<in.chinche.hospital.dto.response.DoctorDto> team = doctorService.bySpecialityDtos(speciality.getId());
        model.addAttribute("speciality", speciality);
        model.addAttribute("procedures", catalogue.proceduresForSpeciality(slug));
        model.addAttribute("doctors", team);
        model.addAttribute("pageTitle", speciality.getName() + " in Katol | Chinche Hospital");
        model.addAttribute("metaDescription", speciality.getSummary());
        model.addAttribute("breadcrumb", speciality.getName());
        return "pages/speciality-detail";
    }

    // ------------------------------------------------------------ procedures

    @GetMapping("/procedures")
    public String procedures(Model model) {
        model.addAttribute("procedures", catalogue.allProcedures());
        model.addAttribute("pageTitle", "Treatments & Procedures | Chinche Hospital Katol");
        model.addAttribute("metaDescription", "Angioplasty, knee and hip replacement, laparoscopic surgery, dialysis, stone removal, "
                + "cataract and stroke care at Chinche Hospital, Katol.");
        return "pages/procedures";
    }

    @GetMapping("/procedures/{slug}")
    public String procedureDetail(@PathVariable String slug, Model model) {
        Procedure procedure = catalogue.procedureBySlug(slug);
        model.addAttribute("procedure", procedure);
        model.addAttribute("doctors", procedure.getSpeciality() == null ? List.of()
                : doctorService.bySpecialityDtos(procedure.getSpeciality().getId()));
        model.addAttribute("pageTitle", procedure.getName() + " in Katol | Chinche Hospital");
        model.addAttribute("metaDescription", procedure.getSummary());
        model.addAttribute("breadcrumb", procedure.getName());
        return "pages/procedure-detail";
    }

    // ------------------------------------------------------------ conditions

    @GetMapping("/conditions")
    public String conditions(Model model) {
        model.addAttribute("conditions", catalogue.allConditions());
        model.addAttribute("pageTitle", "Conditions We Treat | Chinche Hospital Katol");
        model.addAttribute("metaDescription", "Diabetes, hypertension, arthritis, kidney stones, hernia, asthma, thyroid, migraine and "
                + "more — treated at Chinche Hospital, Katol.");
        return "pages/conditions";
    }

    @GetMapping("/conditions/{slug}")
    public String conditionDetail(@PathVariable String slug, Model model) {
        HealthCondition condition = catalogue.conditionBySlug(slug);
        model.addAttribute("condition", condition);
        model.addAttribute("doctors", condition.getSpeciality() == null ? List.of()
                : doctorService.bySpecialityDtos(condition.getSpeciality().getId()));
        model.addAttribute("pageTitle", condition.getName() + " — treatment in Katol | Chinche Hospital");
        model.addAttribute("metaDescription", condition.getName() + " diagnosis and treatment at Chinche Hospital, Katol.");
        model.addAttribute("breadcrumb", condition.getName());
        return "pages/condition-detail";
    }

    // ------------------------------------------------------------ doctors

    @GetMapping({"/doctors", "/find-a-doctor"})
    public String doctors(@RequestParam(required = false) String q,
                          @RequestParam(required = false) Long specialityId,
                          @RequestParam(required = false) Long facilityId,
                          @RequestParam(required = false, defaultValue = "false") Boolean teleconsultOnly,
                          @RequestParam(required = false, defaultValue = "0") int page,
                          Model model) {
        var req = new in.chinche.hospital.dto.request.DoctorSearchRequest(
                q, specialityId, facilityId, teleconsultOnly, page, 12, "sortOrder");
        model.addAttribute("results", doctorService.search(req));
        model.addAttribute("specialities", catalogue.allSpecialities());
        model.addAttribute("facilities", facilityService.all());
        model.addAttribute("q", q);
        model.addAttribute("specialityId", specialityId);
        model.addAttribute("facilityId", facilityId);
        model.addAttribute("teleconsultOnly", teleconsultOnly);
        model.addAttribute("pageTitle", "Find a Doctor in Katol | Chinche Hospital");
        model.addAttribute("metaDescription", "Search " + doctorService.count() + " specialist doctors at Chinche Hospital, Katol by "
                + "speciality, centre or video consultation availability.");
        return "pages/doctors";
    }

    @GetMapping("/doctors/{slug}")
    public String doctorDetail(@PathVariable String slug, Model model) {
        Doctor doctor = doctorService.bySlug(slug);
        model.addAttribute("doctor", doctor);
        model.addAttribute("weeklySchedule", doctorService.weeklySchedule(doctor.getId()));
        model.addAttribute("colleagues", doctor.getSpeciality() == null ? List.of()
                : doctorService.bySpecialityDtos(doctor.getSpeciality().getId()));
        model.addAttribute("pageTitle", doctor.getName() + " — " + doctor.getDesignation() + " in Katol");
        model.addAttribute("metaDescription", doctor.getName() + ", " + doctor.getQualifications() + ". "
                + doctor.getExperienceYears() + " years experience. Book an appointment online at Chinche Hospital, Katol.");
        model.addAttribute("breadcrumb", doctor.getName());
        return "pages/doctor-detail";
    }

    // ------------------------------------------------------------ packages & lab

    @GetMapping("/health-packages")
    public String packages(Model model) {
        // DTOs: HealthPackageDto exposes `includes` as a List<String>, the entity stores
        // one delimited TEXT column that templates would have to split by hand.
        model.addAttribute("packages", catalogue.allPackageDtos());
        model.addAttribute("pageTitle", "Health Check-up Packages in Katol | Chinche Hospital");
        model.addAttribute("metaDescription", "Preventive health check-ups from ₹899 at Chinche Hospital, Katol — full body, women's health, "
                + "senior citizen and diabetes profiles with home sample collection.");
        return "pages/health-packages";
    }

    @GetMapping("/health-packages/{slug}")
    public String packageDetail(@PathVariable String slug, Model model) {
        HealthPackage pkg = catalogue.packageBySlug(slug);
        model.addAttribute("healthPackage", pkg);
        model.addAttribute("includesList", in.chinche.hospital.dto.response.HealthPackageDto.from(pkg).includes());
        model.addAttribute("relatedPackages", catalogue.allPackages().stream()
                .filter(p -> !p.getId().equals(pkg.getId())).limit(3).toList());
        model.addAttribute("pageTitle", pkg.getName() + " — ₹" + pkg.getPrice() + " | Chinche Hospital Katol");
        model.addAttribute("metaDescription", pkg.getDescription());
        model.addAttribute("breadcrumb", pkg.getName());
        return "pages/package-detail";
    }

    @GetMapping("/lab-tests")
    public String labTests(Model model) {
        model.addAttribute("labTests", catalogue.allLabTests());
        model.addAttribute("pageTitle", "Lab Tests & Diagnostics in Katol | Chinche Hospital");
        model.addAttribute("metaDescription", "Blood tests, ECG, echocardiography, ultrasound, X-ray and home sample collection at "
                + "Chinche Diagnostics, Katol. Reports in 6–24 hours.");
        return "pages/lab-tests";
    }

    // ------------------------------------------------------------ network / locations

    @GetMapping({"/locations", "/hospitals"})
    public String locations(Model model) {
        model.addAttribute("facilities", facilityService.all());
        model.addAttribute("pageTitle", "Our Centres — Katol, Narkhed & Saoner | Chinche Hospital");
        model.addAttribute("metaDescription", "Four Chinche Hospital centres serving Katol taluka: the 120-bed Katol hospital, clinics at "
                + "Narkhed and Saoner, and a diagnostics centre in Katol.");
        return "pages/locations";
    }

    @GetMapping("/locations/{slug}")
    public String locationDetail(@PathVariable String slug, Model model) {
        Facility facility = facilityService.bySlug(slug);
        model.addAttribute("facility", facility);
        model.addAttribute("pageTitle", facility.getName() + " | Chinche Hospital");
        model.addAttribute("metaDescription", facility.getAddressLine());
        model.addAttribute("breadcrumb", facility.getName());
        return "pages/location-detail";
    }

    // ------------------------------------------------------------ content

    @GetMapping("/blogs")
    public String blogs(@RequestParam(required = false, defaultValue = "0") int page, Model model) {
        Page<Blog> blogPage = contentService.blogs(page, 9, null);
        model.addAttribute("blogPage", blogPage);
        model.addAttribute("pageTitle", "Health Blogs in Marathi & English | Chinche Hospital Katol");
        model.addAttribute("metaDescription", "Practical health advice from Chinche Hospital physicians — monsoon illness, understanding "
                + "reports, surgery recovery and antenatal care.");
        return "pages/blogs";
    }

    @GetMapping("/blogs/{slug}")
    public String blogDetail(@PathVariable String slug, Model model) {
        Blog blog = contentService.blogBySlug(slug);
        model.addAttribute("blog", blog);
        model.addAttribute("related", contentService.latestBlogs().stream()
                .filter(b -> !b.getId().equals(blog.getId())).limit(3).toList());
        model.addAttribute("pageTitle", blog.getTitle() + " | Chinche Hospital");
        model.addAttribute("metaDescription", blog.getExcerpt());
        model.addAttribute("breadcrumb", "Health Blog");
        return "pages/blog-detail";
    }

    @GetMapping("/announcements")
    public String announcements(@RequestParam(required = false, defaultValue = "0") int page, Model model) {
        model.addAttribute("announcementPage", contentService.announcements(page, 10));
        model.addAttribute("pageTitle", "Hospital Updates & Announcements | Chinche Hospital Katol");
        model.addAttribute("metaDescription", "New facilities, camps and service launches at Chinche Hospital, Katol.");
        return "pages/announcements";
    }

    @GetMapping("/announcements/{slug}")
    public String announcementDetail(@PathVariable String slug, Model model) {
        model.addAttribute("announcement", contentService.announcementBySlug(slug));
        model.addAttribute("pageTitle", contentService.announcementBySlug(slug).getTitle());
        model.addAttribute("metaDescription", contentService.announcementBySlug(slug).getSummary());
        return "pages/announcement-detail";
    }

    @GetMapping("/patient-stories")
    public String stories(Model model) {
        model.addAttribute("stories", contentService.allStories());
        model.addAttribute("pageTitle", "Patient Stories & Outcomes | Chinche Hospital Katol");
        model.addAttribute("metaDescription", "Real treatment outcomes from Chinche Hospital — orthopaedics, maternity, cardiology, "
                + "stroke, urology and diabetes care in Katol taluka.");
        return "pages/stories";
    }

    // ------------------------------------------------------------ static-ish pages

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("facilities", facilityService.all());
        model.addAttribute("awards", contentService.awards());
        model.addAttribute("specialities", catalogue.allSpecialities());
        model.addAttribute("pageTitle", "About Chinche Hospital, Katol | चींचे हॉस्पिटल");
        model.addAttribute("metaDescription", "Chinche Hospital has served Katol taluka since 1998 — a 120-bed multispeciality hospital "
                + "with accredited labs, dialysis, NICU and round-the-clock emergency care.");
        return "pages/about";
    }

    @GetMapping("/emergency")
    public String emergency(Model model) {
        model.addAttribute("pageTitle", "24×7 Emergency & Ambulance | Chinche Hospital Katol");
        model.addAttribute("metaDescription", "Round-the-clock trauma, cardiac and stroke emergency at Chinche Hospital, Katol. "
                + "Call 108 or +91 94221 00108.");
        return "pages/emergency";
    }

    @GetMapping("/contact")
    public String contact(@RequestParam(required = false) String type, Model model) {
        model.addAttribute("facilities", facilityService.all());
        model.addAttribute("enquiryType", type == null ? "GENERAL" : type);
        model.addAttribute("pageTitle", "Contact & Directions | Chinche Hospital Katol");
        model.addAttribute("metaDescription", "Reach Chinche Hospital at मुळाई रोड, कटोल — phone, email, directions and the patient "
                + "experience feedback form.");
        return "pages/contact";
    }

    @GetMapping("/track")
    public String trackForm(Model model) {
        model.addAttribute("pageTitle", "Track Your Appointment | Chinche Hospital");
        model.addAttribute("metaDescription", "Check the status of your Chinche Hospital booking using your reference number and mobile number.");
        return "pages/track";
    }

    @GetMapping("/insurance")
    public String insurance(Model model) {
        model.addAttribute("pageTitle", "Cashless Insurance & TPA | Chinche Hospital Katol");
        model.addAttribute("metaDescription", "Chinche Hospital is empanelled with major TPAs and the Mahatma Jyotiba Phule Jan Arogya Yojana.");
        return "pages/insurance";
    }

    @GetMapping("/careers")
    public String careers(Model model) {
        model.addAttribute("pageTitle", "Careers at Chinche Hospital, Katol");
        model.addAttribute("metaDescription", "Nursing, technician, pharmacy and administrative openings at Chinche Hospital, Katol.");
        return "pages/careers";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("suggestions", searchService.suggest(q, 24));
        model.addAttribute("pageTitle", "Search results" + (q == null ? "" : " for " + q));
        model.addAttribute("metaDescription", "Search doctors, specialities, procedures, conditions and health packages.");
        return "pages/search";
    }

    @GetMapping("/sitemap")
    public String sitemap(Model model) {
        model.addAttribute("specialities", catalogue.allSpecialities());
        model.addAttribute("doctors", doctorService.all());
        model.addAttribute("procedures", catalogue.allProcedures());
        model.addAttribute("packages", catalogue.allPackages());
        model.addAttribute("facilities", facilityService.all());
        model.addAttribute("blogs", contentService.blogs(0, 50, null).getContent());
        model.addAttribute("announcements", contentService.announcements(0, 50).getContent());
        model.addAttribute("stories", contentService.allStories());
        model.addAttribute("pageTitle", "Sitemap | Chinche Hospital Katol");
        return "pages/sitemap";
    }


}
