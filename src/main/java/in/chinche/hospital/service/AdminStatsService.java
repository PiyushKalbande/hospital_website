package in.chinche.hospital.service;

import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Aggregations for the admin dashboard widgets. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

    private final AppointmentRepository appointments;
    private final DoctorRepository doctors;
    private final EnquiryRepository enquiries;
    private final SpecialityRepository specialities;
    private final FacilityRepository facilities;

    public Map<String, Object> overview() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("appointmentsToday", appointments.countByAppointmentDate(LocalDate.now()));
        stats.put("appointmentsTomorrow", appointments.countByAppointmentDate(LocalDate.now().plusDays(1)));
        stats.put("pending", appointments.countByStatus(AppointmentStatus.PENDING));
        stats.put("confirmed", appointments.countByStatus(AppointmentStatus.CONFIRMED));
        stats.put("completed", appointments.countByStatus(AppointmentStatus.COMPLETED));
        stats.put("cancelled", appointments.countByStatus(AppointmentStatus.CANCELLED));
        stats.put("totalAppointments", appointments.count());
        stats.put("last24h", appointments.countSince(Instant.now().minus(24, ChronoUnit.HOURS)));
        stats.put("doctors", doctors.countByActiveTrue());
        stats.put("specialities", specialities.count());
        stats.put("facilities", facilities.count());
        stats.put("openEnquiries", enquiries.countByStatus("NEW"));
        return stats;
    }

    /** Booking volume per day for the last {@code days} days, zero-filled for chart continuity. */
    public List<Map<String, Object>> dailyTrend(int days) {
        LocalDate from = LocalDate.now().minusDays(days - 1L);
        Map<LocalDate, Long> counts = new HashMap<>();
        for (Object[] row : appointments.countGroupedByDate(from, LocalDate.now())) {
            counts.put((LocalDate) row[0], (Long) row[1]);
        }
        List<Map<String, Object>> series = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate d = from.plusDays(i);
            series.add(Map.of("date", d.toString(), "count", counts.getOrDefault(d, 0L)));
        }
        return series;
    }

    public List<Map<String, Object>> bySpeciality() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] r : appointments.countGroupedBySpeciality()) {
            rows.add(Map.of("speciality", r[0], "count", r[1]));
        }
        return rows;
    }

    public List<AppointmentStatus> statusValues() {
        return List.of(AppointmentStatus.values());
    }
}
