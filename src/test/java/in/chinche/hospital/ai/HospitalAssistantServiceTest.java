package in.chinche.hospital.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.domain.Speciality;
import in.chinche.hospital.dto.response.DoctorDto;
import in.chinche.hospital.service.CatalogService;
import in.chinche.hospital.service.DoctorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HospitalAssistantServiceTest {

    @Mock
    private DoctorService doctorService;

    @Mock
    private CatalogService catalogService;

    @Mock
    private AnswerGenerator answerGenerator;

    @Mock
    private LiveDoctorScheduleService liveDoctorScheduleService;

    private AppProperties properties;
    private HospitalKnowledgeBase knowledgeBase;
    private HospitalAssistantService assistant;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.getAssistant().setAllowUnverifiedKnowledge(true);
        properties.getContact().setEmergency("108 / +91 70836 82563");
        knowledgeBase = new HospitalKnowledgeBase(new ObjectMapper(), new DefaultResourceLoader());
        lenient().when(liveDoctorScheduleService.resolve(anyString(), anyString(), anyBoolean()))
                .thenReturn(Optional.empty());
        assistant = new HospitalAssistantService(
                knowledgeBase, doctorService, catalogService, answerGenerator, properties,
                liveDoctorScheduleService);
    }

    @Test
    void citesDepartmentAndProvidesCurrentDoctorBookingHandoff() {
        Speciality dermatology = Speciality.builder().name("Dermatology").slug("dermatology").build();
        dermatology.setId(7L);
        when(catalogService.allSpecialities()).thenReturn(List.of(dermatology));
        when(doctorService.bySpecialityDtos(7L)).thenReturn(List.of(doctor()));
        when(answerGenerator.generate(anyString(), eq("en"), anyString()))
                .thenReturn(Optional.of("The hospital lists a dermatology department."));

        AssistantChatResponse result = assistant.answer(
                new AssistantChatRequest("I need a skin specialist", "en"));

        assertEquals("The hospital lists a dermatology department.", result.answer());
        assertEquals(result.sources().stream().allMatch(AssistantSource::approved), result.verified());
        assertEquals(1, result.sources().size());
        assertTrue(result.sources().stream().anyMatch(source -> source.url().equals("/specialities/dermatology")));
        assertEquals("Dr. Neha Raut", result.doctors().get(0).name());
        assertEquals("/book?doctor=dr-neha-raut", result.doctors().get(0).bookingUrl());
        verify(doctorService).bySpecialityDtos(7L);
    }

    @Test
    void sharesVisitPreparationWithCitationsAndSampleWarningState() {
        when(answerGenerator.generate(anyString(), eq("en"), anyString())).thenReturn(Optional.empty());

        AssistantChatResponse result = assistant.answer(
                new AssistantChatRequest("What should I bring to my appointment?", "en"));

        assertFalse(result.sources().isEmpty());
        assertTrue(result.sources().stream().anyMatch(source -> source.category().equals("preparation")));
        assertTrue(result.answer().contains("Sample checklist"));
        assertEquals(result.sources().stream().allMatch(AssistantSource::approved), result.verified());
    }

    @Test
    void retrievesMarathiVisitPreparation() {
        when(answerGenerator.generate(anyString(), eq("mr"), anyString())).thenReturn(Optional.empty());

        AssistantChatResponse result = assistant.answer(
                new AssistantChatRequest("भेटीसाठी काय आणावे?", "mr"));

        assertTrue(result.sources().stream().anyMatch(source -> source.category().equals("preparation")));
        assertTrue(result.answer().contains("नमुना यादी"));
    }

        @Test
        void retrievesDermatologyForRomanizedMarathiPimpleQuestion() {
                when(answerGenerator.generate(anyString(), eq("mr"), anyString())).thenReturn(Optional.empty());

                AssistantChatResponse result = assistant.answer(
                                new AssistantChatRequest("chehryawarti pimple aale aahe tyasathi doctor aahe ka", "mr"));

                assertTrue(result.sources().stream().anyMatch(source -> source.url().equals("/specialities/dermatology")));
        }

        @Test
        void routesGenericDermatologistAvailabilityToTheDepartmentInsteadOfDoctorNameLookup() {
                when(answerGenerator.generate(anyString(), eq("en"), anyString())).thenReturn(Optional.empty());

                AssistantChatResponse result = assistant.answer(
                                new AssistantChatRequest("Is there a dermatologist available for facial pimples?", "en"));

                assertTrue(result.sources().stream().anyMatch(source -> source.url().equals("/specialities/dermatology")));
                assertFalse(result.answer().contains("couldn't find an active doctor"));
        }

        @Test
        void retrievesSampleHoursForRomanizedMarathiQuestionAndSelectsMarathi() {
                when(answerGenerator.generate(anyString(), eq("mr"), anyString())).thenReturn(Optional.empty());

                AssistantChatResponse result = assistant.answer(
                                new AssistantChatRequest("aaj kiti te kiti chalu rahnar hospital", null));

                assertEquals("mr", result.language());
                assertTrue(result.sources().stream().anyMatch(source -> source.category().equals("hours")));
                assertTrue(result.answer().contains("नमुना माहिती"));
                assertEquals(result.sources().stream().allMatch(AssistantSource::approved), result.verified());
        }

            @Test
            void answersRomanizedMarathiSugarTestQuestionWithGeneralGuidanceAndOfficialSources() {
                properties.getAssistant().setAllowUnverifiedKnowledge(false);
                when(answerGenerator.generate(anyString(), eq("mr"), anyString())).thenReturn(Optional.empty());

                AssistantChatResponse result = assistant.answer(new AssistantChatRequest(
                        "sugar test chay pahile pani pena bhi allowed nahi aahe ka", null));

                assertEquals("mr", result.language());
                assertFalse(result.verified());
                assertTrue(result.answer().contains("साधे पाणी सहसा चालते"));
                assertTrue(result.answer().contains("चहा, कॉफी"));
                assertTrue(result.sources().stream().anyMatch(source ->
                        source.url().equals("https://medlineplus.gov/lab-tests/fasting-for-a-blood-test/")),
                        result.sources().toString());
            }

        @Test
        void marksPublicHealthContextAsNotHospitalPolicyForOllama() {
                String question = "sugar test chay pahile pani allowed aahe ka";
                when(answerGenerator.generate(anyString(), eq("mr"), anyString()))
                                .thenReturn(Optional.of("General guidance only."));

                assistant.answer(new AssistantChatRequest(question, null));

                ArgumentCaptor<String> context = ArgumentCaptor.forClass(String.class);
                verify(answerGenerator).generate(eq(question), eq("mr"), context.capture());
                assertTrue(context.getValue().contains("General public health source; not Chinche Hospital policy"));
                assertTrue(context.getValue().contains("https://medlineplus.gov/lab-tests/fasting-for-a-blood-test/"));
        }

    @Test
    void handsBookingIntentToExistingBookingPage() {
        when(answerGenerator.generate(anyString(), eq("en"), anyString())).thenReturn(Optional.empty());

        AssistantChatResponse result = assistant.answer(
                new AssistantChatRequest("I want to book an appointment", "en"));

        assertEquals("/book", result.bookingUrl());
        assertTrue(result.sources().stream().anyMatch(source -> source.category().equals("booking")));
    }

    @Test
    void excludesUnapprovedRecordsWhenProductionPolicyIsUsed() {
        properties.getAssistant().setAllowUnverifiedKnowledge(false);
        knowledgeBase = new HospitalKnowledgeBase(List.of(new KnowledgeDocument(
                "dermatology", "Dermatology department", null, "speciality",
                "This is unreviewed content.", null, List.of("skin", "specialist"),
                "/specialities/dermatology", "dermatology", false)));
        assistant = new HospitalAssistantService(
                knowledgeBase, doctorService, catalogService, answerGenerator, properties,
                liveDoctorScheduleService);

        AssistantChatResponse result = assistant.answer(
                new AssistantChatRequest("Find a skin specialist", "en"));

        assertTrue(result.sources().isEmpty());
        assertFalse(result.verified());
        assertTrue(result.answer().contains("couldn't find this"));
        verifyNoInteractions(answerGenerator, catalogService, doctorService);
    }

    @Test
    void routesEmergencyBeforeRetrievalOrModelGenerationInMarathi() {
        AssistantChatResponse result = assistant.answer(
                new AssistantChatRequest("छातीत दुखत आहे आणि श्वास घेण्यास त्रास", "mr"));

        assertTrue(result.emergency());
        assertEquals("mr", result.language());
        assertEquals("/emergency", result.emergencyUrl());
        assertEquals("108", result.emergencyPhone());
        assertTrue(result.answer().contains("वाट पाहू नका"));
        verifyNoInteractions(answerGenerator, catalogService, doctorService);
    }

    @Test
    void routesExplicitAmbulanceRequestBeforeRetrievalOrModelGeneration() {
        AssistantChatResponse result = assistant.answer(
                new AssistantChatRequest("This is a test: emergency ambulance 108", "en"));

        assertTrue(result.emergency());
        assertEquals("/emergency", result.emergencyUrl());
        assertEquals("108", result.emergencyPhone());
        verifyNoInteractions(answerGenerator, catalogService, doctorService);
    }

    @Test
    void answersEmergencyNumberFromConfiguredHospitalContact() {
        AssistantChatResponse result = assistant.answer(
                new AssistantChatRequest("emergency number kay aahe", null));

        assertEquals("mr", result.language());
        assertTrue(result.answer().contains("108 / +91 70836 82563"));
        assertTrue(result.verified());
        assertEquals("/emergency", result.sources().get(0).url());
        verifyNoInteractions(answerGenerator);
    }

    @Test
    void answersWhatTheAssistantCanHelpWithWithoutNeedingKnowledgeDocuments() {
        AssistantChatResponse result = assistant.answer(
                new AssistantChatRequest("What can you answer to me?", "en"));

        assertTrue(result.answer().contains("active doctors"));
        assertTrue(result.answer().contains("named doctor"));
        assertFalse(result.answer().contains("couldn't find this"));
        verifyNoInteractions(answerGenerator);
    }

    @Test
    void findsLiveDermatologyDoctorsForRomanizedMarathiDirectoryQuestion() {
        Speciality dermatology = Speciality.builder().name("Dermatology").slug("dermatology").build();
        dermatology.setId(7L);
        when(catalogService.allSpecialities()).thenReturn(List.of(dermatology));
        when(doctorService.bySpecialityDtos(7L)).thenReturn(List.of(doctor()));

        AssistantChatResponse result = assistant.answer(new AssistantChatRequest(
                "skin sathi konte doctors ya dawakhanyamadhe astat", null));

        assertEquals("mr", result.language());
        assertTrue(result.answer().contains("Dr. Neha Raut"));
        assertEquals("Dr. Neha Raut", result.doctors().get(0).name());
        assertTrue(result.verified());
        verifyNoInteractions(answerGenerator);
    }

    @Test
    void unknownQuestionDoesNotInventAnAnswerOrCallTheModel() {
        AssistantChatResponse result = assistant.answer(
                new AssistantChatRequest("Explain quantum entanglement", "en"));

        assertTrue(result.sources().isEmpty());
        assertTrue(result.doctors().isEmpty());
        assertNull(result.bookingUrl());
        assertTrue(result.answer().contains("couldn't find this"));
        verifyNoInteractions(answerGenerator, catalogService, doctorService);
    }

    @Test
    void doesNotMatchAnUnrelatedPreparationRecordToWaitingTimeQuestion() {
        AssistantChatResponse result = assistant.answer(
                new AssistantChatRequest("What is the exact current waiting time?", "en"));

        assertTrue(result.sources().isEmpty());
        assertTrue(result.answer().contains("couldn't find this"));
        verifyNoInteractions(answerGenerator, catalogService, doctorService);
    }

    @Test
    void rejectsRemoteOllamaEndpointsToKeepPromptsLocal() {
        properties.getAssistant().getOllama().setEnabled(true);
        properties.getAssistant().getOllama().setBaseUrl("https://api.example.com");

        assertThrows(IllegalArgumentException.class,
                () -> new OllamaAnswerGenerator(properties, org.springframework.web.client.RestClient.builder()));
    }

    @Test
    void fallsBackWhenLocalModelIsDisabled() {
        OllamaAnswerGenerator generator = new OllamaAnswerGenerator(
                properties, org.springframework.web.client.RestClient.builder());

        assertTrue(generator.generate("question", "en", "context").isEmpty());
    }

    private static DoctorDto doctor() {
        return new DoctorDto(
                11L, "Dr. Neha Raut", "dr-neha-raut", "Dermatologist", "MBBS, MD", null,
                7L, "Dermatology", "dermatology", 1L, "Chinche Hospital", "Katol",
                12, List.of("en", "mr"), null, null, null, List.of("Medical dermatology"),
                4.8, 24, true);
    }
}
