package org.example.finmanager.adapter.inbound.cli.dto;

import java.math.BigDecimal;

/**
 * Данные диалога «перевод».
 *
 * @param toLogin введённый логин получателя
 * @param amount сумма
 * @param comment комментарий; пустая строка, если пропущен
 */
public record TransferForm(String toLogin, BigDecimal amount, String comment) {}
