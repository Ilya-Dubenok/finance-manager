package org.example.finmanager.application.dto.result;

import java.math.BigDecimal;

/**
 * Сумма по категории.
 *
 * @param category название категории
 * @param amount сумма операций категории
 */
public record CategoryAmountDto(String category, BigDecimal amount) {}
