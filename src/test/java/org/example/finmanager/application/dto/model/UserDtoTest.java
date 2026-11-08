package org.example.finmanager.application.dto.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UserDtoTest {

  private static final String LOGIN = "alice";
  private static final String PASSWORD = "secret-test-pw";

  @Test
  public void toStringShowsLoginAndHidesPassword() {
    UserDto user = new UserDto(LOGIN, PASSWORD);

    assertThat(user.toString()).contains(LOGIN).doesNotContain(PASSWORD);
  }
}
