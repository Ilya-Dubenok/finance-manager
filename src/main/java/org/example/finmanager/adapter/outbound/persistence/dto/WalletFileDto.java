package org.example.finmanager.adapter.outbound.persistence.dto;

import java.util.List;

/**
 * Содержимое файла кошелька (спецификация, раздел 7). Значения — строки в формате файла: суммы
 * десятичные, даты ISO.
 *
 * @param version версия формата
 * @param owner логин владельца; информационное поле
 * @param categories категории
 * @param budgets бюджеты
 * @param operations операции
 */
public record WalletFileDto(
    int version,
    String owner,
    List<CategoryFileDto> categories,
    List<BudgetFileDto> budgets,
    List<OperationFileDto> operations) {

  /** Версия формата, которую пишет и читает приложение. */
  public static final int CURRENT_VERSION = 1;

  /** Отсутствующие в файле списки считаются пустыми. */
  public WalletFileDto {
    categories = categories == null ? List.of() : List.copyOf(categories);
    budgets = budgets == null ? List.of() : List.copyOf(budgets);
    operations = operations == null ? List.of() : List.copyOf(operations);
  }
}
