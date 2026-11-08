package org.example.finmanager.application.dto.result;

import java.math.BigDecimal;
import java.util.List;
import org.example.finmanager.application.dto.command.PeriodDto;

/**
 * Подсчёт по выбранным категориям.
 *
 * @param period период подсчёта
 * @param anyCategoryFound найдена ли хотя бы одна из названных категорий
 * @param rows найденные категории с ненулевой суммой в периоде, по алфавиту
 * @param incomeTotal сумма по найденным категориям доходов
 * @param expenseTotal сумма по найденным категориям расходов
 * @param notFound названные категории, которых нет в кошельке, без повторов
 */
public record CategoryTotalsDto(
    PeriodDto period,
    boolean anyCategoryFound,
    List<CategoryTotalRowDto> rows,
    BigDecimal incomeTotal,
    BigDecimal expenseTotal,
    List<String> notFound) {

  /** Копирует списки, чтобы результат нельзя было изменить снаружи. */
  public CategoryTotalsDto {
    rows = List.copyOf(rows);
    notFound = List.copyOf(notFound);
  }
}
