package in.chinche.hospital.repository;

import in.chinche.hospital.domain.DoctorLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, Long> {

    List<DoctorLeave> findByDoctorIdAndLeaveDateBetween(Long doctorId, LocalDate from, LocalDate to);

    boolean existsByDoctorIdAndLeaveDate(Long doctorId, LocalDate date);
}
