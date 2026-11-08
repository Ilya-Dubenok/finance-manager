package org.example.finmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.finmanager.RefusalAssert;
import org.example.finmanager.application.dto.command.TransferCommand;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.UserDto;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.BudgetExceededAlert;
import org.example.finmanager.application.dto.result.TransferResultDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.example.finmanager.application.port.outbound.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TransferServiceTest {

  private static final String ALICE = "alice";
  private static final String BOB = "bob";
  private static final String TRANSFERS = "Transfers";
  private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-08T10:00:00Z"), ZoneOffset.UTC);

  private final Map<String, UserDto> users =
      Map.of(ALICE, new UserDto(ALICE, "pw-a"), BOB, new UserDto(BOB, "pw-b"));
  private final UserRepositoryPort repository = login -> Optional.ofNullable(users.get(login));
  private final FakeWalletPort wallets = new FakeWalletPort();
  private final UserAccessService access = new UserAccessService(repository, wallets);
  private final TransferService service =
      new TransferService(wallets, access, new AlertService(), CLOCK);

  @BeforeEach
  public void giveAliceThousand() {
    WalletDto alice = wallets.get(ALICE);
    wallets.save(
        alice.toBuilder()
            .operations(List.of(FrExample.operation(OperationType.INCOME, TRANSFERS, "1000")))
            .build());
  }

  @Test
  public void addsMirroringOperationsToBothWalletsWithCounterparties() {
    service.transfer(command(BOB, "400", "gift"));

    OperationDto expense = lastOperation(ALICE);
    assertThat(expense.type()).isEqualTo(OperationType.EXPENSE);
    assertThat(expense.category()).isEqualTo(TRANSFERS);
    assertThat(expense.amount()).isEqualByComparingTo("400");
    assertThat(expense.date()).isEqualTo(TODAY);
    assertThat(expense.comment()).isEqualTo("gift");
    assertThat(expense.counterparty()).isEqualTo(BOB);
    OperationDto income = lastOperation(BOB);
    assertThat(income.type()).isEqualTo(OperationType.INCOME);
    assertThat(income.category()).isEqualTo(TRANSFERS);
    assertThat(income.amount()).isEqualByComparingTo("400");
    assertThat(income.date()).isEqualTo(TODAY);
    assertThat(income.comment()).isEqualTo("gift");
    assertThat(income.counterparty()).isEqualTo(ALICE);
  }

  @Test
  public void balancesChangeByTheTransferredAmount() {
    service.transfer(command(BOB, "400", null));

    assertThat(WalletRules.balance(wallets.get(ALICE))).isEqualByComparingTo("600");
    assertThat(WalletRules.balance(wallets.get(BOB))).isEqualByComparingTo("400");
  }

  @Test
  public void returnsTheSenderExpenseAndTheRecipientLogin() {
    TransferResultDto result = service.transfer(command(" bob ", "10.5", null));

    assertThat(result.toLogin()).isEqualTo(BOB);
    assertThat(result.fromOperation().amount()).isEqualTo(new BigDecimal("10.50"));
    assertThat(result.alerts()).isEmpty();
  }

  @Test
  public void bothWalletsAreSavedInOneAction() {
    service.transfer(command(BOB, "1", null));

    assertThat(wallets.saveAllCalls()).isEqualTo(1);
  }

  @Test
  public void blankCommentBecomesMissingAndCommentIsTrimmed() {
    service.transfer(command(BOB, "1", "   "));
    service.transfer(command(BOB, "1", "  hi  "));

    List<OperationDto> operations = wallets.get(BOB).operations();
    assertThat(operations.get(0).comment()).isNull();
    assertThat(operations.get(1).comment()).isEqualTo("hi");
  }

  @Test
  public void unknownRecipientIsRejected() {
    RefusalAssert.assertRefusalWithArgs(
        () -> service.transfer(command("carol", "1", null)), ErrorCode.USER_NOT_FOUND, "carol");
    RefusalAssert.assertRefusal(
        () -> service.transfer(command("BOB", "1", null)), ErrorCode.USER_NOT_FOUND);
  }

  @Test
  public void blankRecipientIsRejected() {
    RefusalAssert.assertRefusal(
        () -> service.transfer(command("  ", "1", null)), ErrorCode.EMPTY_VALUE);
  }

  @Test
  public void transferToYourselfIsRejected() {
    RefusalAssert.assertRefusal(
        () -> service.transfer(command(ALICE, "1", null)), ErrorCode.SELF_TRANSFER);
  }

  @Test
  public void invalidAmountsAreRejected() {
    RefusalAssert.assertRefusal(
        () -> service.transfer(command(BOB, "0", null)), ErrorCode.AMOUNT_NOT_POSITIVE);
    RefusalAssert.assertRefusal(
        () -> service.transfer(command(BOB, "-5", null)), ErrorCode.AMOUNT_NOT_POSITIVE);
    RefusalAssert.assertRefusal(
        () -> service.transfer(command(BOB, "1.234", null)), ErrorCode.AMOUNT_TOO_MANY_DECIMALS);
    RefusalAssert.assertRefusal(
        () -> service.transfer(command(BOB, "1000000000", null)), ErrorCode.AMOUNT_TOO_LARGE);
  }

  @Test
  public void invalidCommentIsRejected() {
    RefusalAssert.assertRefusalWithArgs(
        () -> service.transfer(command(BOB, "1", "x".repeat(101))), ErrorCode.VALUE_TOO_LONG, 100);
    RefusalAssert.assertRefusal(
        () -> service.transfer(command(BOB, "1", "a\tb")), ErrorCode.COMMENT_INVALID_CHARS);
  }

  @Test
  public void amountGreaterThanBalanceIsRejectedWithBalanceAndAmount() {
    assertThatThrownBy(() -> service.transfer(command(BOB, "1000.01", null)))
        .isInstanceOfSatisfying(
            ApplicationException.class,
            e -> {
              assertThat(e.getCode()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
              assertThat((BigDecimal) e.getArgs().get(0)).isEqualByComparingTo("1000");
              assertThat((BigDecimal) e.getArgs().get(1)).isEqualByComparingTo("1000.01");
            });
  }

  @Test
  public void wholeBalanceCanBeTransferred() {
    service.transfer(command(BOB, "1000", null));

    assertThat(WalletRules.balance(wallets.get(ALICE))).isEqualByComparingTo("0");
  }

  @Test
  public void senderWithoutMoneyCannotTransferAnything() {
    wallets.get(BOB);

    RefusalAssert.assertRefusal(
        () ->
            service.transfer(new TransferCommand(BOB, ALICE, new BigDecimal("1"), null)),
        ErrorCode.INSUFFICIENT_FUNDS);
  }

  @Test
  public void rejectedTransferChangesNothing() {
    RefusalAssert.assertRefusal(
        () -> service.transfer(command(BOB, "5000", null)), ErrorCode.INSUFFICIENT_FUNDS);

    assertThat(wallets.get(ALICE).operations()).hasSize(1);
    assertThat(wallets.get(BOB).operations()).isEmpty();
    assertThat(wallets.saveAllCalls()).isZero();
  }

  @Test
  public void damagedWalletOfTheRecipientAbortsTheWholeTransfer() {
    wallets.failGetFor(BOB, new ApplicationException(ErrorCode.WALLET_CORRUPTED, BOB, "bad", "f"));

    RefusalAssert.assertRefusal(
        () -> service.transfer(command(BOB, "100", null)), ErrorCode.WALLET_CORRUPTED);

    assertThat(wallets.get(ALICE).operations()).hasSize(1);
    assertThat(wallets.saveAllCalls()).isZero();
  }

  @Test
  public void recipientWalletIsLoadedOnDemand() {
    service.transfer(command(BOB, "1", null));

    assertThat(wallets.requestedLogins()).contains(BOB);
  }

  @Test
  public void exceededBudgetOfTheTransfersCategoryGivesAlertToTheSender() {
    WalletDto alice = wallets.get(ALICE);
    wallets.save(
        alice.toBuilder()
            .budgets(List.of(new BudgetDto(TRANSFERS, new BigDecimal("100.00"))))
            .build());

    TransferResultDto result = service.transfer(command(BOB, "150", null));

    assertThat(result.alerts())
        .containsExactly(
            new BudgetExceededAlert(TRANSFERS, new BigDecimal("100.00"), new BigDecimal("150.00")));
  }

  @Test
  public void recipientGetsNoAlertsAndSenderCoveredByBalanceGetsNone() {
    TransferResultDto result = service.transfer(command(BOB, "999", null));

    assertThat(result.alerts()).isEmpty();
  }

  private TransferCommand command(String toLogin, String amount, String comment) {
    return new TransferCommand(ALICE, toLogin, new BigDecimal(amount), comment);
  }

  private OperationDto lastOperation(String login) {
    List<OperationDto> operations = wallets.get(login).operations();
    return operations.get(operations.size() - 1);
  }
}
