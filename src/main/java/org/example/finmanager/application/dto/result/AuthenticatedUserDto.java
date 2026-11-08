package org.example.finmanager.application.dto.result;

/**
 * Пользователь, успешно прошедший вход.
 *
 * @param login логин
 */
public record AuthenticatedUserDto(String login) {}
