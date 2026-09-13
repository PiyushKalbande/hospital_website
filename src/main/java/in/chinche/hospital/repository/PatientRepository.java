package in.chinche.hospital.repository;

import in.chinche.hospital.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPhone(String phone);

    Optional<Patient> findByEmail(String email);

    boolean existsByPhone(String phone);
}
