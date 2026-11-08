package org.example.finmanager.adapter.inbound.cli.menu.model;

/** Что меню делает после выполнения действия. */
public enum MenuOutcome {
  /** Показать меню снова. */
  STAY,
  /** Закрыть меню и вернуться на уровень выше. */
  LEAVE,
  /** Закрыть все меню и завершить приложение. */
  EXIT
}
