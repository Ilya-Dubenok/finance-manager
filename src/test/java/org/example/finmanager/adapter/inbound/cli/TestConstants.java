package org.example.finmanager.adapter.inbound.cli;

import lombok.experimental.UtilityClass;

/** Тексты и ввод, общие для нескольких тестов CLI. */
@UtilityClass
public class TestConstants {

  /** Ввод, который выбирает пункт «Выход» или «Назад» в любом меню. */
  public static final String CHOICE_LEAVE = "0";

  /** Заголовок меню до входа в аккаунт. */
  public static final String GUEST_MENU_TITLE = "=== Finance Manager ===";

  /** Прощание при выходе из приложения. */
  public static final String GOODBYE = "Goodbye!";
}
