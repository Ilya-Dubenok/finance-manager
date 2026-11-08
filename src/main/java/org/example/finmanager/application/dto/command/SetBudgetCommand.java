package org.example.finmanager.application.dto.command;

import java.math.BigDecimal;

/**
 * Команда установить или изменить бюджет.
 *
 * @param login логин владельца кошелька
 * @param category название расходной категории
 * @param limit новый лимит
 */
public record SetBudgetCommand(String login, String category, BigDecimal limit) {}
