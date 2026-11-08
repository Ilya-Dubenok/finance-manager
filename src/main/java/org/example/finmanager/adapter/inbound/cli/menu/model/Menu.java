package org.example.finmanager.adapter.inbound.cli.menu.model;

import java.util.List;

/**
 * Меню: заголовок и пронумерованные пункты.
 *
 * @param title заголовок, выводится над пунктами
 * @param items пункты в порядке показа
 */
public record Menu(String title, List<MenuItem> items) {

  /** Копирует список пунктов, чтобы меню оставалось неизменяемым. */
  public Menu {
    items = List.copyOf(items);
  }
}
