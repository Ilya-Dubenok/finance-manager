package org.example.finmanager.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.exception.ApplicationException;

/**
 * Проверяет кошелёк, загруженный из файла: те же правила, что при вводе (спецификация, раздел 7).
 * Возвращает все найденные проблемы; тексты — для диагностики, на экран их выводит сообщение B-115.
 */
@UtilityClass
public class WalletValidator {

  private static final String REQUIRED_FIELDS = "%s #%d: required fields are missing";
  private static final String INVALID_VALUE = "%s #%d (%s): %s";
  private static final String DUPLICATE = "%s #%d (%s): duplicate";
  private static final String RESERVED_NAME = "%s #%d (%s): reserved name";
  private static final String UNKNOWN_CATEGORY = "%s #%d (%s): category does not exist";
  private static final String CATEGORY = "category";
  private static final String BUDGET = "budget";
  private static final String OPERATION = "operation";

  /**
   * Проверяет кошелёк.
   *
   * @param wallet кошелёк с уже добавленными системными категориями
   * @return найденные проблемы по порядку; пустой список, если кошелёк корректен
   */
  public List<String> validate(WalletDto wallet) {
    List<String> problems = new ArrayList<>();
    validateCategories(wallet, problems);
    validateBudgets(wallet, problems);
    validateOperations(wallet, problems);
    return problems;
  }

  private void validateCategories(WalletDto wallet, List<String> problems) {
    Set<String> seen = new HashSet<>();
    int number = 0;
    for (CategoryDto category : wallet.categories()) {
      number++;
      if (category == null || category.name() == null || category.type() == null) {
        problems.add(REQUIRED_FIELDS.formatted(CATEGORY, number));
        continue;
      }
      String name = category.name();
      addIfInvalid(problems, CATEGORY, number, name, () -> InputRules.requireCategoryName(name));
      if (WalletRules.CANCEL_WORD.equalsIgnoreCase(name)) {
        problems.add(RESERVED_NAME.formatted(CATEGORY, number, name));
      }
      if (!seen.add(category.type() + name.toLowerCase())) {
        problems.add(DUPLICATE.formatted(CATEGORY, number, name));
      }
    }
  }

  private void validateBudgets(WalletDto wallet, List<String> problems) {
    Set<String> seen = new HashSet<>();
    int number = 0;
    for (BudgetDto budget : wallet.budgets()) {
      number++;
      if (budget == null || budget.category() == null || budget.limit() == null) {
        problems.add(REQUIRED_FIELDS.formatted(BUDGET, number));
        continue;
      }
      String category = budget.category();
      if (WalletRules.findCategory(wallet, OperationType.EXPENSE, category).isEmpty()) {
        problems.add(UNKNOWN_CATEGORY.formatted(BUDGET, number, category));
      }
      addIfInvalid(
          problems, BUDGET, number, category, () -> InputRules.requireAmount(budget.limit()));
      if (!seen.add(category.toLowerCase())) {
        problems.add(DUPLICATE.formatted(BUDGET, number, category));
      }
    }
  }

  private void validateOperations(WalletDto wallet, List<String> problems) {
    int number = 0;
    for (OperationDto operation : wallet.operations()) {
      number++;
      if (!isComplete(operation)) {
        problems.add(REQUIRED_FIELDS.formatted(OPERATION, number));
        continue;
      }
      String category = operation.category();
      if (WalletRules.findCategory(wallet, operation.type(), category).isEmpty()) {
        problems.add(UNKNOWN_CATEGORY.formatted(OPERATION, number, category));
      }
      addIfInvalid(
          problems,
          OPERATION,
          number,
          category,
          () -> InputRules.requireAmount(operation.amount()));
      addIfInvalid(
          problems,
          OPERATION,
          number,
          category,
          () -> InputRules.requireComment(operation.comment()));
    }
  }

  private boolean isComplete(OperationDto operation) {
    return operation != null
        && operation.type() != null
        && operation.category() != null
        && operation.amount() != null
        && operation.date() != null;
  }

  private void addIfInvalid(
      List<String> problems, String kind, int number, String label, Runnable check) {
    try {
      check.run();
    } catch (ApplicationException e) {
      problems.add(INVALID_VALUE.formatted(kind, number, label, e.getCode().name()));
    }
  }
}
