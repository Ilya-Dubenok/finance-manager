package org.example.finmanager.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.port.outbound.WalletRepositoryPort;

/** Тестовый {@link WalletRepositoryPort}: помнит, что положили через cache и saveAll. */
public class FakeWalletRepository implements WalletRepositoryPort {

  private final Map<String, WalletDto> wallets = new HashMap<>();
  private final Set<String> changed = new TreeSet<>();
  private final List<WalletDto> cached = new ArrayList<>();
  private final List<WalletDto> saved = new ArrayList<>();

  @Override
  public Optional<WalletDto> findByOwner(String login) {
    return Optional.ofNullable(wallets.get(login));
  }

  @Override
  public void cache(WalletDto wallet) {
    wallets.put(wallet.owner(), wallet);
    cached.add(wallet);
  }

  @Override
  public void saveAll(List<WalletDto> toSave) {
    for (WalletDto wallet : toSave) {
      wallets.put(wallet.owner(), wallet);
      changed.add(wallet.owner());
      saved.add(wallet);
    }
  }

  @Override
  public List<WalletDto> findChanged() {
    return changed.stream().map(wallets::get).toList();
  }

  @Override
  public void markSaved(WalletDto wallet) {
    if (wallets.get(wallet.owner()) == wallet) {
      changed.remove(wallet.owner());
    }
  }

  /** Возвращает кошельки, положенные без пометки «изменён». */
  public List<WalletDto> cached() {
    return List.copyOf(cached);
  }

  /** Возвращает кошельки, положенные как изменённые. */
  public List<WalletDto> saved() {
    return List.copyOf(saved);
  }
}
