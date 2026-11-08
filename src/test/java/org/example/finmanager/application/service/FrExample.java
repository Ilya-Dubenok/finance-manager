package org.example.finmanager.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;

/**
 * Кошелёк из примера в требованиях (спецификация, S-1): расходы Food 800, Entertainment 3000,
 * Utilities 3000, Taxi 1500, доходы Salary 60000, Bonus 3000, бюджеты Food 4000, Entertainment
 * 3000, Utilities 2500. Все операции за {@link #DATE}.
 */
@UtilityClass
public class FrExample {

  /** Владелец кошелька. */
  public static final String OWNER = "alice";

  /** Дата всех операций. */
  public static final LocalDate DATE = LocalDate.of(2026, 11, 8);

  /**
   * Собирает кошелёк.
   *
   * @return кошелёк с категориями, операциями и бюджетами из примера
   */
  public WalletDto wallet() {
    List<CategoryDto> categories = new ArrayList<>(WalletRules.newWallet(OWNER).categories());
    for (String name : List.of("Food", "Entertainment", "Utilities", "Taxi")) {
      categories.add(new CategoryDto(name, OperationType.EXPENSE));
    }
    for (String name : List.of("Salary", "Bonus")) {
      categories.add(new CategoryDto(name, OperationType.INCOME));
    }
    List<OperationDto> operations =
        List.of(
            operation(OperationType.EXPENSE, "Food", "300"),
            operation(OperationType.EXPENSE, "Food", "500"),
            operation(OperationType.EXPENSE, "Entertainment", "3000"),
            operation(OperationType.EXPENSE, "Utilities", "3000"),
            operation(OperationType.EXPENSE, "Taxi", "1500"),
            operation(OperationType.INCOME, "Salary", "20000"),
            operation(OperationType.INCOME, "Salary", "40000"),
            operation(OperationType.INCOME, "Bonus", "3000"));
    List<BudgetDto> budgets =
        List.of(
            new BudgetDto("Food", new BigDecimal("4000.00")),
            new BudgetDto("Entertainment", new BigDecimal("3000.00")),
            new BudgetDto("Utilities", new BigDecimal("2500.00")));
    return new WalletDto(OWNER, categories, budgets, operations);
  }

  /**
   * Собирает операцию без комментария и контрагента, датированную {@link #DATE}.
   *
   * @param type тип
   * @param category название категории
   * @param amount сумма
   * @return операция
   */
  public OperationDto operation(OperationType type, String category, String amount) {
    return new OperationDto(type, category, new BigDecimal(amount), DATE, null, null);
  }
}
