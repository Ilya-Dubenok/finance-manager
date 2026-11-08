package org.example.finmanager.adapter.inbound.cli.dto;

import java.time.LocalDate;

/**
 * Данные вопросов о периоде.
 *
 * @param from начало или {@code null}, если пропущено
 * @param to конец или {@code null}, если пропущен
 */
public record PeriodForm(LocalDate from, LocalDate to) {}
