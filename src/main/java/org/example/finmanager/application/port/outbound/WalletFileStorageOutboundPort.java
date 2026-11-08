package org.example.finmanager.application.port.outbound;

import java.nio.file.Path;
import java.util.Optional;
import org.example.finmanager.application.dto.model.WalletDto;

/** Файловое хранилище кошельков: один файл на пользователя. */
public interface WalletFileStorageOutboundPort {

  /**
   * Возвращает путь к файлу кошелька пользователя; файл может не существовать.
   *
   * @param login логин владельца
   * @return путь к файлу
   */
  Path fileOf(String login);

  /**
   * Загружает кошелёк пользователя.
   *
   * @param login логин владельца
   * @return кошелёк или пустой результат, если файла нет
   * @throws org.example.finmanager.application.exception.StorageFormatException если содержимое
   *     файла не соответствует формату
   * @throws org.example.finmanager.application.exception.StorageIoException если файл не удалось
   *     прочитать
   */
  Optional<WalletDto> load(String login);

  /**
   * Записывает кошелёк в файл целиком: либо новый файл, либо прежний, без промежуточных состояний.
   *
   * @param wallet кошелёк
   * @throws org.example.finmanager.application.exception.StorageIoException если запись не удалась
   */
  void save(WalletDto wallet);
}
