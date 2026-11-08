package org.example.finmanager.application.dto.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.example.finmanager.application.dto.model.OperationType;

/**
 * Команда добавить доход или расход.
 *
 * @param login логин владельца кошелька
 * @param type тип операции
 * @param category название категории того же типа
 * @param amount сумма
 * @param date дата или {@code null}, тогда сегодня
 * @param comment комментарий или {@code null}
 */
public record AddOperationCommand(
    String login,
    OperationType type,
    String category,
    BigDecimal amount,
    LocalDate date,
    String comment) {}
