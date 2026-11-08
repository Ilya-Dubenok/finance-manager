package org.example.finmanager.adapter.inbound.cli.dto;

import org.example.finmanager.application.dto.model.OperationType;

/**
 * Данные диалога «переименовать категорию».
 *
 * @param type тип категории
 * @param name текущее название
 * @param newName новое название
 */
public record RenameCategoryForm(OperationType type, String name, String newName) {}
