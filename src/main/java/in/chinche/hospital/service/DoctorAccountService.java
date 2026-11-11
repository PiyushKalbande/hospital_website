package in.chinche.hospital.service;

import in.chinche.hospital.domain.AppUser;
import in.chinche.hospital.domain.Doctor;
import in.chinche.hospital.domain.enums.UserRole;
import in.chinche.hospital.exception.ResourceNotFoundException;
import in.chinche.hospital.repository.AppUserRepository;
import in.chinche.hospital.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorAccountService {

    private final AppUserRepository users;
    private final DoctorRepository doctors;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String createAccount(Long doctorId, String username, String initialPassword) {
        Doctor doctor = doctors.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        String normalizedUsername = username == null ? "" : username.strip();
        if (normalizedUsername.isBlank() || normalizedUsername.length() > 60) {
            throw new IllegalArgumentException("Username must be between 1 and 60 characters.");
        }
        if (initialPassword == null || initialPassword.length() < 12 || initialPassword.length() > 72) {
            throw new IllegalArgumentException("Initial password must be between 12 and 72 characters.");
        }
        if (users.existsByDoctorId(doctorId)) {
            throw new IllegalArgumentException("This doctor already has a login account.");
        }
        if (users.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("That username is already in use.");
        }

        users.save(AppUser.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode(initialPassword))
                .fullName(doctor.getName())
                .email(null)
                .role(UserRole.ROLE_DOCTOR)
                .enabled(true)
                .doctor(doctor)
                .build());
        return normalizedUsername;
    }

    @Transactional(readOnly = true)
    public Map<Long, String> usernamesForDoctors(Collection<Long> doctorIds) {
        if (doctorIds == null || doctorIds.isEmpty()) return Map.of();
        return users.findAllByDoctorIdIn(doctorIds).stream()
                .filter(user -> user.getDoctor() != null)
                .collect(Collectors.toMap(user -> user.getDoctor().getId(), AppUser::getUsername));
    }
}