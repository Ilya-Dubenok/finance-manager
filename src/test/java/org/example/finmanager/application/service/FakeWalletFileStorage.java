package org.example.finmanager.application.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.exception.StorageIoException;
import org.example.finmanager.application.port.outbound.WalletFileStorageOutboundPort;

/** Тестовое файловое хранилище: «файлы» в памяти, ошибки чтения и записи задаются тестом. */
public class FakeWalletFileStorage implements WalletFileStorageOutboundPort {

  private final Map<String, WalletDto> files = new HashMap<>();
  private final List<WalletDto> saves = new ArrayList<>();
  private RuntimeException loadFailure;
  private StorageIoException saveFailure;
  private int loads;

  @Override
  public Path fileOf(String login) {
    return Path.of("data", "wallets", login + ".json");
  }

  @Override
  public Optional<WalletDto> load(String login) {
    loads++;
    if (loadFailure != null) {
      throw loadFailure;
    }
    return Optional.ofNullable(files.get(login));
  }

  @Override
  public void save(WalletDto wallet) {
    if (saveFailure != null) {
      throw saveFailure;
    }
    files.put(wallet.owner(), wallet);
    saves.add(wallet);
  }

  /** Кладёт «файл» кошелька. */
  public void putFile(WalletDto wallet) {
    files.put(wallet.owner(), wallet);
  }

  /** Заставляет чтение падать с заданным исключением. */
  public void failLoadWith(RuntimeException failure) {
    this.loadFailure = failure;
  }

  /** Заставляет запись падать с заданным исключением. */
  public void failSaveWith(StorageIoException failure) {
    this.saveFailure = failure;
  }

  /** Снимает ошибку записи. */
  public void stopFailingToSave() {
    this.saveFailure = null;
  }

  /** Возвращает успешно записанные кошельки по порядку. */
  public List<WalletDto> saves() {
    return List.copyOf(saves);
  }

  /** Возвращает, сколько раз кошельки читали из «файлов». */
  public int loads() {
    return loads;
  }
}
