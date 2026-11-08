package org.example.finmanager.adapter.inbound.cli.render;

import lombok.experimental.UtilityClass;

/** Сокращение комментария для таблицы. */
@UtilityClass
public class CommentFormatter {

  private static final int MAX_LENGTH = 30;
  private static final String ELLIPSIS = "...";
  private static final String EMPTY = "";

  /**
   * Обрезает комментарий до 30 символов, заменяя конец на многоточие.
   *
   * @param comment комментарий или {@code null}
   * @return текст не длиннее 30 символов; пустая строка, если комментария нет
   */
  public String truncate(String comment) {
    if (comment == null) {
      return EMPTY;
    }
    if (comment.length() <= MAX_LENGTH) {
      return comment;
    }
    return comment.substring(0, MAX_LENGTH - ELLIPSIS.length()) + ELLIPSIS;
  }
}
