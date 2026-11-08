package org.example.finmanager.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.BudgetStatusDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;

/** Правила устройства кошелька, общие для сервисов. */
@UtilityClass
public class WalletRules {

  /** Название системной категории, в которую пишутся переводы. */
  public static final String SYSTEM_CATEGORY_NAME = "Transfers";

  /** Слово, которым пользователь отменяет диалог; не может быть названием категории. */
  public static final String CANCEL_WORD = "cancel";

  /**
   * Создаёт пустой кошелёк с системными категориями обоих типов.
   *
   * @param owner логин владельца
   * @return новый кошелёк
   */
  public WalletDto newWallet(String owner) {
    CategoryDto incomeTransfers = new CategoryDto(SYSTEM_CATEGORY_NAME, OperationType.INCOME);
    CategoryDto expenseTransfers = new CategoryDto(SYSTEM_CATEGORY_NAME, OperationType.EXPENSE);
    return new WalletDto(owner, List.of(incomeTransfers, expenseTransfers), List.of(), List.of());
  }

  /**
   * Добавляет системные категории обоих типов, если их нет (без учёта регистра названия).
   *
   * @param wallet кошелёк, загруженный из файла
   * @return кошелёк с системными категориями
   */
  public WalletDto withSystemCategories(WalletDto wallet) {
    List<CategoryDto> categories = new ArrayList<>(wallet.categories());
    for (OperationType type : OperationType.values()) {
      if (findCategory(wallet, type, SYSTEM_CATEGORY_NAME).isEmpty()) {
        categories.add(new CategoryDto(SYSTEM_CATEGORY_NAME, type));
      }
    }
    return wallet.toBuilder().categories(categories).build();
  }

  /**
   * Ищет категорию заданного типа без учёта регистра названия.
   *
   * @param wallet кошелёк
   * @param type тип категории
   * @param name название
   * @return категория или пустой результат
   */
  public Optional<CategoryDto> findCategory(WalletDto wallet, OperationType type, String name) {
    return wallet.categories().stream()
        .filter(category -> category.type() == type)
        .filter(category -> category.name().equalsIgnoreCase(name))
        .findFirst();
  }

  /**
   * Возвращает названия категорий типа по алфавиту без учёта регистра.
   *
   * @param wallet кошелёк
   * @param type тип категорий
   * @return названия
   */
  public List<String> categoryNames(WalletDto wallet, OperationType type) {
    return wallet.categories().stream()
        .filter(category -> category.type() == type)
        .map(CategoryDto::name)
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .toList();
  }

  /**
   * Собирает отказ «категория не найдена» со списком доступных категорий типа.
   *
   * @param wallet кошелёк
   * @param type тип искомой категории
   * @param name введённое название
   * @return исключение для выброса
   */
  public ApplicationException categoryNotFound(WalletDto wallet, OperationType type, String name) {
    List<String> available = categoryNames(wallet, type);
    return new ApplicationException(ErrorCode.CATEGORY_NOT_FOUND, name, available);
  }

  /**
   * Проверяет, что название совпадает с названием системной категории.
   *
   * @param name название
   * @return {@code true} для системной категории
   */
  public boolean isSystemName(String name) {
    return SYSTEM_CATEGORY_NAME.equalsIgnoreCase(name);
  }

  /**
   * Ищет бюджет категории без учёта регистра названия.
   *
   * @param wallet кошелёк
   * @param categoryName название расходной категории
   * @return бюджет или пустой результат
   */
  public Optional<BudgetDto> findBudget(WalletDto wallet, String categoryName) {
    return wallet.budgets().stream()
        .filter(budget -> budget.category().equalsIgnoreCase(categoryName))
        .findFirst();
  }

  /**
   * Считает баланс: доходы минус расходы за всё время.
   *
   * @param wallet кошелёк
   * @return баланс
   */
  public BigDecimal balance(WalletDto wallet) {
    BigDecimal income = sum(wallet.operations(), OperationType.INCOME);
    BigDecimal expense = sum(wallet.operations(), OperationType.EXPENSE);
    return income.subtract(expense);
  }

  /**
   * Считает расходы категории за всё время.
   *
   * @param wallet кошелёк
   * @param categoryName название расходной категории
   * @return сумма расходов
   */
  public BigDecimal spent(WalletDto wallet, String categoryName) {
    return wallet.operations().stream()
        .filter(operation -> operation.type() == OperationType.EXPENSE)
        .filter(operation -> operation.category().equalsIgnoreCase(categoryName))
        .map(OperationDto::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Считает состояние бюджета: потрачено и остаток за всё время.
   *
   * @param wallet кошелёк
   * @param budget бюджет
   * @return состояние бюджета
   */
  public BudgetStatusDto budgetStatus(WalletDto wallet, BudgetDto budget) {
    BigDecimal spent = spent(wallet, budget.category());
    BigDecimal remaining = budget.limit().subtract(spent);
    return new BudgetStatusDto(budget.category(), budget.limit(), spent, remaining);
  }

  /**
   * Возвращает операции периода по возрастанию даты, при равных датах в порядке добавления.
   *
   * @param wallet кошелёк
   * @param period период; границы включительно, любая может быть {@code null}
   * @return операции периода
   */
  public List<OperationDto> operationsInPeriod(WalletDto wallet, PeriodDto period) {
    return wallet.operations().stream()
        .filter(operation -> isInPeriod(operation, period))
        .sorted(Comparator.comparing(OperationDto::date))
        .toList();
  }

  /**
   * Суммирует операции одного типа.
   *
   * @param operations операции
   * @param type тип
   * @return сумма; ноль, если таких операций нет
   */
  public BigDecimal sum(List<OperationDto> operations, OperationType type) {
    return operations.stream()
        .filter(operation -> operation.type() == type)
        .map(OperationDto::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private boolean isInPeriod(OperationDto operation, PeriodDto period) {
    boolean afterStart = period.from() == null || !operation.date().isBefore(period.from());
    boolean beforeEnd = period.to() == null || !operation.date().isAfter(period.to());
    return afterStart && beforeEnd;
  }
}
