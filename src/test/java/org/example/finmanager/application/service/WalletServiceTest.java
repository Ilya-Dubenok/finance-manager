package org.example.finmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import org.example.finmanager.RefusalAssert;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.example.finmanager.application.exception.StorageFormatException;
import org.example.finmanager.application.exception.StorageIoException;
import org.junit.jupiter.api.Test;

public class WalletServiceTest {

  private static final String LOGIN = "alice";
  private static final Path FILE = Path.of("data", "wallets", "alice.json");

  private final FakeWalletRepository repository = new FakeWalletRepository();
  private final FakeWalletFileStorage storage = new FakeWalletFileStorage();
  private final WalletService service = new WalletService(repository, storage);

  @Test
  public void createsNewWalletWithSystemCategoriesWhenNoneExists() {
    WalletDto wallet = service.get(LOGIN);

    assertThat(wallet).isEqualTo(WalletRules.newWallet(LOGIN));
  }

  @Test
  public void cachesNewWalletWithoutMarkingItChanged() {
    WalletDto wallet = service.get(LOGIN);

    assertThat(repository.cached()).containsExactly(wallet);
    assertThat(repository.saved()).isEmpty();
  }

  @Test
  public void returnsTheSameWalletOnRepeatedRequests() {
    WalletDto first = service.get(LOGIN);

    WalletDto second = service.get(LOGIN);

    assertThat(second).isSameAs(first);
    assertThat(repository.cached()).hasSize(1);
    assertThat(storage.loads()).isEqualTo(1);
  }

  @Test
  public void returnsExistingWalletFromMemoryWithoutTouchingTheFile() {
    WalletDto existing = WalletRules.newWallet(LOGIN);
    repository.cache(existing);

    assertThat(service.get(LOGIN)).isSameAs(existing);
    assertThat(storage.loads()).isZero();
  }

  @Test
  public void loadsWalletFromFileAndCachesItWithoutMarkingItChanged() {
    WalletDto fromFile = walletWithFood();
    storage.putFile(fromFile);

    WalletDto wallet = service.get(LOGIN);

    assertThat(wallet.categories()).contains(new CategoryDto("Food", OperationType.EXPENSE));
    assertThat(repository.cached()).containsExactly(wallet);
    assertThat(repository.findChanged()).isEmpty();
  }

  @Test
  public void addsMissingSystemCategoriesToTheLoadedWallet() {
    storage.putFile(new WalletDto(LOGIN, List.of(), List.of(), List.of()));

    WalletDto wallet = service.get(LOGIN);

    assertThat(wallet.categories())
        .containsExactlyInAnyOrder(
            new CategoryDto("Transfers", OperationType.INCOME),
            new CategoryDto("Transfers", OperationType.EXPENSE));
  }

  @Test
  public void formatErrorBlocksLoadingAndReportsFileAndReason() {
    storage.failLoadWith(new StorageFormatException(FILE, "bad json"));

    RefusalAssert.assertRefusalWithArgs(
        () -> service.get(LOGIN), ErrorCode.WALLET_CORRUPTED, LOGIN, "bad json", FILE);
    assertThat(repository.findByOwner(LOGIN)).isEmpty();
  }

  @Test
  public void unreadableFileBlocksLoadingToo() {
    storage.failLoadWith(new StorageIoException(FILE, "access denied"));

    RefusalAssert.assertRefusalWithArgs(
        () -> service.get(LOGIN), ErrorCode.WALLET_CORRUPTED, LOGIN, "access denied", FILE);
  }

  @Test
  public void contentThatBreaksTheRulesBlocksLoadingAndIsNeverCached() {
    OperationDto unknownCategory =
        FrExample.operation(OperationType.EXPENSE, "Nowhere", "5");
    storage.putFile(
        new WalletDto(LOGIN, List.of(), List.of(), List.of(unknownCategory)));

    assertThatThrownBy(() -> service.get(LOGIN))
        .isInstanceOfSatisfying(
            ApplicationException.class,
            e -> {
              assertThat(e.getCode()).isEqualTo(ErrorCode.WALLET_CORRUPTED);
              assertThat(e.getArgs().get(1).toString())
                  .contains("operation #1 (Nowhere): category does not exist");
              assertThat(e.getArgs().get(2)).isEqualTo(FILE);
            });
    assertThat(repository.findByOwner(LOGIN)).isEmpty();
    assertThat(repository.saved()).isEmpty();
  }

  @Test
  public void reportsOnlyTheFirstFewProblems() {
    OperationDto bad = FrExample.operation(OperationType.EXPENSE, "Nowhere", "5");
    storage.putFile(
        new WalletDto(LOGIN, List.of(), List.of(), List.of(bad, bad, bad, bad, bad)));

    assertThatThrownBy(() -> service.get(LOGIN))
        .isInstanceOfSatisfying(
            ApplicationException.class,
            e -> assertThat(e.getArgs().get(1).toString().split("; ")).hasSize(3));
  }

  @Test
  public void savePutsTheWalletInRepositoryAsChanged() {
    WalletDto updated = walletWithFood();

    service.save(updated);

    assertThat(repository.saved()).containsExactly(updated);
    assertThat(service.get(LOGIN)).isSameAs(updated);
  }

  @Test
  public void saveChangesWritesOnlyChangedWalletsAndMarksThemSaved() {
    service.get("bob");
    WalletDto changed = walletWithFood();
    service.save(changed);

    service.saveChanges();

    assertThat(storage.saves()).containsExactly(changed);
    assertThat(repository.findChanged()).isEmpty();
  }

  @Test
  public void secondSaveChangesWritesNothing() {
    service.save(walletWithFood());
    service.saveChanges();

    service.saveChanges();

    assertThat(storage.saves()).hasSize(1);
  }

  @Test
  public void walletChangedAfterTheSaveIsWrittenAgain() {
    service.save(walletWithFood());
    service.saveChanges();
    WalletDto newer = walletWithFood().toBuilder().budgets(List.of()).build();
    service.save(newer);

    service.saveChanges();

    assertThat(storage.saves()).hasSize(2);
    assertThat(storage.saves().get(1)).isSameAs(newer);
  }

  @Test
  public void newWalletThatWasOnlyReadIsNeverWritten() {
    service.get(LOGIN);

    service.saveChanges();

    assertThat(storage.saves()).isEmpty();
  }

  @Test
  public void failedWriteReportsReasonAndKeepsTheWalletChangedSoRetryWorks() {
    WalletDto changed = walletWithFood();
    service.save(changed);
    storage.failSaveWith(new StorageIoException(FILE, "disk full"));

    RefusalAssert.assertRefusalWithArgs(
        service::saveChanges, ErrorCode.SAVE_FAILED, "disk full");
    assertThat(repository.findChanged()).containsExactly(changed);

    storage.stopFailingToSave();
    service.saveChanges();

    assertThat(storage.saves()).containsExactly(changed);
    assertThat(repository.findChanged()).isEmpty();
  }

  @Test
  public void walletsOfDifferentUsersAreIndependent() {
    WalletDto alice = service.get(LOGIN);
    WalletDto bob = service.get("bob");

    assertThat(alice.owner()).isEqualTo(LOGIN);
    assertThat(bob.owner()).isEqualTo("bob");
  }

  private WalletDto walletWithFood() {
    List<CategoryDto> categories =
        List.of(
            new CategoryDto("Transfers", OperationType.INCOME),
            new CategoryDto("Transfers", OperationType.EXPENSE),
            new CategoryDto("Food", OperationType.EXPENSE));
    return new WalletDto(LOGIN, categories, List.of(), List.of());
  }
}
