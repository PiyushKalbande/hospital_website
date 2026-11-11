package in.chinche.hospital.domain;

import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.domain.enums.VisitMode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppointmentSlotKeyTest {

    @Test
    void activeAppointmentsClaimTheirSlotAndCancelledAppointmentsReleaseIt() {
        Doctor doctor = mock(Doctor.class);
        when(doctor.getId()).thenReturn(17L);
        Appointment appointment = Appointment.builder()
                .doctor(doctor)
                .appointmentDate(LocalDate.of(2026, 10, 31))
                .slotStart(LocalTime.of(9, 20))
                .visitMode(VisitMode.IN_HOSPITAL)
                .status(AppointmentStatus.PENDING)
                .build();

        appointment.synchronizeActiveSlotKey();
        assertEquals("17|2026-10-31|09:20:00|IN_HOSPITAL", appointment.getActiveSlotKey());

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.synchronizeActiveSlotKey();
        assertNull(appointment.getActiveSlotKey());
    }

    @Test
    void noShowAppointmentsDoNotReserveTheirSlot() {
        Doctor doctor = mock(Doctor.class);
        when(doctor.getId()).thenReturn(17L);
        Appointment appointment = Appointment.builder()
                .doctor(doctor)
                .appointmentDate(LocalDate.of(2026, 10, 31))
                .slotStart(LocalTime.of(9, 20))
                .visitMode(VisitMode.IN_HOSPITAL)
                .status(AppointmentStatus.NO_SHOW)
                .build();

        appointment.synchronizeActiveSlotKey();

        assertNull(appointment.getActiveSlotKey());
    }
}
