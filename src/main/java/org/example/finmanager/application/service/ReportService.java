package org.example.finmanager.application.service;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finmanager.application.dto.command.ExportCommand;
import org.example.finmanager.application.dto.command.ImportCommand;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.AlertDto;
import org.example.finmanager.application.dto.result.ImportResultDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.example.finmanager.application.exception.StorageFileExistsException;
import org.example.finmanager.application.exception.StorageFormatException;
import org.example.finmanager.application.exception.StorageIoException;
import org.example.finmanager.application.port.inbound.AlertPort;
import org.example.finmanager.application.port.inbound.ReportPort;
import org.example.finmanager.application.port.inbound.WalletPort;
import org.example.finmanager.application.port.outbound.OperationsCsvOutboundPort;
import org.example.finmanager.application.port.outbound.SnapshotFileOutboundPort;
import org.springframework.stereotype.Service;

/** Экспорт снимка и операций, импорт снимка. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService implements ReportPort {

  private static final int MAX_REPORTED_PROBLEMS = 10;
  private static final String PROBLEM_PREFIX = "- ";

  private final WalletPort wallets;
  private final AlertPort alerts;
  private final SnapshotFileOutboundPort snapshots;
  private final OperationsCsvOutboundPort csv;

  @Override
  public Path exportSnapshot(ExportCommand command) {
    WalletDto wallet = wallets.get(command.login());
    Supplier<Path> write =
        () -> snapshots.write(wallet, command.target(), command.overwrite());
    Path written = writing(write);
    log.info("Exported the snapshot of '{}' to {}", command.login(), written);
    return written;
  }

  @Override
  public Path exportOperationsCsv(ExportCommand command) {
    WalletDto wallet = wallets.get(command.login());
    List<OperationDto> operations = wallet.operations();
    Supplier<Path> write =
        () -> csv.write(command.login(), operations, command.target(), command.overwrite());
    Path written = writing(write);
    log.info("Exported the operations of '{}' to {}", command.login(), written);
    return written;
  }

  @Override
  public ImportResultDto importSnapshot(ImportCommand command) {
    WalletDto snapshot = read(command.source());
    WalletDto candidate =
        WalletRules.withSystemCategories(snapshot.toBuilder().owner(command.login()).build());
    List<String> problems = WalletValidator.validate(candidate);
    if (!problems.isEmpty()) {
      throw invalid(problems);
    }

    wallets.save(candidate);
    log.info("Imported the snapshot {} into the wallet of '{}'", command.source(), command.login());
    List<AlertDto> importAlerts = alerts.checkAll(candidate);
    String originalOwner = snapshot.owner();
    boolean mismatch = originalOwner != null && !originalOwner.equals(command.login());
    return new ImportResultDto(
        snapshot.categories().size(),
        snapshot.budgets().size(),
        snapshot.operations().size(),
        originalOwner,
        mismatch,
        importAlerts);
  }

  private Path writing(Supplier<Path> write) {
    try {
      return write.get();
    } catch (StorageFileExistsException e) {
      throw new ApplicationException(ErrorCode.FILE_ALREADY_EXISTS, e.getFile());
    } catch (StorageIoException e) {
      throw new ApplicationException(ErrorCode.EXPORT_FAILED, e.getFile(), e.getMessage());
    }
  }

  private WalletDto read(Path source) {
    try {
      return snapshots.read(source);
    } catch (StorageIoException e) {
      throw new ApplicationException(ErrorCode.IMPORT_READ_FAILED, e.getFile(), e.getMessage());
    } catch (StorageFormatException e) {
      throw invalid(List.of(e.getMessage()));
    }
  }

  private ApplicationException invalid(List<String> problems) {
    int shown = Math.min(problems.size(), MAX_REPORTED_PROBLEMS);
    StringBuilder lines = new StringBuilder();
    for (String problem : problems.subList(0, shown)) {
      lines.append(System.lineSeparator()).append(PROBLEM_PREFIX).append(problem);
    }
    return new ApplicationException(ErrorCode.IMPORT_INVALID, shown, lines.toString());
  }
}
