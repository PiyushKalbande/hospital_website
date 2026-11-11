package in.chinche.hospital.web;

import in.chinche.hospital.domain.AppUser;
import in.chinche.hospital.domain.Doctor;
import in.chinche.hospital.domain.enums.UserRole;
import in.chinche.hospital.repository.AppUserRepository;
import in.chinche.hospital.repository.AppointmentRepository;
import in.chinche.hospital.service.DoctorAttendanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorPortalControllerTest {

    @Mock private AppUserRepository users;
    @Mock private AppointmentRepository appointments;
    @Mock private DoctorAttendanceService attendance;

    private DoctorPortalController controller;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        controller = new DoctorPortalController(users, appointments, attendance);
        doctor = Doctor.builder().name("Dr. Anand Chinche").build();
        doctor.setId(41L);
    }

    @Test
    void scopesAppointmentQueryToDoctorLinkedToAuthenticatedUser() {
        AppUser account = AppUser.builder().username("dr-anand").role(UserRole.ROLE_DOCTOR)
                .enabled(true).doctor(doctor).build();
        when(users.findByUsername("dr-anand")).thenReturn(Optional.of(account));
        when(appointments.findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAscSlotStartAsc(
                eq(41L), any(), any())).thenReturn(List.of());

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.appointments(
                new UsernamePasswordAuthenticationToken("dr-anand", "", List.of()), null, null, model);

        assertEquals("doctor/appointments", view);
        assertSame(doctor, model.get("doctor"));
        verify(appointments).findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAscSlotStartAsc(
                eq(41L), any(), any());
    }

    @Test
    void deniesDoctorAccountWithoutLinkedDoctorProfile() {
        AppUser account = AppUser.builder().username("orphan-doctor").role(UserRole.ROLE_DOCTOR)
                .enabled(true).build();
        when(users.findByUsername("orphan-doctor")).thenReturn(Optional.of(account));

        assertThrows(AccessDeniedException.class, () -> controller.dashboard(
                new UsernamePasswordAuthenticationToken("orphan-doctor", "", List.of()), new ExtendedModelMap()));
        verifyNoInteractions(appointments, attendance);
    }
}
