package org.example.finmanager.adapter.inbound.cli.message;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import org.example.finmanager.adapter.inbound.cli.render.DateFormatter;
import org.example.finmanager.adapter.inbound.cli.render.MoneyFormatter;
import org.example.finmanager.adapter.inbound.cli.render.TypeFormatter;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.exception.ApplicationException;
import org.springframework.stereotype.Component;

/** Каталог пользовательских сообщений на английском языке. */
@Component
public class MessageCatalog {

  private static final String BUNDLE_NAME = "messages";
  private static final Locale MESSAGES_LOCALE = Locale.ENGLISH;
  private static final String LIST_SEPARATOR = ", ";

  private final ResourceBundle bundle;

  /** Загружает сообщения из {@code messages_en.properties}. */
  public MessageCatalog() {
    this.bundle = ResourceBundle.getBundle(BUNDLE_NAME, MESSAGES_LOCALE);
  }

  /**
   * Возвращает сообщение с подставленными аргументами.
   *
   * @param key ключ сообщения, например {@code E-002}
   * @param args значения для подстановки на места {@code {0}}, {@code {1}} и так далее
   * @return готовый текст
   */
  public String get(String key, Object... args) {
    String pattern = bundle.getString(key);
    Object[] texts = Arrays.stream(args).map(this::toText).toArray();
    return new MessageFormat(pattern, MESSAGES_LOCALE).format(texts);
  }

  /**
   * Возвращает сообщение для отказа сервиса.
   *
   * @param exception отказ с кодом и аргументами
   * @return готовый текст из каталога
   */
  public String describe(ApplicationException exception) {
    String key = exception.getCode().getCatalogId();
    return get(key, exception.getArgs().toArray());
  }

  private String toText(Object arg) {
    if (arg instanceof BigDecimal amount) {
      return MoneyFormatter.format(amount);
    }
    if (arg instanceof LocalDate date) {
      return DateFormatter.format(date);
    }
    if (arg instanceof OperationType type) {
      return TypeFormatter.format(type);
    }
    if (arg instanceof Collection<?> items) {
      return items.stream().map(this::toText).collect(Collectors.joining(LIST_SEPARATOR));
    }
    return String.valueOf(arg);
  }
}
