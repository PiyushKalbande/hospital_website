package in.chinche.hospital.repository;

import in.chinche.hospital.domain.DoctorAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DoctorAttendanceRepository extends JpaRepository<DoctorAttendance, Long> {

    Optional<DoctorAttendance> findByDoctorIdAndAttendanceDate(Long doctorId, LocalDate date);

    List<DoctorAttendance> findByAttendanceDate(LocalDate date);
}
