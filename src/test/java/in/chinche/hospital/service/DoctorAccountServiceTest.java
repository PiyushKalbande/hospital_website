package in.chinche.hospital.service;

import in.chinche.hospital.domain.AppUser;
import in.chinche.hospital.domain.Doctor;
import in.chinche.hospital.domain.enums.UserRole;
import in.chinche.hospital.repository.AppUserRepository;
import in.chinche.hospital.repository.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorAccountServiceTest {

    @Mock private AppUserRepository users;
    @Mock private DoctorRepository doctors;
    @Mock private PasswordEncoder passwordEncoder;

    private DoctorAccountService service;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        service = new DoctorAccountService(users, doctors, passwordEncoder);
        doctor = Doctor.builder().name("Dr. Anand Chinche").slug("dr-anand-chinche").build();
        doctor.setId(7L);
        lenient().when(doctors.findById(7L)).thenReturn(Optional.of(doctor));
        lenient().when(users.existsByDoctorId(7L)).thenReturn(false);
        lenient().when(users.existsByUsername("dr-anand")).thenReturn(false);
    }

    @Test
    void createsDoctorRoleAccountWithEncodedPasswordAndDoctorLink() {
        when(passwordEncoder.encode("Secure-Temporary-123")).thenReturn("bcrypt-hash");

        String username = service.createAccount(7L, " dr-anand ", "Secure-Temporary-123");

        assertEquals("dr-anand", username);
        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(saved.capture());
        assertEquals(UserRole.ROLE_DOCTOR, saved.getValue().getRole());
        assertEquals(doctor, saved.getValue().getDoctor());
        assertEquals("bcrypt-hash", saved.getValue().getPassword());
        assertNotEquals("Secure-Temporary-123", saved.getValue().getPassword());
    }

    @Test
    void rejectsShortInitialPasswords() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createAccount(7L, "dr-anand", "short"));
        verify(users, never()).save(any());
    }
}