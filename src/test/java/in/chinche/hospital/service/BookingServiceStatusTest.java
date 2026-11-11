package in.chinche.hospital.service;

import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.domain.Appointment;
import in.chinche.hospital.domain.Doctor;
import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.domain.enums.VisitMode;
import in.chinche.hospital.exception.SlotUnavailableException;
import in.chinche.hospital.repository.AppointmentRepository;
import in.chinche.hospital.repository.DoctorRepository;
import in.chinche.hospital.repository.FacilityRepository;
import in.chinche.hospital.repository.HealthPackageRepository;
import in.chinche.hospital.repository.PatientRepository;
import in.chinche.hospital.repository.SpecialityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceStatusTest {

    @Mock private AppointmentRepository appointments;
    @Mock private PatientRepository patients;
    @Mock private DoctorRepository doctors;
    @Mock private FacilityRepository facilities;
    @Mock private SpecialityRepository specialities;
    @Mock private HealthPackageRepository packages;
    @Mock private SlotAvailabilityService availability;
    @Mock private NotificationService notifications;
    @Mock private AppointmentUpdatePublisher appointmentUpdates;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(appointments, patients, doctors, facilities,
                specialities, packages, availability, notifications, appointmentUpdates, new AppProperties());
    }

    @Test
    void sendsPatientNotificationAfterPersistingAChangedStatus() {
        Appointment appointment = mock(Appointment.class);
        when(appointments.findById(17L)).thenReturn(Optional.of(appointment));
        when(appointment.getStatus()).thenReturn(AppointmentStatus.PENDING);
        when(appointments.saveAndFlush(appointment)).thenReturn(appointment);

        bookingService.updateStatus(17L, AppointmentStatus.CONFIRMED, "Reviewed by reception");

        verify(appointment).setStatus(AppointmentStatus.CONFIRMED);
        verify(appointment).setInternalNotes("Reviewed by reception");
        verify(appointments).saveAndFlush(appointment);
        verify(notifications).sendStatusUpdate(appointment);
        verify(appointmentUpdates).publishAppointmentChangeAfterCommit();
    }

    @Test
    void doesNotSaveOrSendDuplicateEmailWhenStatusIsUnchanged() {
        Appointment appointment = mock(Appointment.class);
        when(appointments.findById(17L)).thenReturn(Optional.of(appointment));
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CONFIRMED);

        bookingService.updateStatus(17L, AppointmentStatus.CONFIRMED, null);

        verify(appointments, never()).save(any());
        verifyNoInteractions(notifications);
        verifyNoInteractions(appointmentUpdates);
    }

    @Test
    void refusesToRestoreAnAppointmentIntoASlotThatHasBeenRebooked() {
        Appointment appointment = mock(Appointment.class);
        Doctor doctor = mock(Doctor.class);
        when(appointments.findById(17L)).thenReturn(Optional.of(appointment));
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CANCELLED);
        when(appointment.getDoctor()).thenReturn(doctor);
        when(doctor.getId()).thenReturn(9L);
        when(appointment.getAppointmentDate()).thenReturn(LocalDate.of(2026, 10, 31));
        when(appointment.getSlotStart()).thenReturn(LocalTime.of(9, 20));
        when(appointment.getVisitMode()).thenReturn(VisitMode.IN_HOSPITAL);
        when(availability.isSlotOpen(9L, LocalDate.of(2026, 10, 31),
                LocalTime.of(9, 20), VisitMode.IN_HOSPITAL)).thenReturn(false);

        assertThrows(SlotUnavailableException.class,
                () -> bookingService.updateStatus(17L, AppointmentStatus.CONFIRMED, null));

        verify(appointments, never()).saveAndFlush(any());
        verifyNoInteractions(notifications, appointmentUpdates);
    }
}