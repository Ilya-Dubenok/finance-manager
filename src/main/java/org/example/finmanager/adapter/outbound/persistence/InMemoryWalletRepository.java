package org.example.finmanager.adapter.outbound.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.port.outbound.WalletRepositoryPort;
import org.springframework.stereotype.Component;

/**
 * Кошельки в памяти процесса: логин владельца → последняя версия кошелька, плюс множество логинов,
 * чьи кошельки изменены и ещё не записаны на диск.
 */
@Component
public class InMemoryWalletRepository implements WalletRepositoryPort {

  private final Map<String, WalletDto> wallets = new ConcurrentHashMap<>();
  private final Set<String> changed = ConcurrentHashMap.newKeySet();

  @Override
  public Optional<WalletDto> findByOwner(String login) {
    return Optional.ofNullable(wallets.get(login));
  }

  @Override
  public synchronized void cache(WalletDto wallet) {
    wallets.put(wallet.owner(), wallet);
  }

  @Override
  public synchronized void saveAll(List<WalletDto> toSave) {
    for (WalletDto wallet : toSave) {
      wallets.put(wallet.owner(), wallet);
      changed.add(wallet.owner());
    }
  }

  @Override
  public synchronized List<WalletDto> findChanged() {
    return changed.stream().sorted().map(wallets::get).toList();
  }

  @Override
  public synchronized void markSaved(WalletDto saved) {
    if (wallets.get(saved.owner()) == saved) {
      changed.remove(saved.owner());
    }
  }
}
