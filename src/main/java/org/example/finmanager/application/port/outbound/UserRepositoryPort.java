package org.example.finmanager.application.port.outbound;

import java.util.Optional;
import org.example.finmanager.application.dto.model.UserDto;

/** Источник пользователей; приложение его только читает. */
public interface UserRepositoryPort {

  /**
   * Ищет пользователя по логину.
   *
   * @param login логин, регистр учитывается
   * @return пользователь или пустой результат, если такого нет
   */
  Optional<UserDto> findByLogin(String login);
}
