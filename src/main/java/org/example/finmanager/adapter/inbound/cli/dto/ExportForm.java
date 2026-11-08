package org.example.finmanager.adapter.inbound.cli.dto;

import java.nio.file.Path;

/**
 * Данные диалога экспорта.
 *
 * @param target путь или {@code null}, если пользователь выбрал путь по умолчанию
 * @param overwrite {@code true}, если пользователь разрешил заменить существующий файл
 */
public record ExportForm(Path target, boolean overwrite) {}
