package org.example.finmanager.adapter.inbound.cli.input;

/** Пользователь ввёл слово отмены и отказался от диалога. */
public class DialogCancelledException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private static final String MESSAGE = "Dialog cancelled by user";

  /** Создаёт исключение с фиксированным сообщением для логов. */
  public DialogCancelledException() {
    super(MESSAGE);
  }
}
