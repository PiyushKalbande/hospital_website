package in.chinche.hospital.ai;

import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.domain.Speciality;
import in.chinche.hospital.dto.response.DoctorDto;
import in.chinche.hospital.service.CatalogService;
import in.chinche.hospital.service.DoctorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class HospitalAssistantService {

    private static final int MAX_SOURCES = 4;
    private static final List<String> EMERGENCY_PHRASES = List.of(
            "chest pain", "chest pressure", "severe bleeding", "difficulty breathing",
            "trouble breathing", "cannot breathe", "cant breathe", "shortness of breath",
            "face drooping", "one sided weakness", "unconscious", "not responding",
            "slurred speech", "severe allergic reaction", "poisoning", "overdose",
            "heart attack", "stroke symptoms", "seizure", "fits",
            "emergency ambulance", "call an ambulance", "call ambulance",
            "send an ambulance", "need an ambulance", "ambulance now",
            "call 108", "emergency help", "रुग्णवाहिका बोलवा", "१०८ वर कॉल",
            "seene me dard", "chhati me dard", "saans nahi", "saans lene me dikkat",
            "saans ruk", "behosh", "zyada khoon", "छाती में दर्द", "सीने में दर्द",
            "सांस लेने में तकलीफ", "बेहोश", "ज्यादा खून", "बोलने में कठिनाई",
            "छातीत दुखत", "श्वास घेण्यास त्रास", "बेशुद्ध", "जास्त रक्तस्त्राव",
            "अचानक अशक्तपणा", "बोलण्यात अडचण");
    private static final List<String> BOOKING_TERMS = List.of(
            "book", "booking", "appointment", "schedule", "reserve", "slot",
            "अपॉइंटमेंट", "बुकिंग", "वेळ ठरवा");
    private static final Map<String, List<String>> SPECIALITY_ALIASES = Map.of(
            "dermatology", List.of(
                    "skin", "dermatology", "dermatologist", "त्वचा", "त्वचारोग", "twacha", "tvacha"));

    private final HospitalKnowledgeBase knowledgeBase;
    private final DoctorService doctorService;
    private final CatalogService catalogService;
    private final AnswerGenerator answerGenerator;
    private final AppProperties properties;
    private final LiveDoctorScheduleService liveDoctorScheduleService;

    public HospitalAssistantService(
            HospitalKnowledgeBase knowledgeBase,
            DoctorService doctorService,
            CatalogService catalogService,
            AnswerGenerator answerGenerator,
            AppProperties properties,
            LiveDoctorScheduleService liveDoctorScheduleService
    ) {
        this.knowledgeBase = knowledgeBase;
        this.doctorService = doctorService;
        this.catalogService = catalogService;
        this.answerGenerator = answerGenerator;
        this.properties = properties;
        this.liveDoctorScheduleService = liveDoctorScheduleService;
    }

    public AssistantChatResponse answer(AssistantChatRequest request) {
        return answer(request, false);
    }

    public AssistantChatResponse answer(AssistantChatRequest request, boolean staffAccess) {
        if (!properties.getAssistant().isEnabled()) {
            throw new AssistantUnavailableException();
        }
        String question = request.message().strip();
        String language = resolveLanguage(request.language(), question);
        if (isEmergency(question)) return emergencyResponse(language);

        Optional<LiveDoctorScheduleContext> liveContext =
                liveDoctorScheduleService.resolve(question, language, staffAccess);
        if (liveContext.isPresent()) {
            LiveDoctorScheduleContext live = liveContext.get();
                        String answer = live.fallbackAnswer();
            List<AssistantDoctor> liveDoctors = live.doctor() == null
                    ? List.of() : List.of(live.doctor());
            return new AssistantChatResponse(
                    answer, language, false, live.verified(), live.sources(), liveDoctors,
                    live.bookingUrl(), null, null);
        }

        if (isEmergencyContactQuestion(question)) return emergencyContactResponse(language);
        if (isAssistantCapabilitiesQuestion(question)) return capabilitiesResponse(language);
        Optional<AssistantChatResponse> directoryResponse = specialityDirectoryResponse(question, language);
        if (directoryResponse.isPresent()) return directoryResponse.get();

        List<RankedKnowledgeDocument> matches = knowledgeBase.retrieve(
                question, properties.getAssistant().isAllowUnverifiedKnowledge(), MAX_SOURCES);
        List<AssistantSource> sources = matches.stream()
                .map(item -> item.document())
                .map(document -> new AssistantSource(
                        document.id(), document.titleFor(language), document.sourceUrl(),
                        document.category(), document.approved()))
                .toList();
        List<AssistantDoctor> doctors = findDoctors(matches);
        String bookingUrl = bookingUrl(question, matches, doctors);
        if (matches.isEmpty()) {
            return new AssistantChatResponse(
                    notFoundAnswer(language), language, false, false, List.of(), List.of(),
                    bookingUrl, null, null);
        }

        String context = buildContext(matches, doctors, language);
        Optional<String> generatedAnswer = answerGenerator.generate(question, language, context);
        String answer = generatedAnswer.filter(value -> !value.isBlank())
                .map(HospitalAssistantService::limitAnswer)
                .orElseGet(() -> extractiveAnswer(matches, language));
        boolean verified = matches.stream().allMatch(item -> item.document().approved());
        return new AssistantChatResponse(
                answer, language, false, verified, sources, doctors, bookingUrl, null, null);
    }

    private List<AssistantDoctor> findDoctors(List<RankedKnowledgeDocument> matches) {
        for (RankedKnowledgeDocument match : matches) {
            String slug = match.document().specialitySlug();
            if (slug == null || slug.isBlank() || match.score() < 3) continue;
            Optional<Speciality> speciality = catalogService.allSpecialities().stream()
                    .filter(item -> slug.equals(item.getSlug()))
                    .findFirst();
            if (speciality.isEmpty()) {
                log.warn("Assistant knowledge entry '{}' references unknown speciality '{}'.",
                        match.document().id(), slug);
                continue;
            }
            return doctorService.bySpecialityDtos(speciality.get().getId()).stream()
                    .limit(3)
                    .map(doctor -> toAssistantDoctor(doctor, speciality.get().getName()))
                    .toList();
        }
        return List.of();
    }

    private static AssistantDoctor toAssistantDoctor(DoctorDto doctor, String specialityName) {
        String slug = UriUtils.encodePathSegment(doctor.slug(), StandardCharsets.UTF_8);
        return new AssistantDoctor(
                doctor.name(),
                doctor.specialityName() == null ? specialityName : doctor.specialityName(),
                "/doctors/" + slug,
                "/book?doctor=" + slug
        );
    }

    private static String bookingUrl(
            String question,
            List<RankedKnowledgeDocument> matches,
            List<AssistantDoctor> doctors
    ) {
        if (!doctors.isEmpty()) return doctors.get(0).bookingUrl();
        if (containsAny(question, BOOKING_TERMS)) return "/book";
        return matches.stream()
                .map(item -> item.document().specialitySlug())
                .filter(slug -> slug != null && !slug.isBlank())
                .findFirst()
                .map(slug -> "/book?speciality=" + UriUtils.encodePathSegment(slug, StandardCharsets.UTF_8))
                .orElse(null);
    }

    private AssistantChatResponse emergencyResponse(String language) {
        String emergencyContact = properties.getContact().getEmergency();
        String phone = emergencyContact == null || emergencyContact.isBlank()
                ? "108" : emergencyContact.split("/", 2)[0].strip();
        String answer = "mr".equals(language)
                ? "ही आपत्कालीन परिस्थिती असू शकते. आत्ताच १०८ वर कॉल करा किंवा जवळच्या आपत्कालीन विभागात जा. मी लक्षणांचे निदान किंवा उपचार सल्ला देऊ शकत नाही—चॅटबॉटच्या उत्तराची वाट पाहू नका."
                : "This may be an emergency. Call 108 now or go to the nearest emergency department. I can't assess symptoms or give treatment advice—do not wait for a chatbot reply.";
        return new AssistantChatResponse(
                answer, language, true, true,
                List.of(new AssistantSource("emergency", "24×7 Emergency", "/emergency", "safety", true)),
                List.of(), null, "/emergency", phone);
    }

    private AssistantChatResponse emergencyContactResponse(String language) {
        String contact = properties.getContact().getEmergency();
        if (contact == null || contact.isBlank()) return new AssistantChatResponse(
                notFoundAnswer(language), language, false, false, List.of(), List.of(),
                null, null, null);
        String answer = "mr".equals(language)
                ? "वेबसाइट सेटिंगमध्ये दिलेला आपत्कालीन संपर्क क्रमांक: " + contact
                        + ". तातडीची वैद्यकीय मदत हवी असल्यास १०८ वर कॉल करा."
                : "The emergency contact currently listed in the website settings is " + contact
                        + ". For urgent medical help, call 108.";
        return new AssistantChatResponse(
                answer, language, false, true,
                List.of(new AssistantSource(
                        "emergency-contact", "Emergency contact", "/emergency", "contact", true)),
                List.of(), null, "/emergency", contact.split("/", 2)[0].strip());
    }

    private AssistantChatResponse capabilitiesResponse(String language) {
        String answer = "mr".equals(language)
                ? "मी सक्रिय डॉक्टर व विभाग शोधण्यात, उपलब्ध डॉक्टरांची यादी दाखवण्यात, बुकिंग पेज उघडण्यात, मंजूर रुग्णालयीन माहिती सांगण्यात आणि नाव दिलेल्या डॉक्टरचे आजचे उपलब्ध स्लॉट तपासण्यात मदत करू शकतो. मी निदान किंवा औषध/उपचार सल्ला देत नाही. आपत्कालीन मदतीसाठी १०८ वर कॉल करा."
                : "I can find active doctors and departments, show doctors currently listed by specialty, take you to appointment booking, answer hospital information that staff has approved, and check today's slots for a named doctor. I can't diagnose or advise on medicines or treatment. For emergencies, call 108.";
        return new AssistantChatResponse(answer, language, false, true, List.of(), List.of(),
                null, null, null);
    }

    private Optional<AssistantChatResponse> specialityDirectoryResponse(String question, String language) {
        if (!isDoctorDirectoryQuestion(question)) return Optional.empty();
        String normalized = normalize(question);
        for (Speciality speciality : catalogService.allSpecialities()) {
            List<String> aliases = SPECIALITY_ALIASES.getOrDefault(speciality.getSlug(), List.of());
            boolean matched = aliases.stream().anyMatch(alias -> containsTerm(normalized, normalize(alias)))
                    || containsTerm(normalized, normalize(speciality.getName()))
                    || containsTerm(normalized, normalize(speciality.getSlug().replace('-', ' ')));
            if (!matched) continue;

            List<AssistantDoctor> doctors = doctorService.bySpecialityDtos(speciality.getId()).stream()
                    .limit(5)
                    .map(doctor -> toAssistantDoctor(doctor, speciality.getName()))
                    .toList();
            String doctorNames = doctors.stream().map(AssistantDoctor::name)
                    .reduce((first, second) -> first + ", " + second).orElse("");
            String answer = doctors.isEmpty()
                    ? ("mr".equals(language)
                    ? speciality.getName() + " विभागात सध्या सक्रिय डॉक्टरांची नोंद दिसत नाही."
                    : "No active doctors are currently listed under " + speciality.getName() + ".")
                    : ("mr".equals(language)
                    ? speciality.getName() + " साठी सध्या उपलब्ध डॉक्टर: " + doctorNames + "."
                    : "Doctors currently listed for " + speciality.getName() + ": " + doctorNames + ".");
            String specialityUrl = "/specialities/" + UriUtils.encodePathSegment(
                    speciality.getSlug(), StandardCharsets.UTF_8);
            return Optional.of(new AssistantChatResponse(
                    answer, language, false, true,
                    List.of(new AssistantSource(
                            "live-speciality-" + speciality.getSlug(), speciality.getName(),
                            specialityUrl, "directory", true)),
                    doctors,
                    doctors.isEmpty()
                            ? "/book?speciality=" + UriUtils.encodePathSegment(
                                    speciality.getSlug(), StandardCharsets.UTF_8)
                            : doctors.get(0).bookingUrl(),
                    null, null));
        }
        return Optional.empty();
    }

    private static boolean isEmergencyContactQuestion(String question) {
        String normalized = normalize(question);
        boolean emergency = containsAny(normalized, List.of(
                "emergency", "ambulance", "आपत्कालीन", "रुग्णवाहिका", "आपत्कालीन"));
        boolean asksContact = containsAny(normalized, List.of(
                "number", "phone", "contact", "क्रमांक", "फोन", "number kay", "kiti"));
        return emergency && asksContact;
    }

    private static boolean isAssistantCapabilitiesQuestion(String question) {
        return containsAny(normalize(question), List.of(
                "what can you answer", "what you can answer", "what can i ask",
                "what do you help", "what can you help", "how can you help",
                "what questions", "tumhi kay", "kay vicharu", "kay uttar",
                "काय विचारू", "काय उत्तर", "कशाबद्दल मदत"));
    }

    private static boolean isDoctorDirectoryQuestion(String question) {
        String normalized = normalize(question);
        boolean asksForPeople = containsAny(normalized, List.of(
                "which doctor", "which doctors", "what doctors", "who are the doctors",
                "doctor name", "doctors list", "list doctors", "konte doctor", "konte doctors",
                "kontya doctor", "kon doctor", "kon doctor", "कुठले डॉक्टर", "कोणते डॉक्टर",
                "कोण डॉक्टर", "डॉक्टरांची नावे"));
        boolean asksSpecialty = SPECIALITY_ALIASES.values().stream()
                .flatMap(List::stream)
                .anyMatch(alias -> containsTerm(normalized, normalize(alias)));
        return asksForPeople && asksSpecialty;
    }

    private static boolean containsTerm(String text, String term) {
        return (" " + text + " ").contains(" " + term + " ");
    }

    private static String extractiveAnswer(List<RankedKnowledgeDocument> matches, String language) {
        return matches.stream()
                .limit(3)
                .map(item -> item.document().titleFor(language) + "\n"
                        + item.document().contentFor(language))
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }

    private static String limitAnswer(String answer) {
        String cleaned = answer.strip();
        return cleaned.length() <= 2000 ? cleaned : cleaned.substring(0, 2000);
    }

    private static String buildContext(
            List<RankedKnowledgeDocument> matches,
            List<AssistantDoctor> doctors,
            String language
    ) {
        StringBuilder context = new StringBuilder();
        for (RankedKnowledgeDocument match : matches) {
            KnowledgeDocument document = match.document();
                        if ("general-health".equals(document.category())) {
                                context.append("[General public health source; not Chinche Hospital policy: ")
                                                .append(document.titleFor(language)).append("]\n")
                                                .append("Source URL: ").append(document.sourceUrl()).append('\n');
                        } else {
                                context.append("[Hospital source: ").append(document.titleFor(language)).append("]\n");
                        }
                        context
                    .append(document.contentFor(language)).append('\n');
        }
        if (!doctors.isEmpty()) {
            context.append("\n[Current public doctor profiles]\n");
            doctors.forEach(doctor -> context.append(doctor.name()).append(" — ")
                    .append(doctor.speciality()).append(" — ")
                    .append(doctor.profileUrl()).append('\n'));
        }
        return context.toString();
    }

    private static String notFoundAnswer(String language) {
        return "mr".equals(language)
                ? "मला रुग्णालयाच्या मंजूर माहितीत याचे उत्तर सापडले नाही. कृपया रुग्णालयाशी संपर्क साधा; मी वैद्यकीय निदान किंवा उपचार सल्ला देऊ शकत नाही."
                : "I couldn't find this in the hospital information available to me. Please contact the hospital to confirm. I can't provide a diagnosis or treatment advice.";
    }

    private static String resolveLanguage(String requestedLanguage, String question) {
        if ("mr".equals(requestedLanguage)) return "mr";
        boolean devanagari = question.codePoints()
                .anyMatch(codePoint -> codePoint >= 0x0900 && codePoint <= 0x097F);
        return devanagari || isRomanizedMarathi(question) ? "mr" : "en";
    }

    private static boolean isRomanizedMarathi(String question) {
        List<String> markers = List.of(
                "aaj", "aahe", "ahe", "aahet", "ahet", "kiti", "kutha", "kuthe", "kuthhe",
                "kay", "ka", "mala", "majha", "majhi", "chalu", "rahnaar", "rahna", "karaycha",
                "konte", "kontya", "kon", "ya", "madhe", "astat", "dawakhana", "dawakhanyamadhe");
        long matches = List.of(normalize(question).split("\\s+")).stream()
                .filter(token -> markers.contains(token) || token.endsWith("sathi"))
                .distinct()
                .count();
        return matches >= 2;
    }

    private static boolean isEmergency(String question) {
        return containsAny(question, EMERGENCY_PHRASES);
    }

    private static boolean containsAny(String text, List<String> phrases) {
        String normalized = normalize(text);
        return phrases.stream().map(HospitalAssistantService::normalize)
                .anyMatch(phrase -> normalized.contains(phrase));
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }
}
