package at.technikum.dms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Eine Sammlung (Ordner/Mappe), der Dokumente zugeordnet werden können.
 * Umbenannt von "Collection", um Verwechslungen mit java.util.Collection zu vermeiden.
 */
@Entity
@Table(name = "collections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCollection {

    public static final int NAME_MAX_LENGTH = 100;
    public static final int DESCRIPTION_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = NAME_MAX_LENGTH)
    private String name;

    @Column(length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

