package org.example.finmanager.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finmanager.application.dto.command.TransferCommand;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.AlertDto;
import org.example.finmanager.application.dto.result.TransferResultDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.example.finmanager.application.port.inbound.AlertPort;
import org.example.finmanager.application.port.inbound.TransferPort;
import org.example.finmanager.application.port.inbound.UserAccessPort;
import org.example.finmanager.application.port.inbound.WalletPort;
import org.springframework.stereotype.Service;

/**
 * Переводы. Оба кошелька вычисляются до записи и кладутся в память одним действием, поэтому
 * перевод атомарен без откатов: любой отказ случается раньше, чем что-то меняется.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService implements TransferPort {

  private static final int AMOUNT_SCALE = 2;

  private final WalletPort wallets;
  private final UserAccessPort users;
  private final AlertPort alerts;
  private final Clock clock;

  @Override
  public TransferResultDto transfer(TransferCommand command) {
    String recipient = extractRecipient(command);
    BigDecimal amount = extractTransferAmount(command);
    String comment = extractComment(command);

    WalletDto senderWallet = wallets.get(command.fromLogin());
    WalletDto recipientWallet = wallets.get(recipient);
    BigDecimal balance = WalletRules.balance(senderWallet);
    if (amount.compareTo(balance) > 0) {
      throw new ApplicationException(ErrorCode.INSUFFICIENT_FUNDS, balance, amount);
    }

    LocalDate today = LocalDate.now(clock);
    OperationDto expense =
        transferOperation(OperationType.EXPENSE, amount, today, comment, recipient);
    OperationDto income =
        transferOperation(OperationType.INCOME, amount, today, comment, command.fromLogin());
    WalletDto updatedSender = withOperation(senderWallet, expense);
    WalletDto updatedRecipient = withOperation(recipientWallet, income);
    wallets.saveAll(List.of(updatedSender, updatedRecipient));

    log.info("Transferred {} from '{}' to '{}'", amount, command.fromLogin(), recipient);
    List<AlertDto> senderAlerts =
        alerts.checkAfterExpense(updatedSender, WalletRules.SYSTEM_CATEGORY_NAME);
    return new TransferResultDto(expense, recipient, senderAlerts);
  }

  private String extractComment(TransferCommand command) {
    String comment = normalizeComment(command.comment());
    InputRules.requireComment(comment);
    return comment;
  }

  private BigDecimal extractTransferAmount(TransferCommand command) {
    InputRules.requireAmount(command.amount());
    return command.amount().setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
  }

  private String extractRecipient(TransferCommand command) {
    InputRules.requireNotBlank(command.toLogin());
    String toLogin = command.toLogin().trim();
    validateRecipient(command, toLogin);
    return toLogin;
  }

  private void validateRecipient(TransferCommand command, String toLogin) {
    if (!users.userExists(toLogin)) {
      throw new ApplicationException(ErrorCode.USER_NOT_FOUND, toLogin);
    }
    if (toLogin.equals(command.fromLogin())) {
      throw new ApplicationException(ErrorCode.SELF_TRANSFER);
    }
  }

  private OperationDto transferOperation(
      OperationType type, BigDecimal amount, LocalDate date, String comment, String counterparty) {
    return new OperationDto(
        type, WalletRules.SYSTEM_CATEGORY_NAME, amount, date, comment, counterparty);
  }

  private WalletDto withOperation(WalletDto wallet, OperationDto operation) {
    List<OperationDto> operations = new ArrayList<>(wallet.operations());
    operations.add(operation);
    return wallet.toBuilder().operations(operations).build();
  }

  private String normalizeComment(String comment) {
    if (comment == null || comment.isBlank()) {
      return null;
    }
    return comment.trim();
  }
}
