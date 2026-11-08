package org.example.finmanager.application.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.example.finmanager.application.exception.StorageFormatException;
import org.example.finmanager.application.exception.StorageIoException;
import org.example.finmanager.application.port.inbound.WalletPort;
import org.example.finmanager.application.port.outbound.WalletFileStorageOutboundPort;
import org.example.finmanager.application.port.outbound.WalletRepositoryPort;
import org.springframework.stereotype.Service;

/**
 * Единственный сервис, который обращается к хранилищам кошельков: память, затем файл, затем новый
 * кошелёк. Сохранение и запись изменённых кошельков идут под одним монитором, чтобы главный поток
 * и shutdown-hook не писали один файл одновременно (D-36).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService implements WalletPort {

  private static final int MAX_REPORTED_PROBLEMS = 3;
  private static final String PROBLEM_SEPARATOR = "; ";

  private final WalletRepositoryPort repository;
  private final WalletFileStorageOutboundPort storage;

  @Override
  public synchronized WalletDto get(String login) {
    Optional<WalletDto> inMemory = repository.findByOwner(login);
    if (inMemory.isPresent()) {
      return inMemory.get();
    }
    WalletDto wallet = loadFromFile(login).orElseGet(() -> createNew(login));
    repository.cache(wallet);
    return wallet;
  }

  @Override
  public synchronized void save(WalletDto wallet) {
    repository.saveAll(List.of(wallet));
  }

  @Override
  public synchronized void saveAll(List<WalletDto> toSave) {
    repository.saveAll(toSave);
  }

  @Override
  public synchronized void saveChanges() {
    for (WalletDto wallet : repository.findChanged()) {
      try {
        storage.save(wallet);
      } catch (StorageIoException e) {
        log.error("Cannot save the wallet of '{}'", wallet.owner(), e);
        throw new ApplicationException(ErrorCode.SAVE_FAILED, e.getMessage());
      }
      repository.markSaved(wallet);
      log.info("Saved the wallet of '{}' to {}", wallet.owner(), storage.fileOf(wallet.owner()));
    }
  }

  private Optional<WalletDto> loadFromFile(String login) {
    Optional<WalletDto> loaded;
    try {
      loaded = storage.load(login);
    } catch (StorageFormatException | StorageIoException e) {
      throw corrupted(login, e.getMessage());
    }
    if (loaded.isEmpty()) {
      return Optional.empty();
    }
    WalletDto wallet = WalletRules.withSystemCategories(loaded.get());
    List<String> problems = WalletValidator.validate(wallet);
    if (!problems.isEmpty()) {
      int count = Math.min(problems.size(), MAX_REPORTED_PROBLEMS);
      throw corrupted(login, String.join(PROBLEM_SEPARATOR, problems.subList(0, count)));
    }
    log.info("Loaded the wallet of '{}' from {}", login, storage.fileOf(login));
    return Optional.of(wallet);
  }

  private WalletDto createNew(String login) {
    log.info("Created a new wallet for '{}'", login);
    return WalletRules.newWallet(login);
  }

  private ApplicationException corrupted(String login, String reason) {
    log.error("The wallet file of '{}' is unusable: {}", login, reason);
    return new ApplicationException(
        ErrorCode.WALLET_CORRUPTED, login, reason, storage.fileOf(login));
  }
}
