package in.chinche.hospital.service;

import in.chinche.hospital.domain.Doctor;
import in.chinche.hospital.domain.DoctorAttendance;
import in.chinche.hospital.domain.DoctorAttendanceBreak;
import in.chinche.hospital.domain.DoctorLeave;
import in.chinche.hospital.domain.Appointment;
import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.repository.AppointmentRepository;
import in.chinche.hospital.repository.DoctorAttendanceRepository;
import in.chinche.hospital.repository.DoctorLeaveRepository;
import in.chinche.hospital.repository.DoctorRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorAttendanceService {

    private final DoctorRepository doctors;
    private final DoctorAttendanceRepository attendance;
    private final DoctorLeaveRepository leaves;
        private final AppointmentRepository appointments;
        private final NotificationService notifications;
        private final WhatsAppNotificationService whatsAppNotifications;
    private final AppointmentUpdatePublisher appointmentUpdates;

        private static final List<AppointmentStatus> ACTIVE_BOOKINGS = List.of(
            AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN);

    public Map<Long, DoctorAttendance> attendanceFor(LocalDate date) {
        return attendance.findByAttendanceDate(date).stream()
            .map(row -> {
                row.setBreaks(row.getBreaks() == null ? new ArrayList<>() : row.getBreaks().stream()
                    .filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new)));
                return row;
            })
                .collect(Collectors.toMap(a -> a.getDoctor().getId(), Function.identity()));
    }

    public DoctorAttendance attendanceForDoctor(Long doctorId, LocalDate date) {
        return attendance.findByDoctorIdAndAttendanceDate(doctorId, date).orElse(null);
    }

    public Map<Long, List<LocalDate>> leaveDates(LocalDate from, LocalDate to) {
        return leaves.findByLeaveDateBetween(from, to).stream()
                .collect(Collectors.groupingBy(l -> l.getDoctor().getId(),
                        Collectors.mapping(DoctorLeave::getLeaveDate, Collectors.toList())));
    }

    @Transactional
    public void saveAttendance(Long doctorId, LocalDate date, LocalTime arrival, LocalTime departure,
                               List<LocalTime> breakStarts, List<LocalTime> breakEnds, String notes) {
        applyAttendanceChange(doctorId, date, arrival, departure, breakStarts, breakEnds,
            notes, null, null);
        }

        @Transactional(Transactional.TxType.SUPPORTS)
        public List<Appointment> attendanceConflicts(Long doctorId, LocalDate date, LocalTime arrival,
                              LocalTime departure, List<LocalTime> breakStarts,
                              List<LocalTime> breakEnds) {
        List<LocalTime> normalizedBreakStarts = breakStarts == null ? List.of() : breakStarts;
        List<LocalTime> normalizedBreakEnds = breakEnds == null ? List.of() : breakEnds;
        validateTimes(arrival, departure, normalizedBreakStarts, normalizedBreakEnds);
        return findAttendanceConflicts(doctorId, date, arrival, departure,
            normalizedBreakStarts, normalizedBreakEnds);
        }

        @Transactional
        public int confirmAttendanceChange(Long doctorId, LocalDate date, LocalTime arrival, LocalTime departure,
                           List<LocalTime> breakStarts, List<LocalTime> breakEnds,
                           String notes, List<Long> approvedAppointmentIds, String emergencyReason) {
        if (emergencyReason == null || emergencyReason.isBlank()) {
            throw new IllegalArgumentException("Enter a reason before notifying affected patients.");
        }
        return applyAttendanceChange(doctorId, date, arrival, departure, breakStarts, breakEnds,
            notes, approvedAppointmentIds, emergencyReason);
        }

        private int applyAttendanceChange(Long doctorId, LocalDate date, LocalTime arrival, LocalTime departure,
                          List<LocalTime> breakStarts, List<LocalTime> breakEnds, String notes,
                          List<Long> approvedAppointmentIds, String emergencyReason) {
        List<LocalTime> normalizedBreakStarts = breakStarts == null ? List.of() : breakStarts;
        List<LocalTime> normalizedBreakEnds = breakEnds == null ? List.of() : breakEnds;
        validateTimes(arrival, departure, normalizedBreakStarts, normalizedBreakEnds);
        Doctor doctor = doctors.findById(doctorId).orElseThrow();
        List<Appointment> impacted = findAttendanceConflicts(doctorId, date, arrival, departure,
            normalizedBreakStarts, normalizedBreakEnds);
        if (!impacted.isEmpty()) {
            if (approvedAppointmentIds == null) {
            throw new IllegalArgumentException(
                "Attendance change conflicts with active bookings. Review affected patients before confirming.");
            }
            if (!impacted.stream().map(Appointment::getId)
                .collect(Collectors.toSet()).equals(new java.util.HashSet<>(approvedAppointmentIds))) {
            throw new IllegalArgumentException(
                "Affected bookings changed since review. Review the current patient list and confirm again.");
            }
            if (impacted.stream().anyMatch(booking -> booking.getStatus() == AppointmentStatus.CHECKED_IN)) {
            throw new IllegalArgumentException(
                "A patient has already checked in during this interval. Please coordinate with them directly.");
            }
        } else if (approvedAppointmentIds != null && !approvedAppointmentIds.isEmpty()) {
            throw new IllegalArgumentException(
                "Affected bookings changed since review. Review the current patient list and confirm again.");
        }

        DoctorAttendance row = attendance.findByDoctorIdAndAttendanceDate(doctorId, date)
                .orElseGet(() -> DoctorAttendance.builder().doctor(doctor).attendanceDate(date).build());
        row.setArrivalTime(arrival);
        row.setDepartureTime(departure);
        row.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        List<DoctorAttendanceBreak> breaks = new ArrayList<>();
        for (int i = 0; i < normalizedBreakStarts.size(); i++) {
            breaks.add(DoctorAttendanceBreak.builder()
                .startTime(normalizedBreakStarts.get(i)).endTime(normalizedBreakEnds.get(i)).build());
        }
        row.setBreaks(breaks);
        attendance.save(row);
        for (Appointment appointment : impacted) {
            appointment.setStatus(AppointmentStatus.RESCHEDULED);
            appointment.setRescheduleReason("Doctor emergency; patient must choose a new appointment time.");
            appointment.setInternalNotes(appendStaffReason(appointment.getInternalNotes(), emergencyReason));
        }
        if (!impacted.isEmpty()) appointments.saveAll(impacted);
        appointmentUpdates.publishAvailabilityChangeAfterCommit();
        if (!impacted.isEmpty()) {
            appointmentUpdates.publishAppointmentChangeAfterCommit();
            afterCommit(() -> impacted.forEach(appointment -> {
                notifications.sendDoctorEmergencyReschedule(appointment);
                whatsAppNotifications.sendDoctorEmergencyReschedule(appointment);
            }));
        }
        return impacted.size();
    }

    @Transactional
    public int setLeave(Long doctorId, LocalDate from, LocalDate to, String reason) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Leave end date must be on or after the start date.");
        }
        Doctor doctor = doctors.findById(doctorId).orElseThrow();
        leaves.deleteByDoctorIdAndLeaveDateBetween(doctorId, from, to);
        List<DoctorLeave> rows = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            rows.add(DoctorLeave.builder().doctor(doctor).leaveDate(date)
                    .reason(reason == null || reason.isBlank() ? null : reason.trim()).build());
        }
        leaves.saveAll(rows);
        appointmentUpdates.publishAvailabilityChangeAfterCommit();
        return rows.size();
    }

    private void validateTimes(LocalTime arrival, LocalTime departure,
                               List<LocalTime> breakStarts, List<LocalTime> breakEnds) {
        if (arrival != null && departure != null && !departure.isAfter(arrival)) {
            throw new IllegalArgumentException("Departure time must be after arrival time.");
        }
        if (breakStarts == null) breakStarts = List.of();
        if (breakEnds == null) breakEnds = List.of();
        if (breakStarts.size() != breakEnds.size()) {
            throw new IllegalArgumentException("Every break must have a start and end time.");
        }
        for (int i = 0; i < breakStarts.size(); i++) {
            LocalTime start = breakStarts.get(i);
            LocalTime end = breakEnds.get(i);
            if (start == null || end == null || !end.isAfter(start)) {
                throw new IllegalArgumentException("Every break end time must be after its start time.");
            }
            if (arrival != null && start.isBefore(arrival) || departure != null && end.isAfter(departure)) {
                throw new IllegalArgumentException("Breaks must fall between arrival and departure.");
            }
        }
    }

    private static boolean conflictsWithAttendance(Appointment appointment, LocalTime arrival,
                                                  LocalTime departure, List<LocalTime> breakStarts,
                                                  List<LocalTime> breakEnds) {
        if (arrival != null && appointment.getSlotStart().isBefore(arrival)) return true;
        if (departure != null && appointment.getSlotEnd().isAfter(departure)) return true;
        for (int index = 0; index < breakStarts.size(); index++) {
            if (overlaps(appointment, breakStarts.get(index), breakEnds.get(index))) return true;
        }
        return false;
    }

    private List<Appointment> findAttendanceConflicts(Long doctorId, LocalDate date, LocalTime arrival,
                                                      LocalTime departure, List<LocalTime> breakStarts,
                                                      List<LocalTime> breakEnds) {
        return appointments.findByDoctorIdAndAppointmentDateAndStatusInOrderBySlotStartAsc(
                        doctorId, date, ACTIVE_BOOKINGS).stream()
                .filter(booking -> conflictsWithAttendance(booking, arrival, departure, breakStarts, breakEnds))
                .toList();
    }

    private static boolean overlaps(Appointment appointment, LocalTime intervalStart, LocalTime intervalEnd) {
        return appointment.getSlotStart().isBefore(intervalEnd)
                && appointment.getSlotEnd().isAfter(intervalStart);
    }

    private static boolean overlaps(LocalTime start, LocalTime end, LocalTime otherStart, LocalTime otherEnd) {
        return otherStart != null && otherEnd != null
                && otherStart.isBefore(end) && otherEnd.isAfter(start);
    }

    private static String appendStaffReason(String existing, String reason) {
        String cleaned = reason.trim();
        String notes = existing == null || existing.isBlank() ? cleaned : existing + "\n" + cleaned;
        return notes.length() <= 1000 ? notes : notes.substring(notes.length() - 1000);
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
