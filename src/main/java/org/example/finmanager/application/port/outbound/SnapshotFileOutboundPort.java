package org.example.finmanager.application.port.outbound;

import java.nio.file.Path;
import org.example.finmanager.application.dto.model.WalletDto;

/** Файлы снимков кошелька по произвольному пути (формат тот же, что у файла кошелька). */
public interface SnapshotFileOutboundPort {

  /**
   * Читает снимок. Владелец в результате — тот, что указан в файле, он может быть {@code null}.
   *
   * @param source путь к файлу
   * @return содержимое снимка без проверки бизнес-правил
   * @throws org.example.finmanager.application.exception.StorageIoException если файл не
   *     читается
   * @throws org.example.finmanager.application.exception.StorageFormatException если содержимое
   *     не соответствует формату
   */
  WalletDto read(Path source);

  /**
   * Записывает снимок; недостающие каталоги создаются, запись атомарная.
   *
   * @param wallet кошелёк
   * @param target путь к файлу или {@code null}, тогда путь по умолчанию в каталоге экспорта
   * @param overwrite можно ли заменить существующий файл
   * @return путь к записанному файлу
   * @throws org.example.finmanager.application.exception.StorageFileExistsException если файл
   *     есть, а заменять его нельзя
   * @throws org.example.finmanager.application.exception.StorageIoException если запись не удалась
   */
  Path write(WalletDto wallet, Path target, boolean overwrite);
}
