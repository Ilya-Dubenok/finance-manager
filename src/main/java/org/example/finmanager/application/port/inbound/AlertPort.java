package org.example.finmanager.application.port.inbound;

import java.util.List;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.AlertDto;

/**
 * Оповещения по состоянию кошелька. Вызывается только другими сервисами, пользователь к нему
 * напрямую не обращается. Когда что проверяется — таблица в спецификации, раздел 5.
 */
public interface AlertPort {

  /**
   * Проверяет после добавления расхода: A-1 по категории операции, затем A-2.
   *
   * @param wallet кошелёк с уже добавленной операцией
   * @param category название категории расхода
   * @return оповещения; пустой список, если всё в порядке
   */
  List<AlertDto> checkAfterExpense(WalletDto wallet, String category);

  /**
   * Проверяет после добавления дохода: только A-2.
   *
   * @param wallet кошелёк с уже добавленной операцией
   * @return оповещения
   */
  List<AlertDto> checkAfterIncome(WalletDto wallet);

  /**
   * Проверяет бюджет одной категории: только A-1.
   *
   * @param wallet кошелёк
   * @param category название расходной категории
   * @return оповещения
   */
  List<AlertDto> checkBudget(WalletDto wallet, String category);

  /**
   * Проверяет всё: A-1 по всем категориям с бюджетом по алфавиту, затем A-2.
   *
   * @param wallet кошелёк
   * @return оповещения
   */
  List<AlertDto> checkAll(WalletDto wallet);
}
