package in.chinche.hospital.ai;

import in.chinche.hospital.domain.Doctor;
import in.chinche.hospital.domain.DoctorAttendance;
import in.chinche.hospital.domain.DoctorAttendanceBreak;
import in.chinche.hospital.domain.Speciality;
import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.dto.response.DayAvailabilityDto;
import in.chinche.hospital.dto.response.SlotDto;
import in.chinche.hospital.repository.AppointmentRepository;
import in.chinche.hospital.repository.DoctorAttendanceRepository;
import in.chinche.hospital.repository.DoctorRepository;
import in.chinche.hospital.service.SlotAvailabilityService;
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
class LiveDoctorScheduleServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    @Mock
    private DoctorRepository doctors;

    @Mock
    private DoctorAttendanceRepository attendance;

    @Mock
    private AppointmentRepository appointments;

    @Mock
    private SlotAvailabilityService availability;

    private LiveDoctorScheduleService service;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        service = new LiveDoctorScheduleService(doctors, attendance, appointments, availability);
        Speciality speciality = Speciality.builder().name("General Medicine").slug("general-medicine").build();
        doctor = Doctor.builder().name("Dr. Anand Chinche").slug("anand-chinche")
                .active(true).speciality(speciality).build();
        doctor.setId(41L);
        lenient().when(doctors.findByActiveTrueOrderBySortOrderAscNameAsc()).thenReturn(List.of(doctor));
        lenient().when(availability.today()).thenReturn(TODAY);
        lenient().when(availability.availabilityForDate(eq(41L), eq(TODAY), any()))
                .thenReturn(new DayAvailabilityDto(TODAY, "Thu", true, null, List.of(
                        new SlotDto(LocalTime.of(10, 0), LocalTime.of(10, 30), true, "MORNING"),
                        new SlotDto(LocalTime.of(10, 30), LocalTime.of(11, 0), false, "MORNING"),
                        new SlotDto(LocalTime.of(11, 0), LocalTime.of(11, 30), true, "MORNING")
                )));
    }

    @Test
    void answersPublicAvailabilityFromTodaysDatabaseSlots() {
        LiveDoctorScheduleContext result = service.resolve(
                "Is Anand Chinche available today and what slots are free?", "en", false).orElseThrow();

        assertTrue(result.verified());
        assertTrue(result.context().contains("LIVE DATABASE SNAPSHOT"));
        assertTrue(result.context().contains("10:00 AM-10:30 AM"));
        assertTrue(result.context().contains("11:00 AM-11:30 AM"));
        assertFalse(result.context().contains("10:30 AM-11:00 AM"));
        assertEquals("/book?doctor=anand-chinche", result.bookingUrl());
        assertEquals("Dr. Anand Chinche", result.doctor().name());
        verifyNoInteractions(attendance, appointments);
    }

        @Test
        void answersRomanizedMarathiDoctorPresenceWithYesNoAndNoUnaskedSlotList() {
                LiveDoctorScheduleContext result = service.resolve(
                                "aaj anand chinche doctor aahe ka", "mr", false).orElseThrow();

                assertTrue(result.fallbackAnswer().startsWith("हो."));
                assertTrue(result.fallbackAnswer().contains("2 बुकिंगच्या वेळा"));
                assertFalse(result.fallbackAnswer().contains("Available times:"));
                assertEquals("Dr. Anand Chinche", result.doctor().name());
        }

        @Test
        void letsGenericSpecialistAvailabilityFallThroughToKnowledgeSearch() {
                assertTrue(service.resolve(
                                "Is there a dermatologist available for facial pimples?", "en", false).isEmpty());
                verify(availability, never()).availabilityForDate(anyLong(), any(), any());
                verifyNoInteractions(attendance, appointments);
        }

    @Test
    void hidesBreaksAndAppointmentCountsFromPublicVisitors() {
        LiveDoctorScheduleContext result = service.resolve(
                "How many patients does Anand Chinche have today and when is his break?",
                "en", false).orElseThrow();

        assertFalse(result.verified());
        assertTrue(result.fallbackAnswer().contains("private"));
        assertTrue(result.sources().isEmpty());
        verify(availability, never()).availabilityForDate(anyLong(), any(), any());
        verifyNoInteractions(attendance, appointments);
    }

    @Test
    void suppliesOnlyBreakTimesAndAggregateAppointmentCountToAuthorizedStaff() {
        DoctorAttendance row = DoctorAttendance.builder()
                .doctor(doctor)
                .attendanceDate(TODAY)
                .breaks(List.of(DoctorAttendanceBreak.builder()
                        .startTime(LocalTime.of(13, 0)).endTime(LocalTime.of(13, 30)).build()))
                .notes("Do not expose staff notes.")
                .build();
        when(attendance.findByDoctorIdAndAttendanceDate(41L, TODAY)).thenReturn(Optional.of(row));
        when(appointments.countByDoctorIdAndAppointmentDateAndStatusIn(
                eq(41L), eq(TODAY), anyList())).thenReturn(4L);

        LiveDoctorScheduleContext result = service.resolve(
                "Anand Chinche break time today and how many patients, which slots are free?",
                "en", true).orElseThrow();

        assertTrue(result.verified());
        assertTrue(result.context().contains("1:00 PM-1:30 PM"));
        assertTrue(result.context().contains("appointment count for this doctor: 4"));
        assertFalse(result.context().contains("Do not expose staff notes"));
        assertTrue(result.sources().stream().anyMatch(source -> source.category().equals("appointments")));
        assertTrue(result.sources().stream().anyMatch(source -> source.category().equals("staff-attendance")));
        assertTrue(result.fallbackAnswer().contains("4 scheduled appointments"));
        verify(appointments).countByDoctorIdAndAppointmentDateAndStatusIn(
                41L, TODAY, List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED,
                        AppointmentStatus.CHECKED_IN, AppointmentStatus.COMPLETED));
    }

    @Test
    void asksForDoctorNameInsteadOfGuessingWhoMeMeans() {
        LiveDoctorScheduleContext result = service.resolve(
                "How many patients do I have today and which slots are free?", "en", true).orElseThrow();

        assertTrue(result.fallbackAnswer().contains("full name"));
        assertTrue(result.sources().isEmpty());
        verifyNoInteractions(attendance, appointments, availability);
    }

    @Test
    void ignoresQuestionsThatAreNotAboutLiveSchedules() {
        assertTrue(service.resolve("What services does the hospital offer?", "en", true).isEmpty());
        verifyNoInteractions(doctors, attendance, appointments, availability);
    }
}
