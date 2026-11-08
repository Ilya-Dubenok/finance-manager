package org.example.finmanager.adapter.outbound.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.exception.StorageIoException;
import org.example.finmanager.application.port.outbound.WalletFileStorageOutboundPort;
import org.example.finmanager.config.AppProperties;
import org.springframework.stereotype.Component;

/**
 * Кошельки в JSON-файлах {@code <каталог данных>/wallets/<login>.json}. Запись атомарная,
 * неизвестные поля при чтении игнорируются. Правила содержимого проверяет application, здесь —
 * только формат (его разбирает {@link WalletFileCodec}).
 */
@Component
@RequiredArgsConstructor
public class JsonWalletFileStorage implements WalletFileStorageOutboundPort {

  private static final String WALLETS_DIRECTORY = "wallets";
  private static final String FILE_EXTENSION = ".json";

  private final AppProperties properties;
  private final WalletFileCodec codec;
  private final AtomicFileWriter fileWriter;

  @Override
  public Path fileOf(String login) {
    return properties.dataDir().resolve(WALLETS_DIRECTORY).resolve(login + FILE_EXTENSION);
  }

  @Override
  public Optional<WalletDto> load(String login) {
    Path file = fileOf(login);
    if (!Files.exists(file)) {
      return Optional.empty();
    }
    WalletDto wallet = codec.decode(file, readBytes(file));
    return Optional.of(wallet.toBuilder().owner(login).build());
  }

  @Override
  public void save(WalletDto wallet) {
    Path file = fileOf(wallet.owner());
    try {
      fileWriter.write(file, codec.encode(wallet));
    } catch (IOException e) {
      throw new StorageIoException(file, e.toString());
    }
  }

  private byte[] readBytes(Path file) {
    try {
      return Files.readAllBytes(file);
    } catch (IOException e) {
      throw new StorageIoException(file, e.toString());
    }
  }
}
