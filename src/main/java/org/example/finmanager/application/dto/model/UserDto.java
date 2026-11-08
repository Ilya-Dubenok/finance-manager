package org.example.finmanager.application.dto.model;

/**
 * Пользователь, заданный в файле пользователей.
 *
 * @param login логин
 * @param password пароль в открытом виде (D-05); в {@code toString} не попадает
 */
public record UserDto(String login, String password) {

  private static final String TO_STRING_FORMAT = "UserDto[login=%s]";

  @Override
  public String toString() {
    return TO_STRING_FORMAT.formatted(login);
  }
}
