package org.example.finmanager.application.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.exception.StorageFileExistsException;
import org.example.finmanager.application.port.outbound.OperationsCsvOutboundPort;
import org.example.finmanager.application.port.outbound.SnapshotFileOutboundPort;

/**
 * Тестовые файлы экспорта и импорта: «файлы» в памяти. Снимок читается и пишется как есть, CSV
 * запоминает переданные операции. Ошибку любого вызова задаёт тест.
 */
public class FakeReportFiles implements SnapshotFileOutboundPort, OperationsCsvOutboundPort {

  private final Map<Path, WalletDto> snapshots = new HashMap<>();
  private final List<Path> csvFiles = new ArrayList<>();
  private final List<OperationDto> csvOperations = new ArrayList<>();
  private RuntimeException failure;

  @Override
  public WalletDto read(Path source) {
    failIfAsked();
    return snapshots.get(source);
  }

  @Override
  public Path write(WalletDto wallet, Path target, boolean overwrite) {
    failIfAsked();
    if (!overwrite && snapshots.containsKey(target)) {
      throw new StorageFileExistsException(target);
    }
    snapshots.put(target, wallet);
    return target;
  }

  @Override
  public Path write(String login, List<OperationDto> operations, Path target, boolean overwrite) {
    failIfAsked();
    if (!overwrite && csvFiles.contains(target)) {
      throw new StorageFileExistsException(target);
    }
    csvFiles.add(target);
    csvOperations.clear();
    csvOperations.addAll(operations);
    return target;
  }

  /** Кладёт «файл» снимка. */
  public void putSnapshot(Path file, WalletDto wallet) {
    snapshots.put(file, wallet);
  }

  /** Возвращает «файл» снимка или {@code null}, если такого нет. */
  public WalletDto snapshot(Path file) {
    return snapshots.get(file);
  }

  /** Возвращает операции, записанные в CSV последним вызовом. */
  public List<OperationDto> csvOperations() {
    return List.copyOf(csvOperations);
  }

  /** Заставляет любой следующий вызов падать с заданным исключением. */
  public void failWith(RuntimeException exception) {
    this.failure = exception;
  }

  private void failIfAsked() {
    if (failure != null) {
      throw failure;
    }
  }
}
