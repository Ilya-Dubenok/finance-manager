package org.example.finmanager.adapter.inbound.cli.dto;

/**
 * Данные диалога «удалить бюджет».
 *
 * @param category введённое название расходной категории
 */
public record RemoveBudgetForm(String category) {}
