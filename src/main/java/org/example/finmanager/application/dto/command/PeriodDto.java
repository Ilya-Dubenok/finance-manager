package org.example.finmanager.application.dto.command;

import java.time.LocalDate;

/**
 * Период для фильтра: обе границы включительно, любая может быть {@code null}.
 *
 * @param from начало или {@code null}
 * @param to конец или {@code null}
 */
public record PeriodDto(LocalDate from, LocalDate to) {}
