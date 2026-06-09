package ru.yandex.practicum.oauth0.auth.repository;

import ru.yandex.practicum.oauth0.auth.model.Metrics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetricsRepository extends JpaRepository<Metrics, Long> {
}