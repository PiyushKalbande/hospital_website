package in.chinche.hospital.service;

import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.domain.Doctor;
import in.chinche.hospital.domain.DoctorAttendance;
import in.chinche.hospital.domain.DoctorAttendanceBreak;
import in.chinche.hospital.domain.DoctorLeave;
import in.chinche.hospital.domain.DoctorSchedule;
import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.domain.enums.VisitMode;
import in.chinche.hospital.dto.response.DayAvailabilityDto;
import in.chinche.hospital.dto.response.SlotDto;
import in.chinche.hospital.exception.ResourceNotFoundException;
import in.chinche.hospital.repository.AppointmentRepository;
import in.chinche.hospital.repository.DoctorAttendanceRepository;
import in.chinche.hospital.repository.DoctorLeaveRepository;
import in.chinche.hospital.repository.DoctorRepository;
import in.chinche.hospital.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Derives bookable slots from recurring weekly schedules.
 *
 * <p>Algorithm per (doctor, date, visitMode):</p>
 * <ol>
 *   <li>Reject past dates and dates beyond {@code chinche.booking.advance-days}.</li>
 *   <li>Reject blackout dates present in {@code doctor_leave}.</li>
 *   <li>Load schedule rows for that weekday (teleconsult rows only serve VIDEO mode).</li>
 *   <li>Window each row into {@code slotMinutes}-long slots; drop any that end before "now".</li>
 *   <li>Remove slot starts already booked (cancellations and no-shows release the slot).</li>
 * </ol>
 *
 * <p>Nothing is persisted here — this is a pure read model, so it stays cheap enough
 * to call on every keystroke of the calendar strip.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlotAvailabilityService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final List<AppointmentStatus> BLOCKING = List.of(
            AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN,
            AppointmentStatus.COMPLETED, AppointmentStatus.RESCHEDULED);

    private final DoctorRepository doctors;
    private final DoctorScheduleRepository schedules;
    private final DoctorLeaveRepository leaves;
    private final DoctorAttendanceRepository attendance;
    private final AppointmentRepository appointments;
    private final AppProperties props;

    public LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public LocalDate lastBookableDate() {
        return today().plusDays(props.getBooking().getAdvanceDays());
    }

    /** The next {@code days} days starting today, with slot counts for the date strip. */
    public List<DayAvailabilityDto> availability(Long doctorId, LocalDate from, int days, VisitMode mode) {
        Doctor doctor = doctors.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));

        LocalDate start = clamp(from);
        int span = Math.max(1, Math.min(days, props.getBooking().getAdvanceDays() + 1));

        Set<LocalDate> blackout = new HashSet<>();
        for (DoctorLeave leave : leaves.findByDoctorIdAndLeaveDateBetween(doctorId, start, start.plusDays(span))) {
            blackout.add(leave.getLeaveDate());
        }

        Map<DayOfWeek, List<DoctorSchedule>> weekly = new EnumMap<>(DayOfWeek.class);
        for (DoctorSchedule s : schedules.findByDoctorIdAndActiveTrue(doctorId)) {
            weekly.computeIfAbsent(s.getDayOfWeek(), d -> new ArrayList<>()).add(s);
        }

        List<DayAvailabilityDto> result = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            LocalDate date = start.plusDays(i);
            if (date.isAfter(lastBookableDate())) break;
            result.add(buildDay(doctor, date, weekly, blackout, mode));
        }
        return result;
    }

    /** Availability for one specific date — used when the widget re-validates before confirm. */
    public DayAvailabilityDto availabilityForDate(Long doctorId, LocalDate date, VisitMode mode) {
        Doctor doctor = doctors.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        LocalDate target = clamp(date);
        Set<LocalDate> blackout = new HashSet<>();
        if (leaves.existsByDoctorIdAndLeaveDate(doctorId, target)) blackout.add(target);
        Map<DayOfWeek, List<DoctorSchedule>> weekly = new EnumMap<>(DayOfWeek.class);
        for (DoctorSchedule s : schedules.findByDoctorIdAndActiveTrue(doctorId)) {
            weekly.computeIfAbsent(s.getDayOfWeek(), d -> new ArrayList<>()).add(s);
        }
        return buildDay(doctor, target, weekly, blackout, mode);
    }

    private DayAvailabilityDto buildDay(Doctor doctor, LocalDate date,
                                        Map<DayOfWeek, List<DoctorSchedule>> weekly,
                                        Set<LocalDate> blackout, VisitMode mode) {
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

        if (blackout.contains(date)) {
            return new DayAvailabilityDto(date, dayName, false, "Doctor unavailable on this date", List.of());
        }
        if (date.isBefore(today())) {
            return new DayAvailabilityDto(date, dayName, false, "Date has passed", List.of());
        }

        List<DoctorSchedule> rows = weekly.getOrDefault(date.getDayOfWeek(), List.of());
        if (rows.isEmpty()) {
            return new DayAvailabilityDto(date, dayName, false, "No clinic hours on " + dayName, List.of());
        }

        Set<LocalTime> booked = new HashSet<>(appointments.findBookedSlotStarts(doctor.getId(), date, mode, BLOCKING));
        List<DoctorAttendanceBreak> awayIntervals = attendance.findByDoctorIdAndAttendanceDate(doctor.getId(), date)
                .map(DoctorAttendance::getBreaks).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .filter(b -> b.getStartTime() != null && b.getEndTime() != null)
                .toList();
        LocalDateTime now = LocalDateTime.now(ZONE);
        List<SlotDto> slots = new ArrayList<>();

        for (DoctorSchedule row : rows) {
            boolean teleRow = row.isTeleconsult();
            if (mode == VisitMode.VIDEO && !teleRow) continue;
            if (mode != VisitMode.VIDEO && teleRow) continue;

            int step = row.getSlotMinutes() != null && row.getSlotMinutes() > 0
                    ? row.getSlotMinutes() : props.getBooking().getSlotMinutes();

            LocalTime cursor = row.getStartTime();
            while (cursor.plusMinutes(step).compareTo(row.getEndTime()) <= 0) {
                LocalTime end = cursor.plusMinutes(step);

                // FIX: Stream ke liye ek 'effectively final' variable banayein
                final LocalTime currentSlotStart = cursor;

                boolean available = !booked.contains(currentSlotStart)
                    && awayIntervals.stream().noneMatch(b -> overlaps(currentSlotStart, end, b)); // Yahan naya variable use kiya

                if (available && date.isEqual(today()) && LocalDateTime.of(date, end).isBefore(now.plusMinutes(30))) {
                    available = false;   // slot ends too soon to be usable
                }
                slots.add(new SlotDto(currentSlotStart, end, available, SlotDto.periodOf(currentSlotStart)));

                // Ab aap bina kisi error ke cursor ko aage badha sakte hain
                cursor = end;
            }

        }

        slots.sort(Comparator.comparing(SlotDto::start));
        boolean anyOpen = slots.stream().anyMatch(SlotDto::available);
        String reason = anyOpen
                ? (awayIntervals.isEmpty() ? null : "Some slots are unavailable during the doctor's break.")
                : (awayIntervals.isEmpty() ? "All slots fully booked for this date"
                : "Doctor is unavailable during the recorded break or fully booked.");
        return new DayAvailabilityDto(date, dayName, anyOpen, reason, slots);
    }

    private boolean overlaps(LocalTime slotStart, LocalTime slotEnd, DoctorAttendanceBreak interval) {
        return interval != null && interval.getStartTime() != null && interval.getEndTime() != null
                && interval.getStartTime().isBefore(slotEnd) && interval.getEndTime().isAfter(slotStart);
    }

    private LocalDate clamp(LocalDate date) {
        LocalDate from = date == null ? today() : date;
        if (from.isBefore(today())) from = today();
        LocalDate max = lastBookableDate();
        return from.isAfter(max) ? max : from;
    }

    /** True when a slot can still be taken right now — re-checked inside the booking transaction. */
    public boolean isSlotOpen(Long doctorId, LocalDate date, LocalTime slotStart, VisitMode mode) {
        DayAvailabilityDto day = availabilityForDate(doctorId, date, mode);
        return day.slots().stream()
                .anyMatch(s -> s.start().equals(slotStart) && s.available());
    }
}
