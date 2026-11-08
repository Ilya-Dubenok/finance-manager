package org.example.finmanager.application.dto.result;

import java.math.BigDecimal;
import org.example.finmanager.application.dto.model.OperationType;

/**
 * Категория для показа в списке.
 *
 * @param name название
 * @param type тип
 * @param system {@code true} для системной категории
 * @param budgetLimit лимит бюджета; {@code null}, если бюджета нет или категория доходная
 */
public record CategoryViewDto(
    String name, OperationType type, boolean system, BigDecimal budgetLimit) {}
