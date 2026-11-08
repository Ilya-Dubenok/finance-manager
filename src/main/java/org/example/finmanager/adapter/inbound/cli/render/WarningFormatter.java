package org.example.finmanager.adapter.inbound.cli.render;

import lombok.experimental.UtilityClass;

/** Пометка предупреждений в консоли: ASCII-маркер {@code [!]} (D-20). */
@UtilityClass
public class WarningFormatter {

  private static final String MARKER = "[!] ";

  /**
   * Помечает текст как предупреждение.
   *
   * @param text текст предупреждения
   * @return текст с маркером в начале
   */
  public String format(String text) {
    return MARKER + text;
  }
}
