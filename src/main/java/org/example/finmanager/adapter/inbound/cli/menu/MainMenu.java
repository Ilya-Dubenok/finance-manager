package org.example.finmanager.adapter.inbound.cli.menu;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.action.ExitAction;
import org.example.finmanager.adapter.inbound.cli.action.HelpAction;
import org.example.finmanager.adapter.inbound.cli.action.LogoutAction;
import org.example.finmanager.adapter.inbound.cli.action.TransferAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.Menu;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuItem;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.port.inbound.StatisticsPort;
import org.springframework.stereotype.Component;

/**
 * Главное меню вошедшего пользователя; в заголовке логин и баланс.
 *
 * <p>Номера пунктов — из спецификации; пункты добавляются по мере реализации срезов.
 */
@Component
@RequiredArgsConstructor
public class MainMenu {

  private static final int OPERATIONS_NUMBER = 1;
  private static final int CATEGORIES_NUMBER = 2;
  private static final int STATISTICS_NUMBER = 3;
  private static final int TRANSFER_NUMBER = 4;
  private static final int REPORTS_NUMBER = 5;
  private static final int HELP_NUMBER = 6;
  private static final int LOGOUT_NUMBER = 7;
  private static final int EXIT_NUMBER = 0;

  private final MessageCatalog messages;
  private final CliSession session;
  private final StatisticsPort statistics;
  private final MenuRunner menuRunner;
  private final OperationsMenu operationsMenu;
  private final CategoriesMenu categoriesMenu;
  private final StatisticsMenu statisticsMenu;
  private final ReportsMenu reportsMenu;
  private final TransferAction transferAction;
  private final HelpAction helpAction;
  private final LogoutAction logoutAction;
  private final ExitAction exitAction;

  /** Строит меню для пользователя текущей сессии. */
  public Menu build() {
    String login = session.login();
    BigDecimal balance = statistics.balance(login);
    List<MenuItem> items =
        List.of(
            operationsItem(),
            categoriesItem(),
            statisticsItem(),
            transferItem(),
            reportsItem(),
            helpItem(),
            logoutItem(),
            exitItem());
    String title = messages.get(MessageKeys.MENU_MAIN_TITLE, login, balance);
    return new Menu(title, items);
  }

  private MenuItem operationsItem() {
    String label = messages.get(MessageKeys.MENU_MAIN_OPERATIONS);
    return new MenuItem(OPERATIONS_NUMBER, label, () -> menuRunner.open(operationsMenu::build));
  }

  private MenuItem categoriesItem() {
    String label = messages.get(MessageKeys.MENU_MAIN_CATEGORIES);
    return new MenuItem(CATEGORIES_NUMBER, label, () -> menuRunner.open(categoriesMenu::build));
  }

  private MenuItem statisticsItem() {
    String label = messages.get(MessageKeys.MENU_MAIN_STATISTICS);
    return new MenuItem(STATISTICS_NUMBER, label, () -> menuRunner.open(statisticsMenu::build));
  }

  private MenuItem transferItem() {
    String label = messages.get(MessageKeys.MENU_MAIN_TRANSFER);
    return new MenuItem(TRANSFER_NUMBER, label, transferAction);
  }

  private MenuItem reportsItem() {
    String label = messages.get(MessageKeys.MENU_MAIN_REPORTS);
    return new MenuItem(REPORTS_NUMBER, label, () -> menuRunner.open(reportsMenu::build));
  }

  private MenuItem helpItem() {
    String label = messages.get(MessageKeys.MENU_MAIN_HELP);
    return new MenuItem(HELP_NUMBER, label, helpAction);
  }

  private MenuItem logoutItem() {
    String label = messages.get(MessageKeys.MENU_MAIN_LOGOUT);
    return new MenuItem(LOGOUT_NUMBER, label, logoutAction);
  }

  private MenuItem exitItem() {
    String label = messages.get(MessageKeys.MENU_MAIN_EXIT);
    return new MenuItem(EXIT_NUMBER, label, exitAction);
  }
}
