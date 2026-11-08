package org.example.finmanager.adapter.inbound.cli.dto;

import java.math.BigDecimal;

/**
 * Данные диалога «установить бюджет».
 *
 * @param category введённое название расходной категории
 * @param limit лимит
 */
public record SetBudgetForm(String category, BigDecimal limit) {}
