package org.example.finmanager.application.port.inbound;

import java.math.BigDecimal;
import java.util.List;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.result.CategoryTotalsDto;
import org.example.finmanager.application.dto.result.SummaryDto;

/** Подсчёты по кошельку. */
public interface StatisticsPort {

  /**
   * Возвращает баланс: доходы минус расходы за всё время.
   *
   * @param login логин владельца
   * @return баланс; может быть отрицательным
   */
  BigDecimal balance(String login);

  /**
   * Возвращает сводку: доходы, расходы и разбивку по категориям за период, баланс и бюджеты за
   * всё время.
   *
   * @param login логин владельца
   * @param period период или {@code null}, тогда все операции
   * @return сводка
   * @throws org.example.finmanager.application.exception.ApplicationException {@code
   *     PERIOD_INVALID}, если начало позже конца
   */
  SummaryDto summary(String login, PeriodDto period);

  /**
   * Считает операции по выбранным категориям обоих типов. Названия ищутся без учёта регистра,
   * повторы игнорируются, ненайденные возвращаются в результате.
   *
   * @param login логин владельца
   * @param categoryNames названия категорий
   * @param period период или {@code null}, тогда все операции
   * @return подсчёт по найденным категориям
   * @throws org.example.finmanager.application.exception.ApplicationException {@code EMPTY_VALUE},
   *     если названий нет, {@code PERIOD_INVALID}, если начало периода позже конца
   */
  CategoryTotalsDto totalsByCategories(
      String login, List<String> categoryNames, PeriodDto period);
}
