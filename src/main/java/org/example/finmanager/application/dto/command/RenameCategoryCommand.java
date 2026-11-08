package org.example.finmanager.application.dto.command;

import org.example.finmanager.application.dto.model.OperationType;

/**
 * Команда переименовать категорию.
 *
 * @param login логин владельца кошелька
 * @param type тип категории
 * @param name текущее название
 * @param newName новое название
 */
public record RenameCategoryCommand(
    String login, OperationType type, String name, String newName) {}
