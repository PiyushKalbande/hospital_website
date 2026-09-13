package in.chinche.hospital.repository;

import in.chinche.hospital.domain.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctorIdAndActiveTrue(Long doctorId);

    List<DoctorSchedule> findByDoctorIdAndDayOfWeekAndActiveTrueAndTeleconsultIsFalse(Long doctorId, DayOfWeek day);

    List<DoctorSchedule> findByDoctorIdAndDayOfWeekAndActiveTrue(Long doctorId, DayOfWeek day);

    List<DoctorSchedule> findByFacilityIdAndActiveTrue(Long facilityId);

    void deleteByDoctorId(Long doctorId);
}
