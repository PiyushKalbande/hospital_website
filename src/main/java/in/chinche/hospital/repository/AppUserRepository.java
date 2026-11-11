package in.chinche.hospital.repository;

import in.chinche.hospital.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByDoctorId(Long doctorId);
    List<AppUser> findAllByDoctorIdIn(Collection<Long> doctorIds);
    boolean existsByDoctorId(Long doctorId);
    boolean existsByUsername(String username);
}
