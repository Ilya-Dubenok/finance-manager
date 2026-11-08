package org.example.finmanager.adapter.inbound.cli.input;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.dto.PeriodForm;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.dto.model.OperationType;
import org.springframework.stereotype.Component;

/**
 * Задаёт вопросы диалога: повторяет вопрос при пустом ответе и неверном формате, прерывает диалог
 * по слову отмены. Бизнес-правила (сумма больше нуля, дата не в будущем и так далее) проверяют
 * сервисы, а не этот класс.
 */
@Component
@RequiredArgsConstructor
public class PromptReader {

  private static final String CHOICE_INCOME = "1";
  private static final String CHOICE_EXPENSE = "2";
  private static final String ANSWER_YES = "yes";
  private static final String ANSWER_NO = "no";
  private static final String LIST_SEPARATOR = ",";

  private final ConsoleIo io;
  private final MessageCatalog messages;

  /**
   * Спрашивает обязательное значение.
   *
   * @param promptKey ключ текста вопроса
   * @return ответ без пробелов по краям
   * @throws DialogCancelledException если введено слово отмены
   */
  public String readRequired(String promptKey) {
    return askUntilFilled(messages.get(promptKey), io::readLine).trim();
  }

  /**
   * Спрашивает необязательное значение.
   *
   * @param promptKey ключ текста вопроса
   * @return ответ без пробелов по краям; пустая строка, если пользователь нажал Enter
   * @throws DialogCancelledException если введено слово отмены
   */
  public String readOptional(String promptKey) {
    io.prompt(messages.get(promptKey));
    String input = io.readLine().trim();
    rejectCancelWord(input);
    return input;
  }

  /**
   * Спрашивает обязательный секрет (пароль); ответ возвращается как введён, без обрезки пробелов.
   *
   * @param promptKey ключ текста вопроса
   * @return ответ как введён
   * @throws DialogCancelledException если введено слово отмены
   */
  public String readSecret(String promptKey) {
    return askUntilFilled(messages.get(promptKey), io::readPassword);
  }

  /**
   * Спрашивает сумму; при неверном формате сообщает об ошибке и спрашивает снова.
   *
   * @param promptKey ключ текста вопроса
   * @return сумма; допустимость значения проверяет сервис
   * @throws DialogCancelledException если введено слово отмены
   */
  public BigDecimal readAmount(String promptKey) {
    while (true) {
      String text = readRequired(promptKey);
      Optional<BigDecimal> amount = AmountParser.parse(text);
      if (amount.isPresent()) {
        return amount.get();
      }
      io.println(messages.get(MessageKeys.ERROR_UNKNOWN_AMOUNT, text));
    }
  }

  /**
   * Спрашивает необязательную дату; при неверном формате сообщает об ошибке и спрашивает снова.
   *
   * @param promptKey ключ текста вопроса
   * @return дата или {@code null}, если пользователь нажал Enter
   * @throws DialogCancelledException если введено слово отмены
   */
  public LocalDate readOptionalDate(String promptKey) {
    while (true) {
      String text = readOptional(promptKey);
      if (text.isEmpty()) {
        return null;
      }
      Optional<LocalDate> date = DateParser.parse(text);
      if (date.isPresent()) {
        return date.get();
      }
      io.println(messages.get(MessageKeys.ERROR_UNKNOWN_DATE, text));
    }
  }

  /**
   * Спрашивает обязательный путь к файлу; при недопустимом пути сообщает об ошибке и спрашивает
   * снова.
   *
   * @param promptKey ключ текста вопроса
   * @return путь; существование файла не проверяется
   * @throws DialogCancelledException если введено слово отмены
   */
  public Path readPath(String promptKey) {
    while (true) {
      String text = readRequired(promptKey);
      Optional<Path> path = PathParser.parse(text);
      if (path.isPresent()) {
        return path.get();
      }
      io.println(messages.get(MessageKeys.ERROR_INVALID_PATH, text));
    }
  }

