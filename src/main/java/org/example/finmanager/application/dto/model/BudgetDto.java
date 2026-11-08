package org.example.finmanager.application.dto.model;

import java.math.BigDecimal;

/**
 * Бюджет: лимит расходов на категорию, один на категорию, без периода.
 *
 * @param category название расходной категории
 * @param limit лимит больше нуля, два знака после запятой
 */
public record BudgetDto(String category, BigDecimal limit) {}
