package org.example.finmanager.adapter.inbound.cli.dto;

import org.example.finmanager.application.dto.model.OperationType;

/**
 * Данные диалога «создать категорию».
 *
 * @param type тип категории
 * @param name введённое название
 */
public record CreateCategoryForm(OperationType type, String name) {}
