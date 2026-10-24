package in.chinche.hospital.web;

import in.chinche.hospital.domain.Appointment;
import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Server-rendered staff console. Locked to ROLE_ADMIN / ROLE_RECEPTIONIST. */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
public class AdminConsoleController {

    private final AdminStatsService stats;
    private final BookingService bookingService;
    private final DoctorService doctorService;
    private final CatalogService catalogue;
    private final EnquiryService enquiryService;
    private final FacilityService facilityService;
    private final DoctorAttendanceService doctorAttendanceService;
    private final AppointmentRepositoryFacade appointments;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("stats", stats.overview());
        model.addAttribute("trend", stats.dailyTrend(14));
        model.addAttribute("bySpeciality", stats.bySpeciality());
        model.addAttribute("todayBookings", appointments.today());
        model.addAttribute("pageTitle", "Dashboard | Chinche Hospital Admin");
        model.addAttribute("activeView", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/appointments")
    public String appointments(@RequestParam(required = false) String status,
                               @RequestParam(required = false) String date,
                               @RequestParam(required = false, defaultValue = "0") int page,
                               Model model) {
        Page<Appointment> result = appointments.query(status, date, page, 20);
        model.addAttribute("appointments", result);
        model.addAttribute("statuses", List.of(AppointmentStatus.values()));
        model.addAttribute("status", status);
        model.addAttribute("date", date);
        model.addAttribute("pageTitle", "Appointments | Chinche Hospital Admin");
        model.addAttribute("activeView", "appointments");
        return "admin/appointments";
    }

    @PostMapping("/appointments/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam AppointmentStatus status,
                               @RequestParam(required = false) String note,
                               RedirectAttributes flash) {
        appointments.updateStatus(id, status, note);
        flash.addFlashAttribute("success", "Booking " + id + " marked as " + status.getLabel() + ".");
        return "redirect:/admin/appointments";
    }

    @GetMapping("/doctors")
    public String doctors(@RequestParam(required = false) LocalDate date,
                          @RequestParam(required = false) Long doctorId,
                          @RequestParam(required = false, defaultValue = "doctors") String view,
                          Model model) {
        LocalDate selectedDate = date == null ? LocalDate.now() : date;
        LocalDate calendarEnd = selectedDate.plusDays(30);
        var doctorList = doctorService.all();
        model.addAttribute("doctors", doctorList);
        model.addAttribute("specialities", catalogue.allSpecialities());
        model.addAttribute("facilities", facilityService.all());
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("attendanceByDoctor", doctorAttendanceService.attendanceFor(selectedDate));
        model.addAttribute("leaveDatesByDoctor", doctorAttendanceService.leaveDates(selectedDate, calendarEnd));
        model.addAttribute("calendarDates", selectedDate.datesUntil(calendarEnd.plusDays(1)).toList());
        model.addAttribute("selectedDoctorId", doctorId);
        model.addAttribute("selectedDoctor", doctorList.stream().filter(d -> d.getId().equals(doctorId)).findFirst().orElse(null));
        model.addAttribute("calendarView", "calendar".equalsIgnoreCase(view));
        model.addAttribute("pageTitle", "Doctors | Chinche Hospital Admin");
        model.addAttribute("activeView", "doctors");
        return "admin/doctors";
    }

    @PostMapping("/doctors/{doctorId}/attendance")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveDoctorAttendance(@PathVariable Long doctorId,
                                       @RequestParam LocalDate date,
                                       @RequestParam(required = false) LocalTime arrival,
                                       @RequestParam(required = false) LocalTime departure,
                                       @RequestParam(name = "breakStart", required = false) List<LocalTime> breakStarts,
                                       @RequestParam(name = "breakEnd", required = false) List<LocalTime> breakEnds,
                                       @RequestParam(required = false) String notes,
                                       RedirectAttributes flash) {
        try {
            doctorAttendanceService.saveAttendance(doctorId, date, arrival, departure,
                    breakStarts == null ? List.of() : breakStarts,
                    breakEnds == null ? List.of() : breakEnds, notes);
            flash.addFlashAttribute("success", "Attendance saved for " + date + ".");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/doctors?date=" + date;
    }

    @PostMapping("/doctors/{doctorId}/leave")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveDoctorLeave(@PathVariable Long doctorId,
                                  @RequestParam LocalDate from,
                                  @RequestParam LocalDate to,
                                  @RequestParam(required = false) String reason,
                                  RedirectAttributes flash) {
        try {
            int days = doctorAttendanceService.setLeave(doctorId, from, to, reason);
            flash.addFlashAttribute("success", "Leave marked for " + days + " day(s).");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/doctors?date=" + from;
    }

    @GetMapping("/catalogue")
    public String catalogue(Model model) {
        model.addAttribute("specialities", catalogue.allSpecialities());
        model.addAttribute("procedures", catalogue.allProcedures());
        model.addAttribute("conditions", catalogue.allConditions());
        model.addAttribute("packages", catalogue.allPackages());
        model.addAttribute("labTests", catalogue.allLabTests());
        model.addAttribute("pageTitle", "Catalogue | Chinche Hospital Admin");
        model.addAttribute("activeView", "catalogue");
        return "admin/catalogue";
    }

    @GetMapping("/enquiries")
    public String enquiries(@RequestParam(required = false) String status,
                            @RequestParam(required = false, defaultValue = "0") int page,
                            Model model) {
        model.addAttribute("enquiries", enquiryService.list(status, page, 20));
        model.addAttribute("status", status);
        model.addAttribute("pageTitle", "Enquiries | Chinche Hospital Admin");
        model.addAttribute("activeView", "enquiries");
        return "admin/enquiries";
    }

    @GetMapping("/facilities")
    public String facilities(Model model) {
        model.addAttribute("facilities", facilityService.all());
        model.addAttribute("pageTitle", "Centres | Chinche Hospital Admin");
        model.addAttribute("activeView", "facilities");
        return "admin/facilities";
    }

    /** Thin facade so the console never touches repositories directly. */
    @org.springframework.stereotype.Component
    @RequiredArgsConstructor
    public static class AppointmentRepositoryFacade {
        private final in.chinche.hospital.repository.AppointmentRepository repo;

        public List<Appointment> today() {
            return repo.findByAppointmentDateOrderBySlotStartAsc(LocalDate.now());
        }

        public Page<Appointment> query(String status, String date, int page, int size) {
            PageRequest pr = PageRequest.of(Math.max(page, 0), size);
            if (date != null && !date.isBlank()) {
                LocalDate d = LocalDate.parse(date);
                List<Appointment> all = repo.findByAppointmentDateOrderBySlotStartAsc(d);
                List<Appointment> filtered = (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status))
                        ? all
                        : all.stream().filter(a -> a.getStatus().name().equalsIgnoreCase(status)).toList();
                int from = Math.min(pr.getOffset() > Integer.MAX_VALUE ? 0 : (int) pr.getOffset(), filtered.size());
                int to = Math.min(from + size, filtered.size());
                return new org.springframework.data.domain.PageImpl<>(filtered.subList(from, to), pr, filtered.size());
            }
            return (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status))
                    ? repo.findAllByOrderByCreatedAtDesc(pr)
                    : repo.findByStatusOrderByCreatedAtDesc(AppointmentStatus.valueOf(status.toUpperCase()), pr);
        }

        public void updateStatus(Long id, AppointmentStatus status, String note) {
            repo.findById(id).ifPresent(a -> {
                a.setStatus(status);
                if (note != null && !note.isBlank()) a.setInternalNotes(note);
                repo.save(a);
            });
        }
    }
}
