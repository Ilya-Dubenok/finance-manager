package org.example.finmanager.application.dto.result;

import java.math.BigDecimal;

/**
 * A-1: у расходной категории есть бюджет, и остаток меньше нуля.
 *
 * @param category название категории
 * @param limit лимит бюджета
 * @param spent потрачено за всё время
 */
public record BudgetExceededAlert(String category, BigDecimal limit, BigDecimal spent)
    implements AlertDto {}
