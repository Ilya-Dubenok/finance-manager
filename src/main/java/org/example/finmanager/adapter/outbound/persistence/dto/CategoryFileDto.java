package org.example.finmanager.adapter.outbound.persistence.dto;

/**
 * Категория в файле кошелька.
 *
 * @param name название
 * @param type {@code INCOME} или {@code EXPENSE}
 */
public record CategoryFileDto(String name, String type) {}
