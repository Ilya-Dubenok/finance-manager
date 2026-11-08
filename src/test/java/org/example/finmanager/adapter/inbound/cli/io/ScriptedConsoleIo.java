package org.example.finmanager.adapter.inbound.cli.io;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Тестовый {@link ConsoleIo}: отдаёт заранее заданные строки и копит вывод. */
public class ScriptedConsoleIo implements ConsoleIo {

  private final Deque<String> input;
  private final StringBuilder output = new StringBuilder();
  private int passwordReads;

  /**
   * Создаёт ввод-вывод с заданным сценарием.
   *
   * @param lines строки, которые «вводит» пользователь; после них наступает конец ввода
   */
  public ScriptedConsoleIo(String... lines) {
    this.input = new ArrayDeque<>(List.of(lines));
  }

  @Override
  public String readLine() {
    return nextLine();
  }

  @Override
  public String readPassword() {
    passwordReads++;
    return nextLine();
  }

  @Override
  public void print(String text) {
    output.append(text);
  }

  @Override
  public void println(String text) {
    output.append(text).append(System.lineSeparator());
  }

  @Override
  public void println() {
    output.append(System.lineSeparator());
  }

  /** Возвращает весь накопленный вывод. */
  public String output() {
    return output.toString();
  }

  /** Возвращает, сколько раз приложение запросило пароль через {@link #readPassword()}. */
  public int passwordReads() {
    return passwordReads;
  }

  private String nextLine() {
    String line = input.pollFirst();
    if (line == null) {
      throw new InputClosedException();
    }
    return line;
  }
}
