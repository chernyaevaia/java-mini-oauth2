package ru.yandex.practicum.oauth0.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String refreshId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String clientId;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean rotated = false;

    @Column
    private Instant createdAt = Instant.now();
}