package org.example.finmanager.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.port.inbound.WalletPort;

/**
 * Тестовый {@link WalletPort}: кошельки в памяти, помнит, чьи кошельки запрашивали, и умеет падать
 * при запросе кошелька заданного пользователя.
 */
public class FakeWalletPort implements WalletPort {

  private final Map<String, WalletDto> wallets = new HashMap<>();
  private final Map<String, RuntimeException> failures = new HashMap<>();
  private final List<String> requested = new ArrayList<>();
  private int saveAllCalls;

  @Override
  public WalletDto get(String login) {
    requested.add(login);
    RuntimeException failure = failures.get(login);
    if (failure != null) {
      throw failure;
    }
    return wallets.computeIfAbsent(login, WalletRules::newWallet);
  }

  @Override
  public void save(WalletDto wallet) {
    wallets.put(wallet.owner(), wallet);
  }

  @Override
  public void saveAll(List<WalletDto> toSave) {
    saveAllCalls++;
    toSave.forEach(this::save);
  }

  @Override
  public void saveChanges() {
    // кошельки живут только в памяти
  }

  /** Заставляет {@code get} для этого пользователя падать с заданным исключением. */
  public void failGetFor(String login, RuntimeException failure) {
    failures.put(login, failure);
  }

  /** Возвращает, сколько раз вызывали {@code saveAll}. */
  public int saveAllCalls() {
    return saveAllCalls;
  }

  /** Возвращает логины, чьи кошельки запрашивали, по порядку. */
  public List<String> requestedLogins() {
    return List.copyOf(requested);
  }
}
