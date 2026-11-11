package in.chinche.hospital.web;

import in.chinche.hospital.dto.response.ApiResponse;
import in.chinche.hospital.domain.Appointment;
import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.exception.SlotUnavailableException;
import in.chinche.hospital.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
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
    private final DoctorAccountService doctorAccounts;
    private final CatalogService catalogue;
    private final EnquiryService enquiryService;
    private final FacilityService facilityService;
    private final DoctorAttendanceService doctorAttendanceService;
    private final AppointmentRepositoryFacade appointments;
    private final AppointmentUpdatePublisher appointmentUpdates;

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
        populateAppointmentModel(status, date, page, model);
        return "admin/appointments";
    }

    @GetMapping("/appointment-emergencies")
    public String appointmentEmergencies(Model model) {
        List<EmergencyNotice> notices = appointments.emergencyQueue();
        model.addAttribute("emergencyNotices", notices);
        model.addAttribute("pageTitle", "Urgent Reschedules | Chinche Hospital Admin");
        model.addAttribute("activeView", "appointment-emergencies");
        return "admin/emergencies";
    }

    @GetMapping("/appointments/fragment")
    public String appointmentFragment(@RequestParam(required = false) String status,
                                     @RequestParam(required = false) String date,
                                     @RequestParam(required = false, defaultValue = "0") int page,
                                     Model model) {
        populateAppointmentModel(status, date, page, model);
        return "admin/appointments :: appointmentResults";
    }

    @GetMapping(value = "/api/appointments/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter appointmentEvents() {
        return appointmentUpdates.subscribeAdmin();
    }

    @PostMapping("/api/appointments/{id}/status")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> updateStatusLive(
            @PathVariable Long id,
            @RequestParam AppointmentStatus status,
            @RequestParam(required = false) String note) {
        try {
            bookingService.updateStatus(id, status, note);
            return ResponseEntity.ok(ApiResponse.ok("Appointment updated.", null));
        } catch (SlotUnavailableException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(ex.getMessage()));
        }
    }

    @PostMapping("/appointments/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam AppointmentStatus status,
                               @RequestParam(required = false) String note,
                               RedirectAttributes flash) {
        try {
            bookingService.updateStatus(id, status, note);
            flash.addFlashAttribute("success", "Booking " + id + " marked as " + status.getLabel() + ".");
        } catch (SlotUnavailableException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/appointments";
    }

    private void populateAppointmentModel(String status, String date, int page, Model model) {
        Page<Appointment> result = appointments.query(status, date, page, 20);
        model.addAttribute("appointments", result);
        model.addAttribute("statuses", List.of(AppointmentStatus.values()));
        model.addAttribute("status", status);
        model.addAttribute("date", date);
        model.addAttribute("pageTitle", "Appointments | Chinche Hospital Admin");
        model.addAttribute("activeView", "appointments");
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
        model.addAttribute("doctorUsernames", doctorAccounts.usernamesForDoctors(
            doctorList.stream().map(doctor -> doctor.getId()).toList()));
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

    @PostMapping("/doctors/{doctorId}/account")
    @PreAuthorize("hasRole('ADMIN')")
    public String createDoctorAccount(@PathVariable Long doctorId,
                                      @RequestParam String username,
                                      @RequestParam String initialPassword,
                                      RedirectAttributes flash) {
        try {
            String createdUsername = doctorAccounts.createAccount(doctorId, username, initialPassword);
            flash.addFlashAttribute("success", "Doctor login '" + createdUsername
                    + "' created. Share the initial password with the doctor through a secure channel.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/doctors?doctorId=" + doctorId;
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
                           @RequestParam(required = false, defaultValue = "false") boolean confirmAffected,
                           @RequestParam(name = "approvedAppointmentIds", required = false) List<Long> approvedAppointmentIds,
                           @RequestParam(required = false) String emergencyReason,
                           Model model, RedirectAttributes flash) {
        List<LocalTime> normalizedStarts = breakStarts == null ? List.of() : breakStarts;
        List<LocalTime> normalizedEnds = breakEnds == null ? List.of() : breakEnds;
        try {
            if (confirmAffected) {
            int affected = doctorAttendanceService.confirmAttendanceChange(doctorId, date, arrival, departure,
                normalizedStarts, normalizedEnds, notes, approvedAppointmentIds, emergencyReason);
            flash.addFlashAttribute("success", "Attendance saved for " + date + ". " + affected
                + " affected patient(s) were added to urgent reschedules and notified.");
            } else {
            List<Appointment> conflicts = doctorAttendanceService.attendanceConflicts(
                doctorId, date, arrival, departure, normalizedStarts, normalizedEnds);
            if (!conflicts.isEmpty()) {
                attendanceConfirmationModel(model, doctorId, date, arrival, departure,
                    normalizedStarts, normalizedEnds, notes, conflicts, null);
                return "admin/attendance-confirmation";
            }
            doctorAttendanceService.saveAttendance(
                doctorId, date, arrival, departure, normalizedStarts, normalizedEnds, notes);
            flash.addFlashAttribute("success", "Attendance and breaks saved for " + date + ".");
            }
        } catch (IllegalArgumentException ex) {
            if (confirmAffected) {
            List<Appointment> currentConflicts = doctorAttendanceService.attendanceConflicts(
                doctorId, date, arrival, departure, normalizedStarts, normalizedEnds);
            if (!currentConflicts.isEmpty()) {
                attendanceConfirmationModel(model, doctorId, date, arrival, departure,
                    normalizedStarts, normalizedEnds, notes, currentConflicts, ex.getMessage());
                return "admin/attendance-confirmation";
            }
            }
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/doctors?date=" + date;
    }

        private static void attendanceConfirmationModel(Model model, Long doctorId, LocalDate date,
                                 LocalTime arrival, LocalTime departure,
                                 List<LocalTime> breakStarts, List<LocalTime> breakEnds,
                                 String notes, List<Appointment> conflicts, String error) {
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("date", date);
        model.addAttribute("arrival", arrival);
        model.addAttribute("departure", departure);
        model.addAttribute("breakStarts", breakStarts);
        model.addAttribute("breakEnds", breakEnds);
        model.addAttribute("notes", notes);
        model.addAttribute("affectedAppointments", conflicts);
        model.addAttribute("hasCheckedIn", conflicts.stream()
            .anyMatch(appointment -> appointment.getStatus() == AppointmentStatus.CHECKED_IN));
        model.addAttribute("error", error);
        model.addAttribute("pageTitle", "Confirm attendance changes | Chinche Hospital Admin");
        model.addAttribute("activeView", "doctors");
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

        public List<EmergencyNotice> emergencyQueue() {
            return repo.findByStatusAndRescheduleReasonIsNotNullOrderByAppointmentDateAscSlotStartAsc(
                            AppointmentStatus.RESCHEDULED).stream()
                    .map(appointment -> new EmergencyNotice(appointment, whatsappUrl(appointment)))
                    .toList();
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

    }

        public record EmergencyNotice(Appointment appointment, String whatsappUrl) {
        }

        private static String whatsappUrl(Appointment appointment) {
        if (appointment.getPatient() == null || !appointment.getPatient().isConsentSms()) return null;
        String phone = appointment.getPatientPhoneDisplay() == null
            ? "" : appointment.getPatientPhoneDisplay().replaceAll("[^0-9]", "");
        if (phone.isBlank()) return null;
        String message = "Namaskar " + appointment.getPatientNameDisplay()
            + ", Chinche Hospital has an urgent update about your appointment on "
            + appointment.getAppointmentDate() + " at " + appointment.getSlotStart()
            + ". The doctor is unexpectedly unavailable. Please contact us to choose another time."
            + " Booking reference: " + appointment.getReferenceNo() + ".";
        return "https://wa.me/" + phone + "?text="
            + UriUtils.encodeQueryParam(message, StandardCharsets.UTF_8);
        }
}
