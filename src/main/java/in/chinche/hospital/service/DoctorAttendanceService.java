package in.chinche.hospital.service;

import in.chinche.hospital.domain.Doctor;
import in.chinche.hospital.domain.DoctorAttendance;
import in.chinche.hospital.domain.DoctorAttendanceBreak;
import in.chinche.hospital.domain.DoctorLeave;
import in.chinche.hospital.repository.DoctorAttendanceRepository;
import in.chinche.hospital.repository.DoctorLeaveRepository;
import in.chinche.hospital.repository.DoctorRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public Map<Long, DoctorAttendance> attendanceFor(LocalDate date) {
        return attendance.findByAttendanceDate(date).stream()
            .map(row -> {
                row.setBreaks(row.getBreaks() == null ? new ArrayList<>() : row.getBreaks().stream()
                    .filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new)));
                return row;
            })
                .collect(Collectors.toMap(a -> a.getDoctor().getId(), Function.identity()));
    }

    public Map<Long, List<LocalDate>> leaveDates(LocalDate from, LocalDate to) {
        return leaves.findByLeaveDateBetween(from, to).stream()
                .collect(Collectors.groupingBy(l -> l.getDoctor().getId(),
                        Collectors.mapping(DoctorLeave::getLeaveDate, Collectors.toList())));
    }

    @Transactional
    public void saveAttendance(Long doctorId, LocalDate date, LocalTime arrival, LocalTime departure,
                               List<LocalTime> breakStarts, List<LocalTime> breakEnds, String notes) {
        validateTimes(arrival, departure, breakStarts, breakEnds);
        breakStarts = breakStarts == null ? List.of() : breakStarts;
        breakEnds = breakEnds == null ? List.of() : breakEnds;
        Doctor doctor = doctors.findById(doctorId).orElseThrow();
        DoctorAttendance row = attendance.findByDoctorIdAndAttendanceDate(doctorId, date)
                .orElseGet(() -> DoctorAttendance.builder().doctor(doctor).attendanceDate(date).build());
        row.setArrivalTime(arrival);
        row.setDepartureTime(departure);
        row.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        List<DoctorAttendanceBreak> breaks = new ArrayList<>();
        for (int i = 0; i < breakStarts.size(); i++) {
            breaks.add(DoctorAttendanceBreak.builder()
                    .startTime(breakStarts.get(i)).endTime(breakEnds.get(i)).build());
        }
        row.setBreaks(breaks);
        attendance.save(row);
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
}
