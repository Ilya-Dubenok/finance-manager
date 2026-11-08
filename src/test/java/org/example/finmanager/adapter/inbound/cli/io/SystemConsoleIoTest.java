package org.example.finmanager.adapter.inbound.cli.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** В тестовой JVM ввод не с терминала, поэтому проверяется путь через {@code System.in}. */
public class SystemConsoleIoTest {

  private static final String STDIN_ENCODING_PROPERTY = "stdin.encoding";

  private final InputStream originalIn = System.in;
  private final PrintStream originalOut = System.out;
  private final String originalEncoding = System.getProperty(STDIN_ENCODING_PROPERTY);
  private final ByteArrayOutputStream captured = new ByteArrayOutputStream();

  @BeforeEach
  public void captureOutput() {
    System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  public void restoreSystemStreams() {
    System.setIn(originalIn);
    System.setOut(originalOut);
    if (originalEncoding == null) {
      System.clearProperty(STDIN_ENCODING_PROPERTY);
    } else {
      System.setProperty(STDIN_ENCODING_PROPERTY, originalEncoding);
    }
  }

  @Test
  public void readsLinesInConfiguredEncoding() {
    SystemConsoleIo io = ioReading("héllo\nwörld\n");

    assertThat(io.readLine()).isEqualTo("héllo");
    assertThat(io.readLine()).isEqualTo("wörld");
  }

  @Test
  public void readsPasswordAsPlainLineWhenInputIsNotTerminal() {
    SystemConsoleIo io = ioReading("pässword\n");

    assertThat(io.readPassword()).isEqualTo("pässword");
  }

  @Test
  public void throwsInputClosedAtEndOfInput() {
    SystemConsoleIo io = ioReading("");

    assertThatThrownBy(io::readLine).isInstanceOf(InputClosedException.class);
  }

  @Test
  public void throwsInputClosedWhenPasswordInputEnds() {
    SystemConsoleIo io = ioReading("");

    assertThatThrownBy(io::readPassword).isInstanceOf(InputClosedException.class);
  }

  @Test
  public void writesTextAndBlankLines() {
    SystemConsoleIo io = ioReading("");

    io.print("a");
    io.println("b");
    io.println();
    io.prompt("c");

    String lineSeparator = System.lineSeparator();
    String expected = "ab" + lineSeparator + lineSeparator + "c" + ConsoleIo.PROMPT_SEPARATOR;
    assertThat(captured.toString(StandardCharsets.UTF_8)).isEqualTo(expected);
  }

  private SystemConsoleIo ioReading(String text) {
    System.setProperty(STDIN_ENCODING_PROPERTY, StandardCharsets.UTF_8.name());
    System.setIn(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    return new SystemConsoleIo();
  }
}
