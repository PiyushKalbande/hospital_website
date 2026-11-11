package in.chinche.hospital.ai;

import java.util.List;

public record KnowledgeDocument(
        String id,
        String title,
        String titleMr,
        String category,
        String content,
        String contentMr,
        List<String> keywords,
        String sourceUrl,
        String specialitySlug,
        boolean approved
) {
    public String titleFor(String language) {
        return "mr".equals(language) && titleMr != null && !titleMr.isBlank() ? titleMr : title;
    }

    public String contentFor(String language) {
        return "mr".equals(language) && contentMr != null && !contentMr.isBlank() ? contentMr : content;
    }
}
