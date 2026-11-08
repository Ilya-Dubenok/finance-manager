package org.example.finmanager.adapter.inbound.cli;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.io.InputClosedException;
import org.example.finmanager.adapter.inbound.cli.menu.GuestMenu;
import org.example.finmanager.adapter.inbound.cli.menu.MenuRunner;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.port.inbound.WalletPort;
import org.springframework.stereotype.Component;

/**
 * Цикл чтения команд пользователя: показывает меню и завершается по выходу или концу ввода. При
 * закрытии контекста (обычный выход, конец ввода, Ctrl+C) записывает изменённые кошельки.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CliApplication {

  private final ConsoleIo io;
  private final MenuRunner menuRunner;
  private final GuestMenu guestMenu;
  private final MessageCatalog messages;
  private final WalletPort wallets;

  /** Запускает цикл и возвращает управление, когда пользователь вышел или ввод закончился. */
  public void run() {
    log.info("CLI started");
    try {
      menuRunner.run(guestMenu::build);
    } catch (InputClosedException e) {
      io.println();
      log.info("Input closed, leaving the application");
    }
    io.println(messages.get(MessageKeys.APP_GOODBYE));
  }

  /**
   * Записывает изменённые кошельки при закрытии контекста. Вопроса о повторе здесь нет: ввод может
   * быть занят главным потоком, поэтому ошибка пишется в лог и в {@code stderr}. Если всё уже
   * записано при выходе через меню, ничего не делает.
   */
  @PreDestroy
  public void saveOnShutdown() {
    try {
      wallets.saveChanges();
    } catch (ApplicationException e) {
      log.error("Cannot save the data on exit", e);
      System.err.println(messages.get(MessageKeys.SHUTDOWN_SAVE_FAILED, e.getArgs().toArray()));
    }
  }
}
