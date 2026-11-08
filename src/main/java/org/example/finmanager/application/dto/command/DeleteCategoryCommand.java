package org.example.finmanager.application.dto.command;

import org.example.finmanager.application.dto.model.OperationType;

/**
 * Команда удалить категорию.
 *
 * @param login логин владельца кошелька
 * @param type тип категории
 * @param name название
 */
public record DeleteCategoryCommand(String login, OperationType type, String name) {}
