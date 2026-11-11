package in.chinche.hospital.ai;

import in.chinche.hospital.domain.Doctor;
import in.chinche.hospital.domain.DoctorAttendance;
import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.domain.enums.VisitMode;
import in.chinche.hospital.dto.response.DayAvailabilityDto;
import in.chinche.hospital.dto.response.SlotDto;
import in.chinche.hospital.repository.AppointmentRepository;
import in.chinche.hospital.repository.DoctorAttendanceRepository;
import in.chinche.hospital.repository.DoctorRepository;
import in.chinche.hospital.service.SlotAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LiveDoctorScheduleService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final List<AppointmentStatus> TODAY_PATIENT_STATUSES = List.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.CHECKED_IN,
            AppointmentStatus.COMPLETED
    );

    private final DoctorRepository doctors;
    private final DoctorAttendanceRepository attendance;
    private final AppointmentRepository appointments;
    private final SlotAvailabilityService availability;

    public Optional<LiveDoctorScheduleContext> resolve(
            String question,
            String language,
            boolean staffAccess
    ) {
        QueryIntent intent = QueryIntent.from(question);
        if (!intent.isLiveQuery()) return Optional.empty();

        String normalizedQuestion = normalize(question);
        List<Doctor> matches = doctors.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .filter(doctor -> normalizedQuestion.contains(normalizeDoctorName(doctor.getName())))
                .toList();
        if (matches.isEmpty() && !intent.requiresStaffAccess()) return Optional.empty();
        if (matches.size() != 1) {
            return Optional.of(missingDoctorContext(language, matches.size() > 1));
        }

        Doctor doctor = matches.get(0);
        AssistantDoctor doctorCard = doctorCard(doctor);
        String bookingUrl = doctorCard.bookingUrl();
        if (intent.requiresStaffAccess() && !staffAccess) {
            return Optional.of(staffOnlyContext(language));
        }

        LocalDate today = availability.today();
        DayAvailabilityDto day = availability.availabilityForDate(
                doctor.getId(), today, VisitMode.IN_HOSPITAL);
        List<SlotDto> openSlots = day.slots().stream().filter(SlotDto::available).toList();
        List<AssistantSource> sources = new ArrayList<>();
        sources.add(new AssistantSource(
                "live-doctor-availability", "Today's live booking availability",
                doctorCard.profileUrl(), "availability", true));

        StringBuilder context = new StringBuilder()
                .append("LIVE DATABASE SNAPSHOT. Read-only data loaded from the hospital database now.\n")
                .append("Date and timezone: ").append(today).append(", Asia/Kolkata.\n")
                .append("Doctor: ").append(doctor.getName()).append(".\n")
                .append("Public in-hospital clinic open today: ").append(day.clinicOpen()).append(".\n")
                .append("Public bookable slots remaining today: ")
                .append(openSlots.isEmpty() ? "none" : openSlots.stream()
                        .map(slot -> format(slot.start()) + "-" + format(slot.end()))
                        .reduce((first, second) -> first + ", " + second).orElse("none"))
                .append(".\n");
        StringBuilder fallback = new StringBuilder();
        long appointmentCount = 0;

        if (intent.breakTime()) {
            Optional<DoctorAttendance> record = attendance.findByDoctorIdAndAttendanceDate(
                    doctor.getId(), today);
            List<String> breaks = record.map(DoctorAttendance::getBreaks).orElse(List.of()).stream()
                    .filter(item -> item != null && item.getStartTime() != null && item.getEndTime() != null)
                    .map(item -> format(item.getStartTime()) + "-" + format(item.getEndTime()))
                    .toList();
            if (breaks.isEmpty()) {
                context.append("No break interval is recorded in today's attendance entry.\n");
            } else {
                context.append("Break intervals recorded in today's attendance: ")
                        .append(String.join(", ", breaks)).append(".\n");
            }
            sources.add(new AssistantSource(
                    "live-doctor-attendance", "Today's staff attendance",
                    "/admin/doctors", "staff-attendance", true));
            fallback.append("Today's recorded break for ").append(doctor.getName()).append(": ")
                    .append(breaks.isEmpty() ? "no break time is recorded in attendance."
                            : String.join(", ", breaks) + ".")
                    .append(' ');
        }

        if (intent.patientCount()) {
                        appointmentCount = appointments.countByDoctorIdAndAppointmentDateAndStatusIn(
                    doctor.getId(), today, TODAY_PATIENT_STATUSES);
                        context.append("Today's appointment count for this doctor: ").append(appointmentCount)
                    .append(". This is an appointment count, not a count of unique people. ")
                    .append("It includes pending, confirmed, checked-in, and completed appointments; ")
                    .append("it excludes cancelled, rescheduled, and no-show appointments.\n");
            sources.add(new AssistantSource(
                    "live-doctor-appointments", "Today's appointment totals",
                    "/admin/appointments", "appointments", true));
            fallback.append("There are ").append(appointmentCount).append(" scheduled appointments for ")
                    .append(doctor.getName()).append(" today. This counts appointments, not unique patients, ")
                    .append("and excludes cancelled, rescheduled, and no-show appointments. ");
        }

        if (intent.availability()) {
            if ("mr".equals(language)) {
                fallback.append(openSlots.isEmpty()
                        ? "आज डॉ. " + doctor.getName() + " यांच्यासाठी बुकिंगची वेळ उपलब्ध नाही."
                        : "हो. आज डॉ. " + doctor.getName() + " यांच्यासाठी " + openSlots.size()
                                + " बुकिंगच्या वेळा उपलब्ध आहेत.");
            } else {
                fallback.append(openSlots.isEmpty()
                        ? "No bookable in-hospital slots remain today for " + doctor.getName() + "."
                        : "Yes. " + doctor.getName() + " has " + openSlots.size()
                                + " bookable in-hospital slots remaining today.");
            }
        }
        if (intent.slotDetails() && !openSlots.isEmpty()) {
            fallback.append(" Available times: ").append(openSlots.stream()
                    .map(slot -> format(slot.start()) + "-" + format(slot.end()))
                    .reduce((first, second) -> first + ", " + second).orElse("none"))
                    .append('.');
        }

        return Optional.of(new LiveDoctorScheduleContext(
                context.toString(),
                fallback.toString().strip(),
                doctorCard,
                List.copyOf(sources),
                bookingUrl,
                true));
    }

    private static LiveDoctorScheduleContext missingDoctorContext(String language, boolean ambiguous) {
        String answer = "mr".equals(language)
                ? (ambiguous
                    ? "या नावाचे एकापेक्षा जास्त डॉक्टर सापडले. कृपया डॉक्टरांचे पूर्ण नाव लिहा."
                    : "या नावाचा सक्रिय डॉक्टर सापडला नाही. कृपया डॉक्टरांचे पूर्ण नाव तपासा.")
                : (ambiguous
                    ? "I found more than one doctor matching that name. Please enter the doctor's full name."
                    : "I couldn't find an active doctor with that name. Please check the doctor's full name.");
        return new LiveDoctorScheduleContext("", answer, null, List.of(), null, false);
    }

    private static LiveDoctorScheduleContext staffOnlyContext(String language) {
        String answer = "mr".equals(language)
                ? "आजचे ब्रेक आणि अपॉइंटमेंटची संख्या फक्त लॉग-इन केलेल्या Admin किंवा Receptionist ला दिसू शकते. कृपया आधी कर्मचारी खात्यातून लॉग-इन करा."
                : "Today's staff break times and appointment counts are private. Please sign in with an Admin or Receptionist account to view them.";
        return new LiveDoctorScheduleContext("", answer, null, List.of(), null, false);
    }

    private static AssistantDoctor doctorCard(Doctor doctor) {
        String slug = UriUtils.encodePathSegment(doctor.getSlug(), StandardCharsets.UTF_8);
        String speciality = doctor.getSpeciality() == null
                ? "Hospital doctor" : doctor.getSpeciality().getName();
        return new AssistantDoctor(
                doctor.getName(), speciality, "/doctors/" + slug, "/book?doctor=" + slug);
    }

    private static String normalizeDoctorName(String name) {
        return normalize(name).replaceFirst("^dr\\s+", "");
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .strip();
    }

    private static String format(java.time.LocalTime time) {
        return time.format(TIME_FORMAT);
    }

    private record QueryIntent(
            boolean availability,
            boolean breakTime,
            boolean patientCount,
            boolean slotDetails
    ) {
        private static QueryIntent from(String question) {
            String normalized = normalize(question);
            boolean availability = containsAny(normalized, List.of(
                    "available", "availability", "free slot", "free time", "open slot",
                    "time slot", "bookable", "khali", "खाली", "उपलब्ध",
                    "doctor aahe", "doctor ahe", "doctor aahet", "doctor ahet"));
            boolean breakTime = containsAny(normalized, List.of(
                    "break", "lunch", "rest time", "tea time", "ब्रेक", "विश्रांती"));
            boolean patientCount = containsAny(normalized, List.of(
                    "patient", "patients", "how many", "kitne", "कितने", "किती", "रुग्ण"));
            boolean slotDetails = containsAny(normalized, List.of(
                    "slot", "time", "timing", "what time", "which time", "kiti te kiti", "vel", "वेळ", "किती ते किती"));
            return new QueryIntent(availability, breakTime, patientCount, slotDetails);
        }

        private boolean isLiveQuery() {
            return availability || breakTime || patientCount;
        }

        private boolean requiresStaffAccess() {
            return breakTime || patientCount;
        }

        private static boolean containsAny(String text, List<String> phrases) {
            return phrases.stream().map(LiveDoctorScheduleService::normalize)
                    .anyMatch(text::contains);
        }
    }
}
