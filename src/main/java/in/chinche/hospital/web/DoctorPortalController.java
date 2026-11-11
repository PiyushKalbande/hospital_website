package in.chinche.hospital.web;

import in.chinche.hospital.domain.AppUser;
import in.chinche.hospital.domain.Appointment;
import in.chinche.hospital.domain.Doctor;
import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.repository.AppUserRepository;
import in.chinche.hospital.repository.AppointmentRepository;
import in.chinche.hospital.service.DoctorAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Controller
@RequestMapping("/doctor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorPortalController {

    private static final ZoneId HOSPITAL_ZONE = ZoneId.of("Asia/Kolkata");
    private static final List<AppointmentStatus> ACTIVE_STATUSES = List.of(
            AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED,
            AppointmentStatus.CHECKED_IN, AppointmentStatus.RESCHEDULED);

    private final AppUserRepository users;
    private final AppointmentRepository appointments;
    private final DoctorAttendanceService attendanceService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Authentication authentication, Model model) {
        Doctor doctor = signedInDoctor(authentication);
        LocalDate today = LocalDate.now(HOSPITAL_ZONE);
        List<Appointment> todayAppointments = appointments
                .findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAscSlotStartAsc(
                        doctor.getId(), today, today).stream()
                .filter(appointment -> ACTIVE_STATUSES.contains(appointment.getStatus()))
                .toList();
        model.addAttribute("doctor", doctor);
        model.addAttribute("todayAppointments", todayAppointments);
        model.addAttribute("pageTitle", "Doctor dashboard | Chinche Hospital");
        model.addAttribute("activeView", "dashboard");
        return "doctor/dashboard";
    }

    @GetMapping("/appointments")
    public String appointments(Authentication authentication,
                               @RequestParam(required = false) LocalDate from,
                               @RequestParam(required = false) LocalDate to,
                               Model model) {
        Doctor doctor = signedInDoctor(authentication);
        LocalDate today = LocalDate.now(HOSPITAL_ZONE);
        LocalDate start = from == null ? today.minusDays(30) : from;
        LocalDate end = to == null ? today.plusDays(60) : to;
        if (end.isBefore(start) || end.isAfter(today.plusDays(365))) {
            throw new IllegalArgumentException("Choose a valid appointment date range.");
        }
        List<Appointment> doctorAppointments = appointments
                .findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAscSlotStartAsc(
                        doctor.getId(), start, end);
        model.addAttribute("doctor", doctor);
        model.addAttribute("appointments", doctorAppointments);
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("pageTitle", "My appointments | Chinche Hospital");
        model.addAttribute("activeView", "appointments");
        return "doctor/appointments";
    }

    @GetMapping("/attendance")
    public String attendance(Authentication authentication,
                             @RequestParam(required = false) LocalDate date,
                             Model model) {
        Doctor doctor = signedInDoctor(authentication);
        LocalDate selectedDate = date == null ? LocalDate.now(HOSPITAL_ZONE) : date;
        model.addAttribute("doctor", doctor);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("attendance", attendanceService.attendanceForDoctor(doctor.getId(), selectedDate));
        model.addAttribute("pageTitle", "My attendance | Chinche Hospital");
        model.addAttribute("activeView", "attendance");
        return "doctor/attendance";
    }

    @PostMapping("/attendance")
    public String updateAttendance(Authentication authentication,
                                   @RequestParam LocalDate date,
                                   @RequestParam(required = false) LocalTime arrival,
                                   @RequestParam(required = false) LocalTime departure,
                                   @RequestParam(name = "breakStart", required = false) List<LocalTime> breakStarts,
                                   @RequestParam(name = "breakEnd", required = false) List<LocalTime> breakEnds,
                                   @RequestParam(required = false) String notes,
                                   @RequestParam(required = false, defaultValue = "false") boolean confirmAffected,
                                   @RequestParam(name = "approvedAppointmentIds", required = false) List<Long> approvedIds,
                                   @RequestParam(required = false) String emergencyReason,
                                   Model model, RedirectAttributes flash) {
        Doctor doctor = signedInDoctor(authentication);
        List<LocalTime> starts = breakStarts == null ? List.of() : breakStarts;
        List<LocalTime> ends = breakEnds == null ? List.of() : breakEnds;
        try {
            if (confirmAffected) {
                int affected = attendanceService.confirmAttendanceChange(doctor.getId(), date, arrival, departure,
                        starts, ends, notes, approvedIds, emergencyReason);
                flash.addFlashAttribute("success", "Attendance saved. " + affected
                        + " affected patient(s) were added to urgent reschedules and notified.");
            } else {
                List<Appointment> conflicts = attendanceService.attendanceConflicts(
                        doctor.getId(), date, arrival, departure, starts, ends);
                if (!conflicts.isEmpty()) {
                    confirmationModel(model, doctor, date, arrival, departure, starts, ends, notes, conflicts, null);
                    return "doctor/attendance-confirmation";
                }
                attendanceService.saveAttendance(doctor.getId(), date, arrival, departure, starts, ends, notes);
                flash.addFlashAttribute("success", "Attendance and breaks saved.");
            }
        } catch (IllegalArgumentException ex) {
            if (confirmAffected) {
                List<Appointment> conflicts = attendanceService.attendanceConflicts(
                        doctor.getId(), date, arrival, departure, starts, ends);
                if (!conflicts.isEmpty()) {
                    confirmationModel(model, doctor, date, arrival, departure, starts, ends, notes,
                            conflicts, ex.getMessage());
                    return "doctor/attendance-confirmation";
                }
            }
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/doctor/attendance?date=" + date;
    }

    private Doctor signedInDoctor(Authentication authentication) {
        if (authentication == null) throw new AccessDeniedException("Doctor sign-in required.");
        AppUser account = users.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Doctor account is unavailable."));
        if (account.getDoctor() == null) {
            throw new AccessDeniedException("This doctor account is not linked to a doctor profile.");
        }
        return account.getDoctor();
    }

    private static void confirmationModel(Model model, Doctor doctor, LocalDate date,
                                         LocalTime arrival, LocalTime departure,
                                         List<LocalTime> starts, List<LocalTime> ends,
                                         String notes, List<Appointment> conflicts, String error) {
        model.addAttribute("doctor", doctor);
        model.addAttribute("doctorId", doctor.getId());
        model.addAttribute("date", date);
        model.addAttribute("arrival", arrival);
        model.addAttribute("departure", departure);
        model.addAttribute("breakStarts", starts);
        model.addAttribute("breakEnds", ends);
        model.addAttribute("notes", notes);
        model.addAttribute("affectedAppointments", conflicts);
        model.addAttribute("hasCheckedIn", conflicts.stream()
                .anyMatch(appointment -> appointment.getStatus() == AppointmentStatus.CHECKED_IN));
        model.addAttribute("error", error);
        model.addAttribute("pageTitle", "Confirm attendance update | Chinche Hospital");
        model.addAttribute("activeView", "attendance");
    }
}
