package org.example.finmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import org.example.finmanager.RefusalAssert;
import org.example.finmanager.application.dto.model.UserDto;
import org.example.finmanager.application.dto.result.AuthenticatedUserDto;
import org.example.finmanager.application.exception.ErrorCode;
import org.example.finmanager.application.port.outbound.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class UserAccessServiceTest {

  private static final String LOGIN = "alice";
  private static final String PASSWORD = "alice-test-pw";
  private static final String WRONG_PASSWORD = "wrong-pw";

  private final Map<String, UserDto> users = Map.of(LOGIN, new UserDto(LOGIN, PASSWORD));
  private final UserRepositoryPort repository = login -> Optional.ofNullable(users.get(login));
  private final FakeWalletPort wallets = new FakeWalletPort();
  private final UserAccessService service = new UserAccessService(repository, wallets);

  @Test
  public void returnsUserWhenCredentialsMatch() {
    AuthenticatedUserDto user = service.login(LOGIN, PASSWORD);

    assertThat(user.login()).isEqualTo(LOGIN);
  }

  @Test
  public void loadsTheWalletOfTheUserOnSuccessfulLogin() {
    service.login(LOGIN, PASSWORD);

    assertThat(wallets.requestedLogins()).containsExactly(LOGIN);
  }

  @Test
  public void doesNotTouchAnyWalletWhenLoginIsRefused() {
    RefusalAssert.assertRefusal(
        () -> service.login(LOGIN, WRONG_PASSWORD), ErrorCode.INVALID_CREDENTIALS);

    assertThat(wallets.requestedLogins()).isEmpty();
  }

  @Test
  public void rejectsWrongPassword() {
    RefusalAssert.assertRefusal(
        () -> service.login(LOGIN, WRONG_PASSWORD), ErrorCode.INVALID_CREDENTIALS);
  }

  @Test
  public void rejectsUnknownLogin() {
    RefusalAssert.assertRefusal(
        () -> service.login("nobody", PASSWORD), ErrorCode.INVALID_CREDENTIALS);
  }

  @Test
  public void treatsLoginAsCaseSensitive() {
    RefusalAssert.assertRefusal(
        () -> service.login("ALICE", PASSWORD), ErrorCode.INVALID_CREDENTIALS);
  }

  @Test
  public void treatsPasswordAsCaseSensitive() {
    RefusalAssert.assertRefusal(
        () -> service.login(LOGIN, PASSWORD.toUpperCase()), ErrorCode.INVALID_CREDENTIALS);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  public void rejectsBlankLogin(String login) {
    RefusalAssert.assertRefusal(() -> service.login(login, PASSWORD), ErrorCode.EMPTY_VALUE);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  public void rejectsBlankPassword(String password) {
    RefusalAssert.assertRefusal(() -> service.login(LOGIN, password), ErrorCode.EMPTY_VALUE);
  }

  @Test
  public void refusalDoesNotRevealWhichPartWasWrong() {
    RefusalAssert.assertRefusalWithArgs(
        () -> service.login(LOGIN, WRONG_PASSWORD), ErrorCode.INVALID_CREDENTIALS);
    RefusalAssert.assertRefusalWithArgs(
        () -> service.login("nobody", PASSWORD), ErrorCode.INVALID_CREDENTIALS);
  }
}
