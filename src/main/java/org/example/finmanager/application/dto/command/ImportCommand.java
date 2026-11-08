package org.example.finmanager.application.dto.command;

import java.nio.file.Path;

/**
 * Команда импортировать снимок кошелька, заменив кошелёк пользователя.
 *
 * @param login логин владельца кошелька
 * @param source путь к файлу снимка
 */
public record ImportCommand(String login, Path source) {}
