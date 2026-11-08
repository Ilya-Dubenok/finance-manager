package org.example.finmanager.application.port.inbound;

import org.example.finmanager.application.dto.result.AuthenticatedUserDto;

/** Доступ пользователей к приложению. */
public interface UserAccessPort {

  /**
   * Проверяет логин и пароль.
   *
   * @param login логин, регистр учитывается
   * @param password пароль
   * @return вошедший пользователь
   * @throws org.example.finmanager.application.exception.ApplicationException {@code EMPTY_VALUE},
   *     если логин или пароль пустые, {@code INVALID_CREDENTIALS}, если пара неверная
   */
  AuthenticatedUserDto login(String login, String password);

  /**
   * Проверяет, что пользователь с таким логином задан в файле пользователей.
   *
   * @param login логин, регистр учитывается
   * @return {@code true}, если пользователь есть
   */
  boolean userExists(String login);
}
