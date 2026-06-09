package ru.yandex.practicum.oauth0.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.oauth0.auth.model.Revocation;

import java.util.Optional;

public interface RevocationRepository extends JpaRepository<Revocation, Long> {
    Optional<Revocation> findByTokenIdAndTokenType(String tokenId, String tokenType);
}