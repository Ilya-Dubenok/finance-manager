package org.example.finmanager;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.example.finmanager.adapter.inbound.cli.CliApplication;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.config.AppConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** Точка входа: поднимает контекст Spring и запускает цикл команд. */
@Slf4j
public final class FinanceManagerApplication {

  private static final int EXIT_CODE_OK = 0;
  private static final int EXIT_CODE_STARTUP_FAILED = 1;

  public static void main(String[] args) {
    int exitCode = new FinanceManagerApplication().run();
    if (exitCode != EXIT_CODE_OK) {
      System.exit(exitCode);
    }
  }

  /**
   * Запускает приложение.
   *
   * @return код выхода: 0 после обычного завершения, 1, если приложение не удалось запустить
   */
  public int run() {
    try (AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(AppConfig.class)) {
      context.registerShutdownHook();
      context.getBean(CliApplication.class).run();
      return EXIT_CODE_OK;
    } catch (BeansException e) {
      return reportStartupFailure(e);
    }
  }

  private int reportStartupFailure(BeansException failure) {
    log.error("Application failed to start", failure);
    MessageCatalog messages = new MessageCatalog();
    Optional<ApplicationException> refusal = findRefusal(failure);
    String text =
        refusal.isPresent()
            ? messages.describe(refusal.get())
            : messages.get(MessageKeys.ERROR_INTERNAL);
    System.err.println(text);
    return EXIT_CODE_STARTUP_FAILED;
  }

  private Optional<ApplicationException> findRefusal(Throwable failure) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof ApplicationException refusal) {
        return Optional.of(refusal);
      }
    }
    return Optional.empty();
  }
}
