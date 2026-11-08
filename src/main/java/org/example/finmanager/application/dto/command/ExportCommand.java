package org.example.finmanager.application.dto.command;

import java.nio.file.Path;

/**
 * Команда экспортировать данные в файл.
 *
 * @param login логин владельца кошелька
 * @param target путь к файлу или {@code null}, тогда файл создаётся в каталоге экспорта с именем по
 *     умолчанию
 * @param overwrite {@code true}, если существующий файл можно заменить
 */
public record ExportCommand(String login, Path target, boolean overwrite) {}
