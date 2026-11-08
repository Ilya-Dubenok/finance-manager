package org.example.finmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.example.finmanager.RefusalAssert;
import org.example.finmanager.application.dto.command.AddOperationCommand;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.BudgetExceededAlert;
import org.example.finmanager.application.dto.result.ExpensesExceedIncomeAlert;
import org.example.finmanager.application.dto.result.OperationResultDto;
import org.example.finmanager.application.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OperationServiceTest {

  private static final String LOGIN = "alice";
  private static final String FOOD = "Food";
  private static final String SALARY = "Salary";
  private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-08T10:00:00Z"), ZoneOffset.UTC);

  private final FakeWalletPort wallets = new FakeWalletPort();
  private final OperationService service = new OperationService(wallets, new AlertService(), CLOCK);

  @BeforeEach
  public void createCategories() {
    givenCategory(OperationType.EXPENSE, FOOD);
    givenCategory(OperationType.INCOME, SALARY);
  }

  @Test
  public void addsExpenseDatedTodayByDefault() {
    OperationResultDto result = add(OperationType.EXPENSE, FOOD, "300", null, null);

    OperationDto expected =
        new OperationDto(
            OperationType.EXPENSE, FOOD, new BigDecimal("300.00"), TODAY, null, null);
    assertThat(result.operation()).isEqualTo(expected);
    assertThat(wallets.get(LOGIN).operations()).containsExactly(expected);
  }

  @Test
  public void addsIncomeWithGivenDate() {
    LocalDate date = LocalDate.of(2026, 10, 1);

    OperationResultDto result = add(OperationType.INCOME, SALARY, "20000.50", date, null);

    assertThat(result.operation().date()).isEqualTo(date);
    assertThat(result.operation().type()).isEqualTo(OperationType.INCOME);
  }

  @Test
  public void normalizesAmountToTwoDecimals() {
    assertThat(add(OperationType.EXPENSE, FOOD, "1500", null, null).operation().amount())
        .isEqualTo(new BigDecimal("1500.00"));
    assertThat(add(OperationType.EXPENSE, FOOD, "10.000", null, null).operation().amount())
        .isEqualTo(new BigDecimal("10.00"));
  }

  @Test
  public void findsCategoryIgnoringCaseAndStoresItsCanonicalName() {
    OperationResultDto result = add(OperationType.EXPENSE, "fOOD", "5", null, null);

    assertThat(result.operation().category()).isEqualTo(FOOD);
  }

  @Test
  public void rejectsUnknownCategoryAndListsAvailableOnes() {
    RefusalAssert.assertRefusalWithArgs(
        () -> add(OperationType.EXPENSE, "Taxi", "5", null, null),
        ErrorCode.CATEGORY_NOT_FOUND,
        "Taxi",
        List.of(FOOD, "Transfers"));
  }

  @Test
  public void looksCategoryUpAmongCategoriesOfTheSameTypeOnly() {
    RefusalAssert.assertRefusal(
        () -> add(OperationType.INCOME, FOOD, "5", null, null), ErrorCode.CATEGORY_NOT_FOUND);
  }

  @Test
  public void rejectsInvalidAmounts() {
    RefusalAssert.assertRefusal(
        () -> add(OperationType.EXPENSE, FOOD, "0", null, null), ErrorCode.AMOUNT_NOT_POSITIVE);
    RefusalAssert.assertRefusal(
        () -> add(OperationType.EXPENSE, FOOD, "-5", null, null), ErrorCode.AMOUNT_NOT_POSITIVE);
    RefusalAssert.assertRefusal(
        () -> add(OperationType.EXPENSE, FOOD, "10.999", null, null),
        ErrorCode.AMOUNT_TOO_MANY_DECIMALS);
    RefusalAssert.assertRefusal(
        () -> add(OperationType.EXPENSE, FOOD, "1000000000", null, null),
        ErrorCode.AMOUNT_TOO_LARGE);
  }

  @Test
  public void acceptsTodayAndRejectsFutureDate() {
    assertThat(add(OperationType.EXPENSE, FOOD, "5", TODAY, null).operation().date())
        .isEqualTo(TODAY);
    RefusalAssert.assertRefusal(
        () -> add(OperationType.EXPENSE, FOOD, "5", TODAY.plusDays(1), null),
        ErrorCode.DATE_IN_FUTURE);
  }

  @Test
  public void trimsCommentAndTreatsBlankCommentAsMissing() {
    assertThat(add(OperationType.EXPENSE, FOOD, "5", null, "  lunch  ").operation().comment())
        .isEqualTo("lunch");
    assertThat(add(OperationType.EXPENSE, FOOD, "5", null, "   ").operation().comment()).isNull();
    assertThat(add(OperationType.EXPENSE, FOOD, "5", null, "").operation().comment()).isNull();
  }

  @Test
  public void rejectsTooLongCommentAndControlCharacters() {
    RefusalAssert.assertRefusalWithArgs(
        () -> add(OperationType.EXPENSE, FOOD, "5", null, "x".repeat(101)),
        ErrorCode.VALUE_TOO_LONG,
        100);
    RefusalAssert.assertRefusal(
        () -> add(OperationType.EXPENSE, FOOD, "5", null, "a\tb"),
        ErrorCode.COMMENT_INVALID_CHARS);
  }

  @Test
  public void rejectedOperationDoesNotChangeTheWallet() {
    RefusalAssert.assertRefusal(
        () -> add(OperationType.EXPENSE, FOOD, "0", null, null), ErrorCode.AMOUNT_NOT_POSITIVE);

    assertThat(wallets.get(LOGIN).operations()).isEmpty();
  }

  @Test
  public void listsOperationsByDateKeepingInsertionOrderForEqualDates() {
    LocalDate early = LocalDate.of(2026, 11, 1);
    add(OperationType.EXPENSE, FOOD, "3", TODAY, "third");
    add(OperationType.EXPENSE, FOOD, "1", early, "first");
    add(OperationType.INCOME, SALARY, "2", early, "second");

    List<OperationDto> operations = service.listOperations(LOGIN, null);

    assertThat(operations)
        .extracting(OperationDto::comment)
        .containsExactly("first", "second", "third");
  }

  @Test
  public void filtersByPeriodInclusively() {
    add(OperationType.EXPENSE, FOOD, "1", LocalDate.of(2026, 11, 1), "a");
    add(OperationType.EXPENSE, FOOD, "1", LocalDate.of(2026, 11, 5), "b");
    add(OperationType.EXPENSE, FOOD, "1", LocalDate.of(2026, 11, 8), "c");

    PeriodDto period = new PeriodDto(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 8));

    assertThat(service.listOperations(LOGIN, period))
        .extracting(OperationDto::comment)
        .containsExactly("b", "c");
  }

  @Test
  public void periodWithOneBoundIsHalfOpen() {
    add(OperationType.EXPENSE, FOOD, "1", LocalDate.of(2026, 11, 1), "a");
    add(OperationType.EXPENSE, FOOD, "1", LocalDate.of(2026, 11, 8), "b");

    List<OperationDto> from = service.listOperations(LOGIN, new PeriodDto(TODAY, null));
    List<OperationDto> to =
        service.listOperations(LOGIN, new PeriodDto(null, LocalDate.of(2026, 11, 1)));

    assertThat(from).extracting(OperationDto::comment).containsExactly("b");
    assertThat(to).extracting(OperationDto::comment).containsExactly("a");
  }

  @Test
  public void returnsEmptyListWhenNothingMatches() {
    PeriodDto period = new PeriodDto(TODAY.minusDays(1), TODAY);

    assertThat(service.listOperations(LOGIN, period)).isEmpty();
  }

  @Test
  public void rejectsPeriodWithStartAfterEnd() {
    PeriodDto reversed = new PeriodDto(TODAY, TODAY.minusDays(1));

    RefusalAssert.assertRefusal(
        () -> service.listOperations(LOGIN, reversed), ErrorCode.PERIOD_INVALID);
  }

  @Test
  public void expenseMoreThanIncomeGivesAlertAboutImbalance() {
    OperationResultDto result = add(OperationType.EXPENSE, FOOD, "300", null, null);

    assertThat(result.alerts())
        .containsExactly(
            new ExpensesExceedIncomeAlert(new BigDecimal("0"), new BigDecimal("300.00")));
  }

  @Test
  public void incomeThatCoversExpensesRemovesTheImbalanceAlert() {
    add(OperationType.EXPENSE, FOOD, "300", null, null);

    OperationResultDto result = add(OperationType.INCOME, SALARY, "300", null, null);

    assertThat(result.alerts()).isEmpty();
  }

  @Test
  public void incomeNeverGivesBudgetAlertsButStillReportsImbalance() {
    add(OperationType.EXPENSE, FOOD, "500", null, null);

    OperationResultDto result = add(OperationType.INCOME, SALARY, "100", null, null);

    assertThat(result.alerts()).hasSize(1);
    assertThat(result.alerts().get(0)).isInstanceOf(ExpensesExceedIncomeAlert.class);
  }

  @Test
  public void expenseOverBudgetGivesBudgetAlertBeforeImbalanceAlert() {
    add(OperationType.INCOME, SALARY, "10", null, null);
    WalletDto wallet = wallets.get(LOGIN);
    wallets.save(
        wallet.toBuilder().budgets(List.of(new BudgetDto(FOOD, new BigDecimal("100.00")))).build());

    OperationResultDto result = add(OperationType.EXPENSE, FOOD, "150", null, null);

    assertThat(result.alerts())
        .containsExactly(
            new BudgetExceededAlert(FOOD, new BigDecimal("100.00"), new BigDecimal("150.00")),
            new ExpensesExceedIncomeAlert(new BigDecimal("10.00"), new BigDecimal("150.00")));
  }

  @Test
  public void expenseExactlyAtTheLimitGivesNoBudgetAlert() {
    add(OperationType.INCOME, SALARY, "1000", null, null);
    WalletDto wallet = wallets.get(LOGIN);
    wallets.save(
        wallet.toBuilder().budgets(List.of(new BudgetDto(FOOD, new BigDecimal("100.00")))).build());

    OperationResultDto result = add(OperationType.EXPENSE, FOOD, "100", null, null);

    assertThat(result.alerts()).isEmpty();
  }

  private OperationResultDto add(
      OperationType type, String category, String amount, LocalDate date, String comment) {
    AddOperationCommand command =
        new AddOperationCommand(LOGIN, type, category, new BigDecimal(amount), date, comment);
    return service.addOperation(command);
  }

  private void givenCategory(OperationType type, String name) {
    WalletDto wallet = wallets.get(LOGIN);
    List<CategoryDto> categories = new ArrayList<>(wallet.categories());
    categories.add(new CategoryDto(name, type));
    wallets.save(wallet.toBuilder().categories(categories).build());
  }
}
