package org.example.finmanager.adapter.inbound.cli.io;

/** Консольный ввод и вывод; в тестах подменяется скриптовой реализацией. */
public interface ConsoleIo {

  /** Разделитель между приглашением и вводом пользователя. */
  String PROMPT_SEPARATOR = " ";

  /**
   * Читает одну строку ввода.
   *
   * @return строка без символа перевода строки
   * @throws InputClosedException если ввод закончился
   */
  String readLine();

  /**
   * Читает пароль: без эха, если консоль это позволяет, иначе обычной строкой.
   *
   * @return введённая строка без символа перевода строки
   * @throws InputClosedException если ввод закончился
   */
  String readPassword();

  /** Выводит текст без перевода строки и сразу сбрасывает буфер вывода. */
  void print(String text);

  /** Выводит текст и переводит строку. */
  void println(String text);

  /** Выводит пустую строку. */
  void println();

  /** Выводит приглашение к вводу: текст и пробел, без перевода строки. */
  default void prompt(String text) {
    print(text + PROMPT_SEPARATOR);
  }
}
