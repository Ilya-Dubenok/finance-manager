package org.example.finmanager.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.junit.jupiter.api.Test;

public class InMemoryWalletRepositoryTest {

  private static final String ALICE = "alice";
  private static final String BOB = "bob";

  private final InMemoryWalletRepository repository = new InMemoryWalletRepository();

  @Test
  public void findsNothingBeforeAnythingIsStored() {
    assertThat(repository.findByOwner(ALICE)).isEmpty();
  }

  @Test
  public void cachedWalletIsFound() {
    WalletDto wallet = wallet(ALICE);

    repository.cache(wallet);

    assertThat(repository.findByOwner(ALICE)).containsSame(wallet);
  }

  @Test
  public void saveAllStoresEveryWalletUnderItsOwner() {
    WalletDto alice = wallet(ALICE);
    WalletDto bob = wallet(BOB);

    repository.saveAll(List.of(alice, bob));

    assertThat(repository.findByOwner(ALICE)).containsSame(alice);
    assertThat(repository.findByOwner(BOB)).containsSame(bob);
  }

  @Test
  public void newerVersionReplacesTheOlderOne() {
    WalletDto first = wallet(ALICE);
    repository.cache(first);
    WalletDto second =
        first.toBuilder()
            .categories(List.of(new CategoryDto("Food", OperationType.EXPENSE)))
            .build();

    repository.saveAll(List.of(second));

    assertThat(repository.findByOwner(ALICE)).containsSame(second);
  }

  @Test
  public void cachedWalletIsNotChanged() {
    repository.cache(wallet(ALICE));

    assertThat(repository.findChanged()).isEmpty();
  }

  @Test
  public void savedWalletsAreChangedUntilMarkedSaved() {
    WalletDto alice = wallet(ALICE);
    WalletDto bob = wallet(BOB);
    repository.saveAll(List.of(bob, alice));

    assertThat(repository.findChanged()).containsExactly(alice, bob);

    repository.markSaved(alice);

    assertThat(repository.findChanged()).containsExactly(bob);
  }

  @Test
  public void markSavedIgnoresWalletReplacedMeanwhile() {
    WalletDto written = wallet(ALICE);
    repository.saveAll(List.of(written));
    WalletDto newer = wallet(ALICE).toBuilder().categories(List.of()).build();
    repository.saveAll(List.of(newer));

    repository.markSaved(written);

    assertThat(repository.findChanged()).containsExactly(newer);
  }

  @Test
  public void cachingDoesNotClearTheChangedMarkOfTheSameOwner() {
    WalletDto first = wallet(ALICE);
    repository.saveAll(List.of(first));

    repository.markSaved(first);

    assertThat(repository.findChanged()).isEmpty();
  }

  private WalletDto wallet(String owner) {
    return new WalletDto(owner, List.of(), List.of(), List.of());
  }
}
