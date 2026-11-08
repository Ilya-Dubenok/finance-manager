package org.example.finmanager.application.port.outbound;

import java.nio.file.Path;
import java.util.List;
import org.example.finmanager.application.dto.model.OperationDto;

/** Экспорт операций в CSV. */
public interface OperationsCsvOutboundPort {

  /**
   * Записывает операции в CSV; недостающие каталоги создаются, запись атомарная.
   *
   * @param login логин владельца, нужен для имени файла по умолчанию
   * @param operations операции в порядке вывода
   * @param target путь к файлу или {@code null}, тогда путь по умолчанию в каталоге экспорта
   * @param overwrite можно ли заменить существующий файл
   * @return путь к записанному файлу
   * @throws org.example.finmanager.application.exception.StorageFileExistsException если файл
   *     есть, а заменять его нельзя
   * @throws org.example.finmanager.application.exception.StorageIoException если запись не удалась
   */
  Path write(String login, List<OperationDto> operations, Path target, boolean overwrite);
}
