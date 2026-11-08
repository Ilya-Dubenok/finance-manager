package org.example.finmanager.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finmanager.application.dto.command.AddOperationCommand;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.AlertDto;
import org.example.finmanager.application.dto.result.OperationResultDto;
import org.example.finmanager.application.port.inbound.AlertPort;
import org.example.finmanager.application.port.inbound.OperationPort;
import org.example.finmanager.application.port.inbound.WalletPort;
import org.springframework.stereotype.Service;

/** Доходы и расходы. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationService implements OperationPort {

  private static final int AMOUNT_SCALE = 2;

  private final WalletPort wallets;
  private final AlertPort alerts;
  private final Clock clock;

  @Override
  public OperationResultDto addOperation(AddOperationCommand command) {
    WalletDto wallet = wallets.get(command.login());
    CategoryDto category = findCategory(wallet, command);
    InputRules.requireAmount(command.amount());
    LocalDate today = LocalDate.now(clock);
    LocalDate date = command.date() == null ? today : command.date();
    InputRules.requireNotInFuture(date, today);
    String comment = normalizeComment(command.comment());
    InputRules.requireComment(comment);

    BigDecimal amount = command.amount().setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
    OperationDto operation =
        new OperationDto(command.type(), category.name(), amount, date, comment, null);
    List<OperationDto> operations = new ArrayList<>(wallet.operations());
    operations.add(operation);
    WalletDto updated = wallet.toBuilder().operations(operations).build();
    wallets.save(updated);
    log.info(
        "Added {} of {} to '{}' for '{}'",
        command.type(),
        amount,
        category.name(),
        command.login());
    return new OperationResultDto(operation, alertsAfter(updated, operation));
  }

  @Override
  public List<OperationDto> listOperations(String login, PeriodDto period) {
    PeriodDto filter = period == null ? new PeriodDto(null, null) : period;
    InputRules.requirePeriod(filter);
    return WalletRules.operationsInPeriod(wallets.get(login), filter);
  }

  private CategoryDto findCategory(WalletDto wallet, AddOperationCommand command) {
    return WalletRules.findCategory(wallet, command.type(), command.category())
        .orElseThrow(
            () -> WalletRules.categoryNotFound(wallet, command.type(), command.category()));
  }

  private List<AlertDto> alertsAfter(WalletDto wallet, OperationDto operation) {
    if (operation.type() == OperationType.EXPENSE) {
      return alerts.checkAfterExpense(wallet, operation.category());
    }
    return alerts.checkAfterIncome(wallet);
  }

  private String normalizeComment(String comment) {
    if (comment == null || comment.isBlank()) {
      return null;
    }
    return comment.trim();
  }
}
