package org.example.finmanager.application.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finmanager.application.dto.model.UserDto;
import org.example.finmanager.application.dto.result.AuthenticatedUserDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.example.finmanager.application.port.inbound.UserAccessPort;
import org.example.finmanager.application.port.inbound.WalletPort;
import org.example.finmanager.application.port.outbound.UserRepositoryPort;
import org.springframework.stereotype.Service;

/** Вход пользователей: сверяет логин и пароль с заданными пользователями. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccessService implements UserAccessPort {

  private final UserRepositoryPort users;
  private final WalletPort wallets;

  @Override
  public AuthenticatedUserDto login(String login, String password) {
    InputRules.requireNotBlank(login);
    InputRules.requireNotBlank(password);
    Optional<UserDto> user = users.findByLogin(login);
    if (user.isEmpty() || !user.get().password().equals(password)) {
      log.warn("Failed login attempt for '{}'", login);
      throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS);
    }
    wallets.get(login);
    log.info("User '{}' logged in", login);
    return new AuthenticatedUserDto(login);
  }

  @Override
  public boolean userExists(String login) {
    return users.findByLogin(login).isPresent();
  }
}
