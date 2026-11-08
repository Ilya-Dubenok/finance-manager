package org.example.finmanager.adapter.inbound.cli.dto;

import org.example.finmanager.application.dto.model.OperationType;

/**
 * Данные диалога «удалить категорию».
 *
 * @param type тип категории
 * @param name название
 */
public record DeleteCategoryForm(OperationType type, String name) {}
