package org.example.finmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.example.finmanager.RefusalAssert;
import org.example.finmanager.application.dto.command.ExportCommand;
import org.example.finmanager.application.dto.command.ImportCommand;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.BudgetExceededAlert;
import org.example.finmanager.application.dto.result.ImportResultDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.example.finmanager.application.exception.StorageFormatException;
import org.example.finmanager.application.exception.StorageIoException;
import org.junit.jupiter.api.Test;

public class ReportServiceTest {

  private static final String ALICE = "alice";
  private static final String BOB = "bob";
  private static final Path FILE = Path.of("out", "snapshot.json");
  private static final Path CSV_FILE = Path.of("out", "operations.csv");

  private final FakeWalletPort wallets = new FakeWalletPort();
  private final FakeReportFiles files = new FakeReportFiles();
  private final ReportService service =
      new ReportService(wallets, new AlertService(), files, files);

  @Test
  public void exportWritesTheCurrentWalletToTheRequestedFile() {
    WalletDto wallet = FrExample.wallet();
    wallets.save(wallet);

    Path written = service.exportSnapshot(new ExportCommand(ALICE, FILE, false));

    assertThat(written).isEqualTo(FILE);
    assertThat(files.snapshot(FILE)).isEqualTo(wallet);
  }

  @Test
  public void exportRefusesToReplaceAnExistingFileUnlessAllowed() {
    wallets.save(FrExample.wallet());
    files.putSnapshot(FILE, WalletRules.newWallet(ALICE));

    RefusalAssert.assertRefusalWithArgs(
        () -> service.exportSnapshot(new ExportCommand(ALICE, FILE, false)),
        ErrorCode.FILE_ALREADY_EXISTS,
        FILE);
    assertThat(files.snapshot(FILE).operations()).isEmpty();

    service.exportSnapshot(new ExportCommand(ALICE, FILE, true));

    assertThat(files.snapshot(FILE).operations()).hasSize(8);
  }

  @Test
  public void exportReportsWriteFailureWithTheFileAndTheReason() {
    files.failWith(new StorageIoException(FILE, "disk is full"));

    RefusalAssert.assertRefusalWithArgs(
        () -> service.exportSnapshot(new ExportCommand(ALICE, FILE, false)),
        ErrorCode.EXPORT_FAILED,
        FILE,
        "disk is full");
  }

  @Test
  public void csvExportPassesAllOperationsOfTheWallet() {
    wallets.save(FrExample.wallet());

    Path written = service.exportOperationsCsv(new ExportCommand(ALICE, CSV_FILE, false));

    assertThat(written).isEqualTo(CSV_FILE);
    assertThat(files.csvOperations()).isEqualTo(FrExample.wallet().operations());
  }

  @Test
  public void csvExportOfAnEmptyWalletIsAllowed() {
    Path written = service.exportOperationsCsv(new ExportCommand(ALICE, CSV_FILE, false));

    assertThat(written).isEqualTo(CSV_FILE);
    assertThat(files.csvOperations()).isEmpty();
  }

  @Test
  public void csvExportRefusesToReplaceAnExistingFileUnlessAllowed() {
    service.exportOperationsCsv(new ExportCommand(ALICE, CSV_FILE, false));

    RefusalAssert.assertRefusalWithArgs(
        () -> service.exportOperationsCsv(new ExportCommand(ALICE, CSV_FILE, false)),
        ErrorCode.FILE_ALREADY_EXISTS,
        CSV_FILE);
    assertThat(service.exportOperationsCsv(new ExportCommand(ALICE, CSV_FILE, true)))
        .isEqualTo(CSV_FILE);
  }

  @Test
  public void importReplacesTheWalletAndReportsWhatWasLoaded() {
    files.putSnapshot(FILE, FrExample.wallet());

    ImportResultDto result = service.importSnapshot(new ImportCommand(ALICE, FILE));

    assertThat(result.categoryCount()).isEqualTo(FrExample.wallet().categories().size());
    assertThat(result.budgetCount()).isEqualTo(3);
    assertThat(result.operationCount()).isEqualTo(8);
    assertThat(result.ownerMismatch()).isFalse();
    assertThat(wallets.get(ALICE).operations()).hasSize(8);
  }

