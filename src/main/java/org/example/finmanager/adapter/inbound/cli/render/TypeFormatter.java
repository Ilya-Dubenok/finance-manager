package org.example.finmanager.adapter.inbound.cli.render;

import java.util.Locale;
import lombok.experimental.UtilityClass;
import org.example.finmanager.application.dto.model.OperationType;

/** Название типа операции в интерфейсе: {@code income} или {@code expense}. */
@UtilityClass
public class TypeFormatter {

  /**
   * Возвращает название типа.
   *
   * @param type тип операции
   * @return название строчными буквами
   */
  public String format(OperationType type) {
    return type.name().toLowerCase(Locale.ROOT);
  }
}
