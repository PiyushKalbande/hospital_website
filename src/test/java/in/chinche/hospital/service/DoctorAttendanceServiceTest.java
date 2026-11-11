package in.chinche.hospital.service;

import in.chinche.hospital.domain.Appointment;
import in.chinche.hospital.domain.Doctor;
import in.chinche.hospital.domain.DoctorAttendance;
import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.repository.AppointmentRepository;
import in.chinche.hospital.repository.DoctorAttendanceRepository;
import in.chinche.hospital.repository.DoctorLeaveRepository;
import in.chinche.hospital.repository.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorAttendanceServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 5);

    @Mock private DoctorRepository doctors;
    @Mock private DoctorAttendanceRepository attendance;
    @Mock private DoctorLeaveRepository leaves;
    @Mock private AppointmentRepository appointments;
    @Mock private NotificationService notifications;
        @Mock private WhatsAppNotificationService whatsAppNotifications;
    @Mock private AppointmentUpdatePublisher updates;

    private DoctorAttendanceService service;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        service = new DoctorAttendanceService(
                doctors, attendance, leaves, appointments, notifications, whatsAppNotifications, updates);
        doctor = Doctor.builder().name("Dr. Anand Chinche").build();
        doctor.setId(7L);
        lenient().when(doctors.findById(7L)).thenReturn(Optional.of(doctor));
        lenient().when(attendance.findByDoctorIdAndAttendanceDate(7L, DATE)).thenReturn(Optional.empty());
        lenient().when(appointments.findByDoctorIdAndAppointmentDateAndStatusInOrderBySlotStartAsc(
                eq(7L), eq(DATE), anyList())).thenReturn(List.of());
    }

    @Test
    void rejectsRoutineBreakThatOverlapsConfirmedBooking() {
        Appointment booking = appointment(AppointmentStatus.CONFIRMED, LocalTime.of(15, 0), LocalTime.of(15, 20));
        when(appointments.findByDoctorIdAndAppointmentDateAndStatusInOrderBySlotStartAsc(
                eq(7L), eq(DATE), anyList())).thenReturn(List.of(booking));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveAttendance(7L, DATE, null, null,
                        List.of(LocalTime.of(15, 0)), List.of(LocalTime.of(15, 30)), null));

        assertTrue(error.getMessage().contains("Review affected patients"));
        verify(attendance, never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void emergencyBreakReschedulesOverlappingBookingsAndQueuesPatientNotices() {
        Appointment booking = appointment(AppointmentStatus.CONFIRMED, LocalTime.of(15, 0), LocalTime.of(15, 20));
                when(booking.getId()).thenReturn(51L);
                when(booking.getStatus()).thenReturn(AppointmentStatus.CONFIRMED);
                when(booking.getInternalNotes()).thenReturn(null);
        when(appointments.findByDoctorIdAndAppointmentDateAndStatusInOrderBySlotStartAsc(
                eq(7L), eq(DATE), anyList())).thenReturn(List.of(booking));

        List<Appointment> preview = service.attendanceConflicts(7L, DATE, null, null,
                List.of(LocalTime.of(15, 0)), List.of(LocalTime.of(15, 30)));
        assertEquals(List.of(booking), preview);

        int affected = service.confirmAttendanceChange(7L, DATE, null, null,
                List.of(LocalTime.of(15, 0)), List.of(LocalTime.of(15, 30)), null,
                List.of(51L), "Doctor called to emergency");

        assertEquals(1, affected);
        verify(booking).setStatus(AppointmentStatus.RESCHEDULED);
        verify(booking).setRescheduleReason("Doctor emergency; patient must choose a new appointment time.");
        verify(booking).setInternalNotes("Doctor called to emergency");
        verify(appointments).saveAll(List.of(booking));
        verify(attendance).save(any(DoctorAttendance.class));
        verify(notifications).sendDoctorEmergencyReschedule(booking);
        verify(whatsAppNotifications).sendDoctorEmergencyReschedule(booking);
        verify(updates).publishAvailabilityChangeAfterCommit();
        verify(updates).publishAppointmentChangeAfterCommit();
    }

    @Test
    void refusesToAutomaticallyReschedulePatientAlreadyCheckedIn() {
        Appointment booking = appointment(AppointmentStatus.CHECKED_IN, LocalTime.of(15, 0), LocalTime.of(15, 20));
                when(booking.getId()).thenReturn(51L);
                when(booking.getStatus()).thenReturn(AppointmentStatus.CHECKED_IN);
        when(appointments.findByDoctorIdAndAppointmentDateAndStatusInOrderBySlotStartAsc(
                eq(7L), eq(DATE), anyList())).thenReturn(List.of(booking));

        assertThrows(IllegalArgumentException.class, () -> service.confirmAttendanceChange(7L, DATE,
                null, null, List.of(LocalTime.of(15, 0)), List.of(LocalTime.of(15, 30)), null,
                List.of(51L), "Emergency"));

        verify(attendance, never()).save(any());
        verify(appointments, never()).saveAll(anyList());
        verifyNoInteractions(notifications);
    }

        @Test
        void rejectsConfirmationWhenAffectedAppointmentSetChangedAfterPreview() {
                Appointment booking = appointment(AppointmentStatus.CONFIRMED, LocalTime.of(15, 0), LocalTime.of(15, 20));
                when(appointments.findByDoctorIdAndAppointmentDateAndStatusInOrderBySlotStartAsc(
                                eq(7L), eq(DATE), anyList())).thenReturn(List.of(booking));

                assertThrows(IllegalArgumentException.class, () -> service.confirmAttendanceChange(7L, DATE,
                                null, null, List.of(LocalTime.of(15, 0)), List.of(LocalTime.of(15, 30)), null,
                                List.of(999L), "Emergency"));

                verify(attendance, never()).save(any());
                verify(appointments, never()).saveAll(anyList());
                verifyNoInteractions(notifications);
        }

    private static Appointment appointment(AppointmentStatus status, LocalTime start, LocalTime end) {
        Appointment appointment = mock(Appointment.class);
        when(appointment.getSlotStart()).thenReturn(start);
        when(appointment.getSlotEnd()).thenReturn(end);
        return appointment;
    }
}