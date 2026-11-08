package org.example.finmanager.adapter.inbound.cli.dto;

import java.nio.file.Path;

/**
 * Данные диалога импорта.
 *
 * @param source путь к файлу снимка
 */
public record ImportForm(Path source) {}
