package in.chinche.hospital.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppointmentPatientSnapshotTest {

    @Test
    void displaysBookingTimeIdentityInsteadOfLaterPatientProfileChanges() {
        Patient patient = Patient.builder().fullName("Latest profile name").phone("9999999999").build();
        Appointment appointment = Appointment.builder()
                .patient(patient)
                .patientNameSnapshot("Name at booking")
                .patientPhoneSnapshot("8888888888")
                .build();

        assertEquals("Name at booking", appointment.getPatientNameDisplay());
        assertEquals("8888888888", appointment.getPatientPhoneDisplay());
    }

    @Test
    void fallsBackToPatientProfileForLegacyRowsWithoutSnapshots() {
        Patient patient = Patient.builder().fullName("Legacy patient").phone("7777777777").build();
        Appointment appointment = Appointment.builder().patient(patient).build();

        assertEquals("Legacy patient", appointment.getPatientNameDisplay());
        assertEquals("7777777777", appointment.getPatientPhoneDisplay());
    }
}