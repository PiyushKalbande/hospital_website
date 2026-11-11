package in.chinche.hospital.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HospitalKnowledgeBaseTest {

    @Test
    void loadsKnowledgeBaseThroughSpringConstructorInjection() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().registerSingleton("objectMapper", new ObjectMapper());
            context.registerBean(HospitalKnowledgeBase.class);
            context.refresh();

            assertFalse(context.getBean(HospitalKnowledgeBase.class).allDocuments().isEmpty());
        }
    }

    @Test
    void servesTrustedPublicHealthGuidanceWithoutMarkingItHospitalApproved() {
        HospitalKnowledgeBase knowledgeBase = new HospitalKnowledgeBase(List.of(new KnowledgeDocument(
                "glucose", "Fasting glucose", null, "general-health", "Plain water is usually allowed.",
                null, List.of("water", "sugar test"),
                "https://medlineplus.gov/lab-tests/fasting-for-a-blood-test/", null, false)));

        List<RankedKnowledgeDocument> results = knowledgeBase.retrieve("sugar test water", false, 4);

        assertEquals(1, results.size());
        assertFalse(results.get(0).document().approved());
    }

    @Test
    void retrievesCdcDiabetesTestTypesForHba1cAndRandomGlucoseQuestion() {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new ObjectMapper();
        HospitalKnowledgeBase knowledgeBase = new HospitalKnowledgeBase(objectMapper,
                new org.springframework.core.io.DefaultResourceLoader());

        List<RankedKnowledgeDocument> results = knowledgeBase.retrieve("HbA1c random glucose", false, 4);

        assertTrue(results.stream().anyMatch(item -> item.document().sourceUrl()
                .equals("https://www.cdc.gov/diabetes/diabetes-testing/")));
    }

    @Test
    void rejectsExternalSourcesOutsideTheCuratedAllowlist() {
        assertThrows(IllegalStateException.class, () -> new HospitalKnowledgeBase(List.of(new KnowledgeDocument(
                "untrusted", "Untrusted source", null, "general-health", "Content", null, List.of("test"),
                "https://example.com/medical", null, false))));
    }
}
