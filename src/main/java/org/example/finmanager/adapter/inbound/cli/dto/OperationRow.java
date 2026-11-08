package org.example.finmanager.adapter.inbound.cli.dto;

/**
 * Строка таблицы операций: все значения уже превращены в текст для вывода.
 *
 * @param date дата
 * @param type название типа
 * @param category категория
 * @param amount сумма в формате вывода
 * @param counterparty логин второй стороны или {@code null}
 * @param comment комментарий, при необходимости сокращённый
 */
public record OperationRow(
    String date,
    String type,
    String category,
    String amount,
    String counterparty,
    String comment) {}
