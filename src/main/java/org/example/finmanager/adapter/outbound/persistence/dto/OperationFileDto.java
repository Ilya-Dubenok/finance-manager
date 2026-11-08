package org.example.finmanager.adapter.outbound.persistence.dto;

/**
 * Операция в файле кошелька.
 *
 * @param type {@code INCOME} или {@code EXPENSE}
 * @param category название категории
 * @param amount сумма, десятичная строка
 * @param date дата в формате ISO {@code yyyy-MM-dd}
 * @param comment комментарий или {@code null}
 * @param counterparty логин второй стороны или {@code null}
 */
public record OperationFileDto(
    String type,
    String category,
    String amount,
    String date,
    String comment,
    String counterparty) {}
