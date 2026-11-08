package org.example.finmanager.adapter.inbound.cli.io;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import org.springframework.stereotype.Component;

/**
 * Ввод и вывод через {@code System.in} и {@code System.out} в кодировке консоли.
 *
 * <p>Пароль без эха читается через {@link Console}, только когда ввод идёт с настоящего
 * терминала; иначе (перенаправленный ввод, IDE) пароль читается обычной строкой.
 */
@Component
public class SystemConsoleIo implements ConsoleIo {

  private static final String STDIN_ENCODING_PROPERTY = "stdin.encoding";
  private static final char ERASED_CHAR = '\0';

  /** Консоль JDK; может быть {@code null}, а при вводе не с терминала не используется. */
  private final Console console;

  private final BufferedReader reader;
  private final PrintStream out;

  /** Создаёт ввод-вывод; кодировка ввода берётся у консоли, если она подключена. */
  public SystemConsoleIo() {
    this.console = System.console();
    Charset charset = inputCharset();
    InputStreamReader source = new InputStreamReader(System.in, charset);
    this.reader = new BufferedReader(source);
    this.out = System.out;
  }

  @Override
  public String readLine() {
    try {
      String line = reader.readLine();
      if (line == null) {
        throw new InputClosedException();
      }
      return line;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public String readPassword() {
    if (!isTerminal()) {
      return readLine();
    }
    char[] chars = console.readPassword();
    if (chars == null) {
      throw new InputClosedException();
    }
    String password = new String(chars);
    Arrays.fill(chars, ERASED_CHAR);
    return password;
  }

  @Override
  public void print(String text) {
    out.print(text);
    out.flush();
  }

  @Override
  public void println(String text) {
    out.println(text);
    out.flush();
  }

  @Override
  public void println() {
    out.println();
    out.flush();
  }

  private boolean isTerminal() {
    return console != null && console.isTerminal();
  }

  private Charset inputCharset() {
    if (isTerminal()) {
      return console.charset();
    }
    String name = System.getProperty(STDIN_ENCODING_PROPERTY);
    return name == null ? Charset.defaultCharset() : Charset.forName(name);
  }
}
