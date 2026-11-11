package in.chinche.hospital.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class HospitalKnowledgeBase {

    private static final String KNOWLEDGE_FILE = "classpath:knowledge/hospital-knowledge.json";
    private static final Set<String> TRUSTED_GENERAL_HEALTH_HOSTS = Set.of("medlineplus.gov", "www.cdc.gov");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "for", "from", "how", "i", "in", "is", "it",
            "me", "my", "of", "on", "or", "the", "to", "what", "when", "where",
            "which", "who", "with", "you", "your", "book", "hai", "hain", "ka", "ke", "ki",
            "kya", "ko", "kaise", "kab", "kaha", "mujhe", "mera", "meri", "m",
            "mala", "majha", "majhi", "aahe", "ahe", "काय", "कसे", "कुठे", "कधी",
            "मला", "माझे", "माझी", "आहे", "आहेत", "का", "आणि", "मध्ये", "साठी");

    private final List<KnowledgeDocument> documents;

    @Autowired
    public HospitalKnowledgeBase(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        Resource resource = resourceLoader.getResource(KNOWLEDGE_FILE);
        try (var input = resource.getInputStream()) {
            documents = validateDocuments(objectMapper.readValue(input, new TypeReference<>() { }));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load hospital assistant knowledge file: " + KNOWLEDGE_FILE, ex);
        }
    }

    HospitalKnowledgeBase(List<KnowledgeDocument> documents) {
        this.documents = validateDocuments(documents);
    }

    private static List<KnowledgeDocument> validateDocuments(List<KnowledgeDocument> documents) {
        List<KnowledgeDocument> validated = List.copyOf(documents);
        if (validated.isEmpty()) {
            throw new IllegalStateException("Hospital assistant knowledge file must contain at least one document.");
        }
        for (KnowledgeDocument document : validated) {
            if (document.id() == null || document.id().isBlank()
                    || document.title() == null || document.title().isBlank()
                    || document.content() == null || document.content().isBlank()
                    || !isAllowedSource(document)) {
                throw new IllegalStateException("Invalid hospital assistant knowledge document: " + document.id());
            }
        }
        return validated;
    }

    public List<RankedKnowledgeDocument> retrieve(String query, boolean allowUnverified, int limit) {
        List<String> queryTokens = tokens(query).stream()
                .filter(token -> token.length() > 1 && !STOP_WORDS.contains(token))
                .distinct()
                .toList();
        if (queryTokens.isEmpty()) return List.of();

        Map<String, Integer> documentFrequency = new HashMap<>();
        for (KnowledgeDocument document : documents) {
            tokens(documentText(document)).stream().distinct()
                    .forEach(token -> documentFrequency.merge(token, 1, Integer::sum));
        }

        List<RankedKnowledgeDocument> ranked = new ArrayList<>();
        for (KnowledgeDocument document : documents) {
            if (!allowUnverified && !document.approved() && !isTrustedGeneralHealthDocument(document)) continue;
            int score = score(document, queryTokens, documentFrequency, documents.size());
            if (score > 0) ranked.add(new RankedKnowledgeDocument(document, score));
        }
        List<RankedKnowledgeDocument> ordered = ranked.stream()
                .sorted(Comparator.comparingInt(RankedKnowledgeDocument::score).reversed()
                        .thenComparing(item -> item.document().id()))
                .toList();
        if (ordered.isEmpty()) return List.of();
        int relevanceThreshold = Math.max(2, (int) Math.ceil(ordered.get(0).score() * 0.45));
        return ordered.stream()
                .filter(item -> item.score() >= relevanceThreshold)
                .limit(Math.max(0, limit))
                .toList();
    }

    public List<KnowledgeDocument> allDocuments() {
        return documents;
    }

    static List<String> tokens(String text) {
        if (text == null || text.isBlank()) return List.of();
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        var matcher = TOKEN_PATTERN.matcher(normalized);
        List<String> result = new ArrayList<>();
        while (matcher.find()) result.add(matcher.group());
        return result;
    }

    private static int score(
            KnowledgeDocument document,
            List<String> queryTokens,
            Map<String, Integer> documentFrequency,
            int documentCount
    ) {
        List<String> titleTokens = tokens(document.title() + " " + nullToEmpty(document.titleMr()));
        List<String> keywordTokens = document.keywords() == null ? List.of()
                : tokens(String.join(" ", document.keywords()));
        List<String> contentTokens = tokens(document.content() + " " + nullToEmpty(document.contentMr()));
        int score = 0;
        int matchedTokens = 0;
        int strongMatches = 0;
        for (String token : queryTokens) {
            int relevance = (int) Math.ceil((double) documentCount
                    / documentFrequency.getOrDefault(token, documentCount));
            boolean inTitle = titleTokens.contains(token);
            boolean inKeywords = keywordTokens.contains(token);
            boolean inContent = contentTokens.contains(token);
            if (inTitle || inKeywords || inContent) matchedTokens++;
            if (inTitle || inKeywords) strongMatches++;
            if (inTitle) score += 4 * relevance;
            if (inKeywords) score += 3 * relevance;
            if (inContent) score += relevance;
        }
        int minimumMatches = queryTokens.size() >= 3 ? 2 : 1;
        return matchedTokens >= minimumMatches || strongMatches > 0 ? score : 0;
    }

    private static String documentText(KnowledgeDocument document) {
        return document.title() + " " + nullToEmpty(document.titleMr()) + " "
                + document.content() + " " + nullToEmpty(document.contentMr()) + " "
                + (document.keywords() == null ? "" : String.join(" ", document.keywords()));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean isAllowedSource(KnowledgeDocument document) {
        String sourceUrl = document.sourceUrl();
        if (sourceUrl == null || sourceUrl.isBlank()) return false;
        if (sourceUrl.startsWith("/") && !sourceUrl.startsWith("//")) return true;
        return isTrustedGeneralHealthDocument(document);
    }

    private static boolean isTrustedGeneralHealthDocument(KnowledgeDocument document) {
        if (!"general-health".equals(document.category()) || document.sourceUrl() == null) return false;
        try {
            URI source = URI.create(document.sourceUrl());
            return "https".equalsIgnoreCase(source.getScheme())
                    && source.getUserInfo() == null
                    && source.getPort() == -1
                    && TRUSTED_GENERAL_HEALTH_HOSTS.contains(source.getHost());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
