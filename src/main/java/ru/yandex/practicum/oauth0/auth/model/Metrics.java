package ru.yandex.practicum.oauth0.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "metrics")
@Getter
@Setter
@NoArgsConstructor
public class Metrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;
    private String grantType;
    private String clientId;
    private String userId;
    private boolean success;
    private String errorCode;
    private String details;
    private Instant createdAt = Instant.now();
}