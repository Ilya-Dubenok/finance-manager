package org.example.finmanager.application.port.inbound;

import java.util.List;
import org.example.finmanager.application.dto.model.WalletDto;

/** Доступ других сервисов к кошелькам; единственная точка обращения к хранилищу кошельков. */
public interface WalletPort {

  /**
   * Возвращает кошелёк пользователя: из памяти, иначе из файла, иначе новый с системными
   * категориями.
   *
   * @param login логин владельца
   * @return кошелёк
   * @throws org.example.finmanager.application.exception.ApplicationException {@code
   *     WALLET_CORRUPTED}, если файл кошелька не читается или не проходит проверку; файл при этом
   *     не меняется
   */
  WalletDto get(String login);

  /**
   * Сохраняет новую версию кошелька в памяти и помечает её изменённой.
   *
   * @param wallet кошелёк целиком
   */
  void save(WalletDto wallet);

  /**
   * Сохраняет новые версии нескольких кошельков одним действием: все или ни одного. Нужен
   * переводу, который меняет кошельки двух пользователей.
   *
   * @param wallets кошельки целиком
   */
  void saveAll(List<WalletDto> wallets);

  /**
   * Записывает на диск все изменённые кошельки. Не записанные из-за ошибки остаются изменёнными,
   * поэтому вызов можно повторить.
   *
   * @throws org.example.finmanager.application.exception.ApplicationException {@code SAVE_FAILED},
   *     если запись не удалась
   */
  void saveChanges();
}