  /**
   * Спрашивает необязательный путь к файлу; при недопустимом пути сообщает об ошибке и
   * спрашивает снова.
   *
   * @param promptKey ключ текста вопроса
   * @return путь или {@code null}, если пользователь нажал Enter
   * @throws DialogCancelledException если введено слово отмены
   */
  public Path readOptionalPath(String promptKey) {
    while (true) {
      String text = readOptional(promptKey);
      if (text.isEmpty()) {
        return null;
      }
      Optional<Path> path = PathParser.parse(text);
      if (path.isPresent()) {
        return path.get();
      }
      io.println(messages.get(MessageKeys.ERROR_INVALID_PATH, text));
    }
  }

  /**
   * Спрашивает необязательный период: начало и конец.
   *
   * @return период; границы {@code null}, если пользователь пропустил вопросы
   * @throws DialogCancelledException если введено слово отмены
   */
  public PeriodForm readPeriod() {
    LocalDate from = readOptionalDate(MessageKeys.PROMPT_PERIOD_FROM);
    LocalDate to = readOptionalDate(MessageKeys.PROMPT_PERIOD_TO);
    return new PeriodForm(from, to);
  }

  /**
   * Спрашивает тип: 1 — доход, 2 — расход; при другом ответе сообщает об ошибке и спрашивает снова.
   *
   * @param promptKey ключ текста вопроса
   * @return выбранный тип
   * @throws DialogCancelledException если введено слово отмены
   */
  public OperationType readType(String promptKey) {
    while (true) {
      String choice = readRequired(promptKey);
      if (CHOICE_INCOME.equals(choice)) {
        return OperationType.INCOME;
      }
      if (CHOICE_EXPENSE.equals(choice)) {
        return OperationType.EXPENSE;
      }
      io.println(messages.get(MessageKeys.ERROR_UNKNOWN_TYPE));
    }
  }

  /**
   * Задаёт вопрос «да или нет»; при другом ответе сообщает об ошибке и спрашивает снова.
   *
   * @param promptKey ключ текста вопроса
   * @param args значения для подстановки в текст вопроса
   * @return {@code true} для «yes», {@code false} для «no»
   * @throws DialogCancelledException если введено слово отмены
   */
  public boolean readConfirmation(String promptKey, Object... args) {
    String prompt = messages.get(promptKey, args);
    while (true) {
      String answer = askUntilFilled(prompt, io::readLine).trim();
      if (ANSWER_YES.equalsIgnoreCase(answer)) {
        return true;
      }
      if (ANSWER_NO.equalsIgnoreCase(answer)) {
        return false;
      }
      io.println(messages.get(MessageKeys.ERROR_UNKNOWN_CONFIRMATION));
    }
  }

  /**
   * Спрашивает список значений через запятую; пустые элементы отбрасываются.
   *
   * @param promptKey ключ текста вопроса
   * @return непустой список значений без пробелов по краям
   * @throws DialogCancelledException если введено слово отмены
   */
  public List<String> readList(String promptKey) {
    while (true) {
      String text = readRequired(promptKey);
      List<String> items =
          Arrays.stream(text.split(LIST_SEPARATOR))
              .map(String::trim)
              .filter(item -> !item.isEmpty())
              .toList();
      if (!items.isEmpty()) {
        return items;
      }
      io.println(messages.get(MessageKeys.ERROR_EMPTY_INPUT));
    }
  }

  private String askUntilFilled(String prompt, Supplier<String> reader) {
    while (true) {
      io.prompt(prompt);
      String input = reader.get();
      String candidate = input.trim();
      rejectCancelWord(candidate);
      if (!candidate.isEmpty()) {
        return input;
      }
      io.println(messages.get(MessageKeys.ERROR_EMPTY_INPUT));
    }
  }

  private void rejectCancelWord(String candidate) {
    String cancelWord = messages.get(MessageKeys.DIALOG_CANCEL_WORD);
    if (candidate.equalsIgnoreCase(cancelWord)) {
      throw new DialogCancelledException();
    }
  }
}
