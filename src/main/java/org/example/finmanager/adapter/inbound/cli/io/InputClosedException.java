package org.example.finmanager.adapter.inbound.cli.io;

/** Сигнализирует, что поток ввода закончился (конец файла, Ctrl+Z или Ctrl+D). */
public class InputClosedException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private static final String MESSAGE = "Input stream is closed";

  /** Создаёт исключение с фиксированным сообщением для логов. */
  public InputClosedException() {
    super(MESSAGE);
  }
}
