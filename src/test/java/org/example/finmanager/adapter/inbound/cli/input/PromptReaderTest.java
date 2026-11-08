package org.example.finmanager.adapter.inbound.cli.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;
import org.example.finmanager.adapter.inbound.cli.dto.PeriodForm;
import org.example.finmanager.adapter.inbound.cli.io.InputClosedException;
import org.example.finmanager.adapter.inbound.cli.io.ScriptedConsoleIo;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.dto.model.OperationType;
import org.junit.jupiter.api.Test;

public class PromptReaderTest {

  private static final String EMPTY_INPUT_MESSAGE = "The value must not be empty.";
  private static final String LOGIN_PROMPT = "Login: ";

  private final MessageCatalog messages = new MessageCatalog();

  @Test
  public void returnsRequiredValueWithoutSurroundingSpaces() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("  alice  ");

    String value = readerFor(io).readRequired(MessageKeys.PROMPT_LOGIN);

    assertThat(value).isEqualTo("alice");
    assertThat(io.output()).isEqualTo(LOGIN_PROMPT);
  }

  @Test
  public void asksAgainWhenValueIsEmpty() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("", "   ", "alice");

    String value = readerFor(io).readRequired(MessageKeys.PROMPT_LOGIN);

    assertThat(value).isEqualTo("alice");
    assertThat(io.output()).contains(EMPTY_INPUT_MESSAGE);
    assertThat(occurrences(io.output(), LOGIN_PROMPT)).isEqualTo(3);
  }

  @Test
  public void readsSecretThroughPasswordInputAndKeepsItAsTyped() {
    ScriptedConsoleIo io = new ScriptedConsoleIo(" pw ");

    String value = readerFor(io).readSecret(MessageKeys.PROMPT_PASSWORD);

    assertThat(value).isEqualTo(" pw ");
    assertThat(io.passwordReads()).isEqualTo(1);
  }

  @Test
  public void asksAgainWhenSecretIsEmpty() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("", "pw");

    String value = readerFor(io).readSecret(MessageKeys.PROMPT_PASSWORD);

    assertThat(value).isEqualTo("pw");
    assertThat(io.output()).contains(EMPTY_INPUT_MESSAGE);
  }

  @Test
  public void cancelWordCancelsRequiredQuestionIgnoringCase() {
    ScriptedConsoleIo io = new ScriptedConsoleIo(" CaNcEl ");
    PromptReader reader = readerFor(io);

    assertThatThrownBy(() -> reader.readRequired(MessageKeys.PROMPT_LOGIN))
        .isInstanceOf(DialogCancelledException.class);
  }

  @Test
  public void cancelWordCancelsSecretQuestion() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("cancel");
    PromptReader reader = readerFor(io);

    assertThatThrownBy(() -> reader.readSecret(MessageKeys.PROMPT_PASSWORD))
        .isInstanceOf(DialogCancelledException.class);
  }

  @Test
  public void propagatesEndOfInput() {
    ScriptedConsoleIo io = new ScriptedConsoleIo();
    PromptReader reader = readerFor(io);

    assertThatThrownBy(() -> reader.readRequired(MessageKeys.PROMPT_LOGIN))
        .isInstanceOf(InputClosedException.class);
  }

  @Test
  public void optionalValueMayBeEmptyAndIsTrimmed() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("", "  lunch  ");
    PromptReader reader = readerFor(io);

    assertThat(reader.readOptional(MessageKeys.PROMPT_COMMENT)).isEmpty();
    assertThat(reader.readOptional(MessageKeys.PROMPT_COMMENT)).isEqualTo("lunch");
  }

  @Test
  public void cancelWordCancelsOptionalQuestion() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("cancel");
    PromptReader reader = readerFor(io);

    assertThatThrownBy(() -> reader.readOptional(MessageKeys.PROMPT_COMMENT))
        .isInstanceOf(DialogCancelledException.class);
  }

  @Test
  public void amountIsParsedFromAnySupportedFormat() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("1500", " 1500.50 ", "1500,50");
    PromptReader reader = readerFor(io);

    assertThat(reader.readAmount(MessageKeys.PROMPT_AMOUNT)).isEqualByComparingTo("1500");
    assertThat(reader.readAmount(MessageKeys.PROMPT_AMOUNT)).isEqualByComparingTo("1500.50");
    assertThat(reader.readAmount(MessageKeys.PROMPT_AMOUNT)).isEqualByComparingTo("1500.50");
  }

  @Test
  public void amountQuestionIsRepeatedWhenFormatIsWrong() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("abc", "1e3", "42");

    BigDecimal amount = readerFor(io).readAmount(MessageKeys.PROMPT_AMOUNT);

    assertThat(amount).isEqualByComparingTo("42");
    assertThat(io.output())
        .contains("Cannot recognize the amount \"abc\". Example: 1500 or 1500.50.")
        .contains("Cannot recognize the amount \"1e3\".");
  }

  @Test
  public void amountQuestionTreatsEmptyAnswerAsMissingValue() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("", "7");

    BigDecimal amount = readerFor(io).readAmount(MessageKeys.PROMPT_AMOUNT);

    assertThat(amount).isEqualByComparingTo("7");
    assertThat(io.output()).contains(EMPTY_INPUT_MESSAGE);
  }

  @Test
  public void optionalDateIsNullWhenSkipped() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("   ");

    assertThat(readerFor(io).readOptionalDate(MessageKeys.PROMPT_DATE)).isNull();
  }

  @Test
  public void optionalDateIsParsed() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("08.11.2026");

    LocalDate date = readerFor(io).readOptionalDate(MessageKeys.PROMPT_DATE);

    assertThat(date).isEqualTo(LocalDate.of(2026, 11, 8));
  }

  @Test
  public void dateQuestionIsRepeatedWhenFormatIsWrong() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("2026-11-08", "31.02.2026", "01.02.2026");

    LocalDate date = readerFor(io).readOptionalDate(MessageKeys.PROMPT_DATE);

    assertThat(date).isEqualTo(LocalDate.of(2026, 2, 1));
    assertThat(io.output())
        .contains("Cannot recognize the date \"2026-11-08\". Format: dd.mm.yyyy")
        .contains("Cannot recognize the date \"31.02.2026\".");
  }

  @Test
  public void typeIsChosenByNumber() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("1", " 2 ");
    PromptReader reader = readerFor(io);

    assertThat(reader.readType(MessageKeys.PROMPT_TYPE)).isEqualTo(OperationType.INCOME);
    assertThat(reader.readType(MessageKeys.PROMPT_TYPE)).isEqualTo(OperationType.EXPENSE);
  }

  @Test
  public void typeQuestionIsRepeatedWhenAnswerIsNotOneOrTwo() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("3", "income", "2");

    OperationType type = readerFor(io).readType(MessageKeys.PROMPT_TYPE);

    assertThat(type).isEqualTo(OperationType.EXPENSE);
    assertThat(occurrences(io.output(), "Choose 1 (income) or 2 (expense).")).isEqualTo(2);
  }

  @Test
  public void confirmationAcceptsYesAndNoIgnoringCase() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("yes", " NO ", "Yes");
    PromptReader reader = readerFor(io);

    assertThat(reader.readConfirmation(MessageKeys.PROMPT_CONFIRM_REMOVE_BUDGET, "Food")).isTrue();
    assertThat(reader.readConfirmation(MessageKeys.PROMPT_CONFIRM_REMOVE_BUDGET, "Food")).isFalse();
    assertThat(reader.readConfirmation(MessageKeys.PROMPT_CONFIRM_REMOVE_BUDGET, "Food")).isTrue();
  }

  @Test
  public void confirmationPromptContainsTheGivenValue() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("yes");

    readerFor(io).readConfirmation(MessageKeys.PROMPT_CONFIRM_REMOVE_BUDGET, "Food");

    assertThat(io.output()).isEqualTo("Delete the budget of category \"Food\"? (yes/no): ");
  }

  @Test
  public void confirmationIsAskedAgainForAnyOtherAnswer() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("maybe", "y", "yes");

    boolean answer = readerFor(io).readConfirmation(MessageKeys.PROMPT_CONFIRM_REMOVE_BUDGET, "x");

    assertThat(answer).isTrue();
    assertThat(occurrences(io.output(), "Enter \"yes\" or \"no\".")).isEqualTo(2);
  }

  @Test
  public void confirmationTreatsEmptyAnswerAsMissingValue() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("", "no");

    boolean answer = readerFor(io).readConfirmation(MessageKeys.PROMPT_CONFIRM_REMOVE_BUDGET, "x");

    assertThat(answer).isFalse();
    assertThat(io.output()).contains(EMPTY_INPUT_MESSAGE);
  }

  @Test
  public void cancelWordCancelsConfirmation() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("cancel");
    PromptReader reader = readerFor(io);

    assertThatThrownBy(
            () -> reader.readConfirmation(MessageKeys.PROMPT_CONFIRM_REMOVE_BUDGET, "x"))
        .isInstanceOf(DialogCancelledException.class);
  }

  @Test
  public void listIsSplitByCommasAndTrimmed() {
    ScriptedConsoleIo io = new ScriptedConsoleIo(" Food , Cinema ,, Salary ");

    List<String> names = readerFor(io).readList(MessageKeys.PROMPT_CATEGORIES);

    assertThat(names).containsExactly("Food", "Cinema", "Salary");
  }

  @Test
  public void listQuestionIsAskedAgainWhenNothingRemainsAfterSplitting() {
    ScriptedConsoleIo io = new ScriptedConsoleIo(",,", " , ", "Food");

    List<String> names = readerFor(io).readList(MessageKeys.PROMPT_CATEGORIES);

    assertThat(names).containsExactly("Food");
    assertThat(occurrences(io.output(), EMPTY_INPUT_MESSAGE)).isEqualTo(2);
  }

  @Test
  public void periodAsksBothBoundsAndAllowsSkippingThem() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("01.11.2026", "");

    PeriodForm period = readerFor(io).readPeriod();

    assertThat(period).isEqualTo(new PeriodForm(LocalDate.of(2026, 11, 1), null));
    assertThat(io.output())
        .isEqualTo(
            "Period start (dd.mm.yyyy, Enter - skip): Period end (dd.mm.yyyy, Enter - skip): ");
  }

  @Test
  public void readsRequiredPathAndStripsQuotes() {
    ScriptedConsoleIo io = new ScriptedConsoleIo(" \"C:\\my data\\file.json\" ");

    Path path = readerFor(io).readPath(MessageKeys.PROMPT_IMPORT_PATH);

    assertThat(path).isEqualTo(Path.of("C:\\my data\\file.json"));
  }

  @Test
  public void asksAgainWhenPathIsEmptyOrInvalid() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("", "bad\u0000name", "file.json");

    Path path = readerFor(io).readPath(MessageKeys.PROMPT_IMPORT_PATH);

    assertThat(path).isEqualTo(Path.of("file.json"));
    assertThat(io.output()).contains(EMPTY_INPUT_MESSAGE).contains("Invalid file path");
  }

  @Test
  public void cancelWordCancelsPathQuestion() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("cancel");
    PromptReader reader = readerFor(io);

    assertThatThrownBy(() -> reader.readPath(MessageKeys.PROMPT_IMPORT_PATH))
        .isInstanceOf(DialogCancelledException.class);
  }

  @Test
  public void optionalPathIsNullOnEnter() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("");

    Path path = readerFor(io).readOptionalPath(MessageKeys.PROMPT_EXPORT_PATH);

    assertThat(path).isNull();
  }

  @Test
  public void optionalPathIsReadWhenGiven() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("out.json");

    Path path = readerFor(io).readOptionalPath(MessageKeys.PROMPT_EXPORT_PATH);

    assertThat(path).isEqualTo(Path.of("out.json"));
  }

  @Test
  public void optionalPathAsksAgainWhenInvalid() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("bad\u0000name", "");

    Path path = readerFor(io).readOptionalPath(MessageKeys.PROMPT_EXPORT_PATH);

    assertThat(path).isNull();
    assertThat(io.output()).contains("Invalid file path");
  }

  private PromptReader readerFor(ScriptedConsoleIo io) {
    return new PromptReader(io, messages);
  }

  private int occurrences(String text, String part) {
    return text.split(Pattern.quote(part), -1).length - 1;
  }
}
