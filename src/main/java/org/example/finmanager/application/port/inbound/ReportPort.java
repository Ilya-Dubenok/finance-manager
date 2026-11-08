package org.example.finmanager.application.port.inbound;

import java.nio.file.Path;
import org.example.finmanager.application.dto.command.ExportCommand;
import org.example.finmanager.application.dto.command.ImportCommand;
import org.example.finmanager.application.dto.result.ImportResultDto;

/** Обмен данными с файлами: экспорт снимка и операций, импорт снимка. */
public interface ReportPort {

  /**
   * Экспортирует кошелёк в JSON-снимок.
   *
   * @param command владелец, путь и разрешение заменить файл
   * @return путь к записанному файлу
   * @throws org.example.finmanager.application.exception.ApplicationException {@code
   *     FILE_ALREADY_EXISTS}, если файл есть, а заменять его нельзя, {@code EXPORT_FAILED}, если
   *     запись не удалась
   */
  Path exportSnapshot(ExportCommand command);

  /**
   * Экспортирует операции в CSV.
   *
   * @param command владелец, путь и разрешение заменить файл
   * @return путь к записанному файлу
   * @throws org.example.finmanager.application.exception.ApplicationException {@code
   *     FILE_ALREADY_EXISTS} или {@code EXPORT_FAILED}
   */
  Path exportOperationsCsv(ExportCommand command);

  /**
   * Импортирует снимок: кошелёк пользователя целиком заменяется содержимым файла. Импорт идёт
   * целиком или никак.
   *
   * @param command владелец и путь к файлу
   * @return сколько записей импортировано, владелец снимка и оповещения
   * @throws org.example.finmanager.application.exception.ApplicationException {@code
   *     IMPORT_READ_FAILED}, если файл не читается, {@code IMPORT_INVALID}, если содержимое не
   *     проходит проверку
   */
  ImportResultDto importSnapshot(ImportCommand command);
}
