package org.example.finmanager.application.dto.result;

import java.math.BigDecimal;
import org.example.finmanager.application.dto.model.OperationType;

/**
 * Строка подсчёта по выбранным категориям.
 *
 * @param category название категории
 * @param type тип категории
 * @param amount сумма операций категории за период
 */
public record CategoryTotalRowDto(String category, OperationType type, BigDecimal amount) {}
