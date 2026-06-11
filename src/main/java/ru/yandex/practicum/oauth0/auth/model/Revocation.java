package ru.yandex.practicum.oauth0.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "revocations")
@Getter
@Setter
@NoArgsConstructor
public class Revocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tokenId;

    @Column(nullable = false)
    private String tokenType;

    @Column
    private Instant revokedAt = Instant.now();

    @Column
    private Instant expiresAt;
}