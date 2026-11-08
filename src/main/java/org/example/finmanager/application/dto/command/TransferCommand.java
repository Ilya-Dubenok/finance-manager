package org.example.finmanager.application.dto.command;

import java.math.BigDecimal;

/**
 * Команда перевести деньги другому пользователю.
 *
 * @param fromLogin логин отправителя
 * @param toLogin логин получателя
 * @param amount сумма
 * @param comment комментарий или {@code null}
 */
public record TransferCommand(
    String fromLogin, String toLogin, BigDecimal amount, String comment) {}