  @Test
  public void importTakesTheOwnerFromTheLoggedInUserAndNotesTheMismatch() {
    files.putSnapshot(FILE, FrExample.wallet().toBuilder().owner(BOB).build());

    ImportResultDto result = service.importSnapshot(new ImportCommand(ALICE, FILE));

    assertThat(result.ownerMismatch()).isTrue();
    assertThat(result.originalOwner()).isEqualTo(BOB);
    assertThat(wallets.get(ALICE).owner()).isEqualTo(ALICE);
  }

  @Test
  public void snapshotWithoutOwnerHasNoMismatch() {
    files.putSnapshot(FILE, FrExample.wallet().toBuilder().owner(null).build());

    ImportResultDto result = service.importSnapshot(new ImportCommand(ALICE, FILE));

    assertThat(result.ownerMismatch()).isFalse();
    assertThat(result.originalOwner()).isNull();
  }

  @Test
  public void importAddsSystemCategoriesMissingFromTheSnapshot() {
    WalletDto bare =
        new WalletDto(
            ALICE,
            List.of(new CategoryDto("Food", OperationType.EXPENSE)),
            List.of(),
            List.of());
    files.putSnapshot(FILE, bare);

    service.importSnapshot(new ImportCommand(ALICE, FILE));

    assertThat(wallets.get(ALICE).categories())
        .extracting(CategoryDto::name)
        .contains("Food", "Transfers");
  }

  @Test
  public void importReturnsAlertsOfTheImportedWallet() {
    files.putSnapshot(FILE, overspentWallet());

    ImportResultDto result = service.importSnapshot(new ImportCommand(ALICE, FILE));

    assertThat(result.alerts()).hasAtLeastOneElementOfType(BudgetExceededAlert.class);
  }

  @Test
  public void invalidSnapshotIsRejectedAndTheWalletStaysUntouched() {
    WalletDto before = FrExample.wallet();
    wallets.save(before);
    OperationDto unknownCategory = FrExample.operation(OperationType.EXPENSE, "Nowhere", "5");
    files.putSnapshot(
        FILE, before.toBuilder().operations(List.of(unknownCategory)).build());

    assertThatThrownBy(() -> service.importSnapshot(new ImportCommand(ALICE, FILE)))
        .isInstanceOfSatisfying(
            ApplicationException.class,
            e -> {
              assertThat(e.getCode()).isEqualTo(ErrorCode.IMPORT_INVALID);
              assertThat(e.getArgs().get(0)).isEqualTo(1);
              assertThat(e.getArgs().get(1).toString()).contains("Nowhere");
            });
    assertThat(wallets.get(ALICE)).isEqualTo(before);
  }

  @Test
  public void atMostTenProblemsAreListed() {
    List<OperationDto> broken =
        IntStream.range(0, 15)
            .mapToObj(index -> FrExample.operation(OperationType.EXPENSE, "Nowhere", "5"))
            .toList();
    files.putSnapshot(FILE, FrExample.wallet().toBuilder().operations(broken).build());

    assertThatThrownBy(() -> service.importSnapshot(new ImportCommand(ALICE, FILE)))
        .isInstanceOfSatisfying(
            ApplicationException.class, e -> assertThat(e.getArgs().get(0)).isEqualTo(10));
  }

  @Test
  public void unreadableFileIsReportedWithTheFileAndTheReason() {
    files.failWith(new StorageIoException(FILE, "no such file"));

    RefusalAssert.assertRefusalWithArgs(
        () -> service.importSnapshot(new ImportCommand(ALICE, FILE)),
        ErrorCode.IMPORT_READ_FAILED,
        FILE,
        "no such file");
  }

  @Test
  public void malformedFileIsReportedAsAnInvalidSnapshot() {
    files.failWith(new StorageFormatException(FILE, "not json"));

    assertThatThrownBy(() -> service.importSnapshot(new ImportCommand(ALICE, FILE)))
        .isInstanceOfSatisfying(
            ApplicationException.class,
            e -> {
              assertThat(e.getCode()).isEqualTo(ErrorCode.IMPORT_INVALID);
              assertThat(e.getArgs().get(1).toString()).contains("not json");
            });
  }

  private WalletDto overspentWallet() {
    WalletDto wallet = FrExample.wallet();
    OperationDto extra = FrExample.operation(OperationType.EXPENSE, "Food", "9000");
    List<OperationDto> operations = new ArrayList<>(wallet.operations());
    operations.add(extra);
    return wallet.toBuilder().operations(operations).build();
  }
}
