package org.example.finmanager.adapter.inbound.cli.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.example.finmanager.adapter.inbound.cli.dto.AddOperationForm;
import org.example.finmanager.adapter.inbound.cli.dto.CreateCategoryForm;
import org.example.finmanager.adapter.inbound.cli.dto.OperationRow;
import org.example.finmanager.adapter.inbound.cli.dto.PeriodForm;
import org.example.finmanager.adapter.inbound.cli.dto.RemoveBudgetForm;
import org.example.finmanager.adapter.inbound.cli.dto.SetBudgetForm;
import org.example.finmanager.application.dto.command.AddOperationCommand;
import org.example.finmanager.application.dto.command.CreateCategoryCommand;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.command.RemoveBudgetCommand;
import org.example.finmanager.application.dto.command.SetBudgetCommand;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.junit.jupiter.api.Test;

public class MappersTest {

  private static final String LOGIN = "alice";
  private static final LocalDate DATE = LocalDate.of(2026, 11, 8);

  private final CommandMapper commandMapper = new CommandMapperImpl();
  private final OperationRowMapper rowMapper = new OperationRowMapperImpl();

  @Test
  public void addOperationFormBecomesCommandWithTheLoginOfTheSession() {
    AddOperationForm form =
        new AddOperationForm(OperationType.EXPENSE, "Food", new BigDecimal("300"), null, "lunch");

    AddOperationCommand command = commandMapper.toCommand(LOGIN, form);

    assertThat(command)
        .isEqualTo(
            new AddOperationCommand(
                LOGIN, OperationType.EXPENSE, "Food", new BigDecimal("300"), null, "lunch"));
  }

  @Test
  public void createCategoryFormBecomesCommand() {
    CreateCategoryCommand command =
        commandMapper.toCommand(LOGIN, new CreateCategoryForm(OperationType.INCOME, "Salary"));

    assertThat(command)
        .isEqualTo(new CreateCategoryCommand(LOGIN, OperationType.INCOME, "Salary"));
  }

  @Test
  public void budgetFormsBecomeCommands() {
    SetBudgetCommand set =
        commandMapper.toCommand(LOGIN, new SetBudgetForm("Food", new BigDecimal("4000")));
    RemoveBudgetCommand remove = commandMapper.toCommand(LOGIN, new RemoveBudgetForm("Food"));

    assertThat(set).isEqualTo(new SetBudgetCommand(LOGIN, "Food", new BigDecimal("4000")));
    assertThat(remove).isEqualTo(new RemoveBudgetCommand(LOGIN, "Food"));
  }

  @Test
  public void periodFormBecomesPeriod() {
    assertThat(commandMapper.toPeriod(new PeriodForm(DATE, null)))
        .isEqualTo(new PeriodDto(DATE, null));
  }

  @Test
  public void operationBecomesRowOfTextsReadyForOutput() {
    OperationDto operation =
        new OperationDto(
            OperationType.EXPENSE,
            "Transfers",
            new BigDecimal("1234.50"),
            DATE,
            "gift",
            "bob");

    OperationRow row = rowMapper.toRow(operation);

    assertThat(row)
        .isEqualTo(
            new OperationRow("08.11.2026", "expense", "Transfers", "1,234.50", "bob", "gift"));
  }

  @Test
  public void rowShortensLongCommentAndShowsMissingOnesAsEmpty() {
    OperationDto longComment =
        new OperationDto(
            OperationType.INCOME, "Salary", BigDecimal.TEN, DATE, "c".repeat(40), null);
    OperationDto noComment =
        new OperationDto(OperationType.INCOME, "Salary", BigDecimal.TEN, DATE, null, null);

    assertThat(rowMapper.toRow(longComment).comment()).isEqualTo("c".repeat(27) + "...");
    assertThat(rowMapper.toRow(noComment).comment()).isEmpty();
    assertThat(rowMapper.toRow(noComment).counterparty()).isNull();
  }
}
