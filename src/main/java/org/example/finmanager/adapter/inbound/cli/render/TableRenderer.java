package org.example.finmanager.adapter.inbound.cli.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.experimental.UtilityClass;

/**
 * Рисует таблицу рамкой из {@code +}, {@code -} и {@code |}: тексты слева, числа справа, ширина
 * колонки по самой длинной ячейке (D-20).
 */
@UtilityClass
public class TableRenderer {

  private static final String CORNER = "+";
  private static final String BORDER_FILL = "-";
  private static final String ROW_START = "| ";
  private static final String CELL_SEPARATOR = " | ";
  private static final String ROW_END = " |";
  private static final int BORDER_PADDING = 2;
  private static final String SPACE = " ";
  private static final String EMPTY = "";

  /**
   * Рисует таблицу.
   *
   * @param headers заголовки колонок
   * @param rows строки; в каждой столько ячеек, сколько заголовков; {@code null} считается пустой
   * @param rightAligned номера колонок (с нуля), выровненных вправо
   * @return таблица из строк, разделённых переводом строки, без перевода строки в конце
   */
  public String render(List<String> headers, List<List<String>> rows, Set<Integer> rightAligned) {
    List<Integer> widths = widths(headers, rows);
    String border = border(widths);
    List<String> lines = new ArrayList<>();
    lines.add(border);
    lines.add(row(headers, widths, rightAligned));
    lines.add(border);
    for (List<String> cells : rows) {
      lines.add(row(cells, widths, rightAligned));
    }
    lines.add(border);
    return String.join(System.lineSeparator(), lines);
  }

  private List<Integer> widths(List<String> headers, List<List<String>> rows) {
    List<Integer> widths = new ArrayList<>();
    for (int column = 0; column < headers.size(); column++) {
      int width = text(headers.get(column)).length();
      for (List<String> cells : rows) {
        width = Math.max(width, text(cells.get(column)).length());
      }
      widths.add(width);
    }
    return widths;
  }

  private String border(List<Integer> widths) {
    StringBuilder border = new StringBuilder(CORNER);
    for (int width : widths) {
      border.append(BORDER_FILL.repeat(width + BORDER_PADDING)).append(CORNER);
    }
    return border.toString();
  }

  private String row(List<String> cells, List<Integer> widths, Set<Integer> rightAligned) {
    List<String> padded = new ArrayList<>();
    for (int column = 0; column < widths.size(); column++) {
      String cell = text(cells.get(column));
      int padding = widths.get(column) - cell.length();
      String filler = SPACE.repeat(padding);
      padded.add(rightAligned.contains(column) ? filler + cell : cell + filler);
    }
    return ROW_START + String.join(CELL_SEPARATOR, padded) + ROW_END;
  }

  private String text(String cell) {
    return cell == null ? EMPTY : cell;
  }
}
