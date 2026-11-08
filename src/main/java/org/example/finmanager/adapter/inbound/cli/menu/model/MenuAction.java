package org.example.finmanager.adapter.inbound.cli.menu.model;

/** Действие пункта меню. */
@FunctionalInterface
public interface MenuAction {

  /**
   * Выполняет действие.
   *
   * @return что делать меню дальше: остаться или закрыться
   */
  MenuOutcome execute();
}
