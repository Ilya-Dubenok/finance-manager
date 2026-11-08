package org.example.finmanager.adapter.inbound.cli.menu.model;

/**
 * Пункт меню.
 *
 * @param number номер, который вводит пользователь
 * @param label подпись пункта
 * @param action действие, выполняемое при выборе
 */
public record MenuItem(int number, String label, MenuAction action) {

  /** Создаёт пункт с номером 0, который закрывает текущее меню. */
  public static MenuItem leave(String label) {
    return new MenuItem(0, label, () -> MenuOutcome.LEAVE);
  }
}
