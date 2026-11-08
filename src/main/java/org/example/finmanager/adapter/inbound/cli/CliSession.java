package org.example.finmanager.adapter.inbound.cli;

import org.springframework.stereotype.Component;

/** Сессия консольного приложения: логин вошедшего пользователя. */
@Component
public class CliSession {

  private static final String NOT_LOGGED_IN_MESSAGE = "No user is logged in";

  /** Логин вошедшего пользователя; {@code null}, пока никто не вошёл. */
  private String login;

  /** Возвращает {@code true}, если пользователь вошёл. */
  public boolean isAuthenticated() {
    return login != null;
  }

  /**
   * Возвращает логин вошедшего пользователя.
   *
   * @throws IllegalStateException если никто не вошёл
   */
  public String login() {
    if (login == null) {
      throw new IllegalStateException(NOT_LOGGED_IN_MESSAGE);
    }
    return login;
  }

  /** Запоминает вошедшего пользователя. */
  public void logIn(String login) {
    this.login = login;
  }

  /** Завершает сессию. */
  public void logOut() {
    this.login = null;
  }
}
