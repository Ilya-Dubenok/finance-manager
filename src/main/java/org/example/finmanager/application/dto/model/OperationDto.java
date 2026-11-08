package org.example.finmanager.application.dto.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Операция: доход или расход.
 *
 * @param type тип
 * @param category название категории того же типа
 * @param amount сумма больше нуля, два знака после запятой
 * @param date дата операции
 * @param comment комментарий или {@code null}
 * @param counterparty логин второй стороны (только у переводов) или {@code null}
 */
public record OperationDto(
    OperationType type,
    String category,
    BigDecimal amount,
    LocalDate date,
    String comment,
    String counterparty) {}
