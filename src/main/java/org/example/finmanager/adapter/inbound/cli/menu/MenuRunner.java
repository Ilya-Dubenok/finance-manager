package org.example.finmanager.adapter.inbound.cli.menu;

import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finmanager.adapter.inbound.cli.input.DialogCancelledException;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.io.InputClosedException;
import org.example.finmanager.adapter.inbound.cli.menu.model.Menu;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuItem;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.exception.ApplicationException;
import org.springframework.stereotype.Component;

/**
 * Показывает меню, читает выбор пользователя и вызывает действие выбранного пункта.
 *
 * <p>Меню строится заново при каждом показе, поэтому заголовок может меняться (например, баланс).
 * Ошибки действия не прерывают цикл: отказ сервиса и отмена диалога дают сообщение, любое другое
 * исключение пишется в лог, пользователь видит сообщение о внутренней ошибке. Конец ввода цикл
 * не перехватывает.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MenuRunner {

  private static final String ITEM_LINE_FORMAT = "%d. %s";

  private final ConsoleIo io;
  private final MessageCatalog messages;

  /**
   * Повторяет показ меню и обработку выбора, пока действие не попросит закрыть меню.
   *
   * @param menuSource строит меню; вызывается перед каждым показом
   * @return {@code LEAVE}, если меню закрыто пунктом «Назад», {@code EXIT}, если пользователь
   *     вышел из приложения
   */
  public MenuOutcome run(Supplier<Menu> menuSource) {
    MenuOutcome outcome = MenuOutcome.STAY;
    while (outcome == MenuOutcome.STAY) {
      Menu menu = menuSource.get();
      show(menu);
      String input = io.readLine().trim();
      outcome = handle(menu, input);
    }
    return outcome;
  }

  /**
   * Показывает вложенное меню из действия родительского меню.
   *
   * @param submenuSource строит вложенное меню
   * @return {@code STAY}, если пользователь вернулся «Назад» и родительское меню остаётся на
   *     экране, {@code EXIT}, если он вышел из приложения
   */
  public MenuOutcome open(Supplier<Menu> submenuSource) {
    MenuOutcome outcome = run(submenuSource);
    return outcome == MenuOutcome.EXIT ? MenuOutcome.EXIT : MenuOutcome.STAY;
  }

  private void show(Menu menu) {
    io.println();
    io.println(menu.title());
    for (MenuItem item : menu.items()) {
      io.println(ITEM_LINE_FORMAT.formatted(item.number(), item.label()));
    }
    io.prompt(messages.get(MessageKeys.MENU_PROMPT));
  }

  private MenuOutcome handle(Menu menu, String input) {
    if (input.isEmpty()) {
      io.println(messages.get(MessageKeys.ERROR_EMPTY_INPUT));
      return MenuOutcome.STAY;
    }
    Optional<MenuItem> item = find(menu, input);
    if (item.isEmpty()) {
      String max = String.valueOf(maxNumber(menu));
      io.println(messages.get(MessageKeys.ERROR_UNKNOWN_MENU_ITEM, input, max));
      return MenuOutcome.STAY;
    }
    return execute(item.get());
  }

  private MenuOutcome execute(MenuItem item) {
    try {
      return item.action().execute();
    } catch (InputClosedException e) {
      throw e;
    } catch (ApplicationException e) {
      io.println(messages.describe(e));
      return MenuOutcome.STAY;
    } catch (DialogCancelledException e) {
      io.println(messages.get(MessageKeys.DIALOG_CANCELLED));
      return MenuOutcome.STAY;
    } catch (RuntimeException e) {
      log.error("Unexpected error in menu action", e);
      io.println(messages.get(MessageKeys.ERROR_INTERNAL));
      return MenuOutcome.STAY;
    }
  }

  private Optional<MenuItem> find(Menu menu, String input) {
    return menu.items().stream()
        .filter(item -> Integer.toString(item.number()).equals(input))
        .findFirst();
  }

  private int maxNumber(Menu menu) {
    return menu.items().stream().mapToInt(MenuItem::number).max().orElse(0);
  }
}
