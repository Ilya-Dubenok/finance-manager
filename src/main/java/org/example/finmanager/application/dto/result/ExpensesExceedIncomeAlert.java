package org.example.finmanager.application.dto.result;

import java.math.BigDecimal;

/**
 * A-2: сумма расходов за всё время больше суммы доходов.
 *
 * @param income доходы за всё время
 * @param expense расходы за всё время
 */
public record ExpensesExceedIncomeAlert(BigDecimal income, BigDecimal expense)
    implements AlertDto {}
