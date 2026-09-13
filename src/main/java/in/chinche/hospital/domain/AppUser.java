package in.chinche.hospital.domain;

import in.chinche.hospital.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

/** Back-office staff account (admin console). BCrypt-hashed passwords only. */
@Entity
@Table(name = "app_user", indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser extends BaseEntity {

    @Column(nullable = false, length = 60, unique = true)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "full_name", length = 160)
    private String fullName;

    @Column(length = 160)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private UserRole role = UserRole.ROLE_RECEPTIONIST;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "last_login_at")
    private java.time.Instant lastLoginAt;
}
