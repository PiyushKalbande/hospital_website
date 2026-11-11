package in.chinche.hospital.service;

import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.domain.Doctor;
import in.chinche.hospital.domain.DoctorAttendance;
import in.chinche.hospital.domain.DoctorAttendanceBreak;
import in.chinche.hospital.domain.DoctorSchedule;
import in.chinche.hospital.domain.enums.VisitMode;
import in.chinche.hospital.repository.AppointmentRepository;
import in.chinche.hospital.repository.DoctorAttendanceRepository;
import in.chinche.hospital.repository.DoctorLeaveRepository;
import in.chinche.hospital.repository.DoctorRepository;
import in.chinche.hospital.repository.DoctorScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotAvailabilityAttendanceTest {

    @Mock private DoctorRepository doctors;
    @Mock private DoctorScheduleRepository schedules;
    @Mock private DoctorLeaveRepository leaves;
    @Mock private DoctorAttendanceRepository attendance;
    @Mock private AppointmentRepository appointments;

    private SlotAvailabilityService service;
    private Doctor doctor;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getBooking().setSlotMinutes(30);
        service = new SlotAvailabilityService(doctors, schedules, leaves, attendance, appointments, properties);
        date = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1);
        doctor = Doctor.builder().name("Dr. Anand Chinche").active(true).build();
        doctor.setId(7L);
        lenient().when(doctors.findById(7L)).thenReturn(Optional.of(doctor));
        lenient().when(leaves.existsByDoctorIdAndLeaveDate(7L, date)).thenReturn(false);
        DoctorSchedule schedule = DoctorSchedule.builder()
                .doctor(doctor).dayOfWeek(date.getDayOfWeek())
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .slotMinutes(30).active(true).teleconsult(false).build();
        lenient().when(schedules.findByDoctorIdAndActiveTrue(7L)).thenReturn(List.of(schedule));
        lenient().when(appointments.findBookedSlotStarts(eq(7L), eq(date), eq(VisitMode.IN_HOSPITAL), anyList()))
                .thenReturn(List.of());
    }

    @Test
    void hidesSlotsOutsideRecordedArrivalDepartureAndBreakIntervals() {
        DoctorAttendance row = DoctorAttendance.builder()
                .doctor(doctor).attendanceDate(date)
                .arrivalTime(LocalTime.of(10, 30))
                .departureTime(LocalTime.of(11, 30))
                .breaks(List.of(DoctorAttendanceBreak.builder()
                        .startTime(LocalTime.of(10, 45)).endTime(LocalTime.of(11, 0)).build()))
                .build();
        when(attendance.findByDoctorIdAndAttendanceDate(7L, date)).thenReturn(Optional.of(row));

        var result = service.availabilityForDate(7L, date, VisitMode.IN_HOSPITAL);

        assertEquals(4, result.slots().size());
        assertFalse(result.slots().get(0).available());
        assertFalse(result.slots().get(1).available());
        assertTrue(result.slots().get(2).available());
        assertFalse(result.slots().get(3).available());
    }
}
