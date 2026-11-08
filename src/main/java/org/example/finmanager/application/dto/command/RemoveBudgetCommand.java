package org.example.finmanager.application.dto.command;

/**
 * Команда удалить бюджет.
 *
 * @param login логин владельца кошелька
 * @param category название расходной категории
 */
public record RemoveBudgetCommand(String login, String category) {}
