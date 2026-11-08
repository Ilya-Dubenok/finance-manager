package org.example.finmanager.application.dto.model;

/**
 * Категория кошелька.
 *
 * @param name название в том регистре, в котором введено
 * @param type тип категории
 */
public record CategoryDto(String name, OperationType type) {}
