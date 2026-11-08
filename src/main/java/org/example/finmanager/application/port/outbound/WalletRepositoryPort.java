package org.example.finmanager.application.port.outbound;

import java.util.List;
import java.util.Optional;
import org.example.finmanager.application.dto.model.WalletDto;

/** Хранилище кошельков в памяти с пометкой «изменён, ещё не записан на диск». */
public interface WalletRepositoryPort {

  /**
   * Ищет кошелёк по владельцу.
   *
   * @param login логин владельца
   * @return кошелёк или пустой результат, если его ещё нет в памяти
   */
  Optional<WalletDto> findByOwner(String login);

  /**
   * Кладёт кошелёк в память без пометки «изменён».
   *
   * @param wallet кошелёк
   */
  void cache(WalletDto wallet);

  /**
   * Кладёт кошельки в память как изменённые; все или ни одного.
   *
   * @param wallets кошельки
   */
  void saveAll(List<WalletDto> wallets);

  /**
   * Возвращает кошельки с пометкой «изменён».
   *
   * @return изменённые кошельки
   */
  List<WalletDto> findChanged();

  /**
   * Снимает пометку «изменён», если в хранилище лежит именно этот экземпляр: кошелёк мог
   * измениться, пока он записывался.
   *
   * @param saved записанный кошелёк
   */
  void markSaved(WalletDto saved);
}
