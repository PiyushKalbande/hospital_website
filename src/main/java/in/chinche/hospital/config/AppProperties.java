package in.chinche.hospital.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for the {@code chinche.*} namespace. Hospital contact
 * details live here rather than being hard-coded in templates, so a rebrand or a
 * new branch number is a config change, not a code change.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "chinche")
public class AppProperties {

    private Cache cache = new Cache();
    private Seeding seeding = new Seeding();
    private Booking booking = new Booking();
    private Contact contact = new Contact();
    private Assistant assistant = new Assistant();
    private WhatsApp whatsapp = new WhatsApp();

    @Getter @Setter
    public static class Cache {
        private boolean enabled = true;
    }

    @Getter @Setter
    public static class Seeding {
        private boolean enabled = true;
    }

    @Getter @Setter
    public static class Booking {
        /** Default slot length in minutes when a schedule row does not override it. */
        private int slotMinutes = 20;
        /** How many days ahead the calendar exposes. */
        private int advanceDays = 45;
        /** Patients allowed per identical slot (1 = strictly exclusive). */
        private int maxPerSlot = 1;
        /** Prefix of the generated booking reference. */
        private String referencePrefix = "CHH";
    }

    @Getter @Setter
    public static class Contact {
        private String addressLine1;
        private String addressLine2;
        private String pincode;
        private String state;
        private String phone;
        private String emergency;
        private String email;
        private String mapEmbed;
    }

    @Getter @Setter
    public static class Assistant {
        private boolean enabled = true;
        private boolean allowUnverifiedKnowledge;
        private Ollama ollama = new Ollama();
    }

    @Getter @Setter
    public static class Ollama {
        private boolean enabled;
        private String baseUrl = "http://localhost:11434";
        private String model = "qwen2.5:1.5b";
        private int timeoutSeconds = 15;
        private int contextLength = 4096;
        private int maxTokens = 192;
    }

    @Getter @Setter
    public static class WhatsApp {
        private boolean enabled;
        private String baseUrl = "https://graph.facebook.com";
        private String apiVersion = "v22.0";
        private String phoneNumberId;
        private String accessToken;
        private String templateName = "doctor_emergency_reschedule";
        private String languageCode = "en_US";
    }
}
