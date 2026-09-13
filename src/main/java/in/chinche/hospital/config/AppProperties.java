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
}
