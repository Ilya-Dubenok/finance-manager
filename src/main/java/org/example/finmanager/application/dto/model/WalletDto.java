package org.example.finmanager.application.dto.model;

import java.util.List;
import lombok.Builder;

/**
 * Кошелёк пользователя. Неизменяемый: любое изменение даёт новый экземпляр.
 *
 * @param owner логин владельца
 * @param categories категории, всегда включают системные
 * @param budgets бюджеты, не больше одного на расходную категорию
 * @param operations операции в порядке добавления
 */
@Builder(toBuilder = true)
public record WalletDto(
    String owner,
    List<CategoryDto> categories,
    List<BudgetDto> budgets,
    List<OperationDto> operations) {

  /** Копирует списки, чтобы кошелёк нельзя было изменить снаружи. */
  public WalletDto {
    categories = List.copyOf(categories);
    budgets = List.copyOf(budgets);
    operations = List.copyOf(operations);
  }
}
