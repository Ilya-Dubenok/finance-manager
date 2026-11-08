package org.example.finmanager.adapter.inbound.cli.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.example.finmanager.application.dto.model.OperationType;

/**
 * Данные диалога «добавить доход или расход»: значения уже разобраны из текста.
 *
 * @param type тип операции
 * @param category введённое название категории
 * @param amount сумма
 * @param date дата или {@code null}, если пользователь пропустил вопрос
 * @param comment комментарий; пустая строка, если пропущен
 */
public record AddOperationForm(
    OperationType type, String category, BigDecimal amount, LocalDate date, String comment) {}
