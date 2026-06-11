package ru.yandex.practicum.oauth0.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String clientSecretHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_grants", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "grant")
    private List<String> allowedGrants;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_scopes", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "scope")
    private List<String> allowedScopes;

    @Column
    private String audience;
}