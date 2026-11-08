package org.example.finmanager.application.dto.result;

import java.math.BigDecimal;

/**
 * Состояние бюджета на всё время.
 *
 * @param category название категории
 * @param limit лимит
 * @param spent сумма расходов категории за всё время
 * @param remaining лимит минус потрачено; меньше нуля означает превышение
 */
public record BudgetStatusDto(
    String category, BigDecimal limit, BigDecimal spent, BigDecimal remaining) {}
