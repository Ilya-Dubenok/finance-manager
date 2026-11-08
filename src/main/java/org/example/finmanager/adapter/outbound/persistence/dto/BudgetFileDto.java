package org.example.finmanager.adapter.outbound.persistence.dto;

/**
 * Бюджет в файле кошелька.
 *
 * @param category название расходной категории
 * @param limit лимит, десятичная строка
 */
public record BudgetFileDto(String category, String limit) {}
