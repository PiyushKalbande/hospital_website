package in.chinche.hospital.repository;

import in.chinche.hospital.domain.Appointment;
import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.domain.enums.VisitMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByReferenceNo(String referenceNo);

    List<Appointment> findByPatientIdOrderByAppointmentDateDesc(Long patientId);

    List<Appointment> findByAppointmentDateOrderBySlotStartAsc(LocalDate date);

    Page<Appointment> findByStatusOrderByCreatedAtDesc(AppointmentStatus status, Pageable pageable);

    Page<Appointment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Slots still considered "taken" for a doctor on a date (cancellations free the slot). */
    @Query("""
            select a.slotStart from Appointment a
            where a.doctor.id = :doctorId
              and a.appointmentDate = :date
              and a.visitMode = :mode
              and a.status in :blocking
            """)
    List<LocalTime> findBookedSlotStarts(@Param("doctorId") Long doctorId,
                                         @Param("date") LocalDate date,
                                         @Param("mode") VisitMode mode,
                                         @Param("blocking") List<AppointmentStatus> blocking);

    boolean existsByDoctorIdAndAppointmentDateAndSlotStartAndVisitModeAndStatusNotIn(
            Long doctorId, LocalDate date, LocalTime slotStart, VisitMode mode, List<AppointmentStatus> excluded);

    long countByAppointmentDate(LocalDate date);

    long countByStatus(AppointmentStatus status);

    @Query("select count(a) from Appointment a where a.createdAt >= :since")
    long countSince(@Param("since") java.time.Instant since);

    /** Daily booking volume for the admin dashboard sparkline. */
    @Query("""
            select a.appointmentDate, count(a) from Appointment a
            where a.appointmentDate between :from and :to
            group by a.appointmentDate order by a.appointmentDate
            """)
    List<Object[]> countGroupedByDate(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select s.name, count(a) from Appointment a
            join a.speciality s group by s.name order by count(a) desc
            """)
    List<Object[]> countGroupedBySpeciality();
}
