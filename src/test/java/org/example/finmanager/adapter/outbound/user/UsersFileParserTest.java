package org.example.finmanager.adapter.outbound.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.example.finmanager.application.dto.model.UserDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class UsersFileParserTest {

  private static final String FILE = "users.properties";
  private static final String ALICE_LINE = "alice=pw-one";

  private final UsersFileParser parser = new UsersFileParser();

  @Test
  public void parsesLoginsAndPasswords() {
    Map<String, UserDto> users = parser.parse(FILE, List.of(ALICE_LINE, "bob=pw-two"));

    assertThat(users).containsOnlyKeys("alice", "bob");
    assertThat(users.get("alice").password()).isEqualTo("pw-one");
    assertThat(users.get("bob").password()).isEqualTo("pw-two");
  }

  @Test
  public void skipsCommentsAndBlankLines() {
    List<String> lines = List.of("# comment", "", "   ", "  # indented comment", ALICE_LINE);

    Map<String, UserDto> users = parser.parse(FILE, lines);

    assertThat(users).containsOnlyKeys("alice");
  }

  @Test
  public void splitsAtFirstEqualsSign() {
    Map<String, UserDto> users = parser.parse(FILE, List.of("alice=a=b"));

    assertThat(users.get("alice").password()).isEqualTo("a=b");
  }

  @Test
  public void keepsPasswordAsIs() {
    Map<String, UserDto> users = parser.parse(FILE, List.of("alice= pw "));

    assertThat(users.get("alice").password()).isEqualTo(" pw ");
  }

  @Test
  public void trimsLogin() {
    Map<String, UserDto> users = parser.parse(FILE, List.of(" alice =pw"));

    assertThat(users).containsOnlyKeys("alice");
  }

  @Test
  public void ignoresByteOrderMarkOnFirstLine() {
    Map<String, UserDto> users = parser.parse(FILE, List.of("﻿" + ALICE_LINE));

    assertThat(users).containsOnlyKeys("alice");
  }

  @Test
  public void rejectsLineWithoutSeparatorAndReportsLineNumber() {
    List<String> lines = List.of(ALICE_LINE, "bob-without-password");

    assertRefusal(lines, ErrorCode.USERS_FILE_NO_SEPARATOR, FILE, 2);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "al=pw",
        "bad login=pw",
        "alicé=pw",
        "=pw",
        "a-very-long-login-over-32-chars-xx=pw"
      })
  public void rejectsInvalidLogin(String line) {
    String login = line.substring(0, line.indexOf('='));

    assertRefusal(List.of(line), ErrorCode.USERS_FILE_BAD_LOGIN, FILE, 1, login);
  }

  @ParameterizedTest
  @ValueSource(strings = {"alice=", "alice=   "})
  public void rejectsBlankPassword(String line) {
    assertRefusal(List.of(line), ErrorCode.USERS_FILE_EMPTY_PASSWORD, FILE, 1, "alice");
  }

  @Test
  public void rejectsDuplicateLoginIgnoringCase() {
    List<String> lines = List.of(ALICE_LINE, "", "ALICE=pw-two");

    assertRefusal(lines, ErrorCode.USERS_FILE_DUPLICATE_LOGIN, FILE, 3, "ALICE", 1);
  }

  @Test
  public void rejectsFileWithoutUsers() {
    assertRefusal(List.of("# only a comment", ""), ErrorCode.USERS_FILE_NO_USERS, FILE);
  }

  private void assertRefusal(List<String> lines, ErrorCode code, Object... args) {
    assertThatThrownBy(() -> parser.parse(FILE, lines))
        .isInstanceOfSatisfying(
            ApplicationException.class,
            e -> {
              assertThat(e.getCode()).isEqualTo(code);
              assertThat(e.getArgs()).containsExactly(args);
            });
  }
}
