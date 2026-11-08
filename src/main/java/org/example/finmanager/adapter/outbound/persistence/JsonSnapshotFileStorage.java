package org.example.finmanager.adapter.outbound.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.exception.StorageIoException;
import org.example.finmanager.application.port.outbound.SnapshotFileOutboundPort;
import org.springframework.stereotype.Component;

/** Снимки кошелька в JSON по произвольному пути; формат тот же, что у файла кошелька. */
@Component
@RequiredArgsConstructor
public class JsonSnapshotFileStorage implements SnapshotFileOutboundPort {

  private static final String FILE_EXTENSION = ".json";

  private final WalletFileCodec codec;
  private final ExportFiles exportFiles;

  @Override
  public WalletDto read(Path source) {
    try {
      return codec.decode(source, Files.readAllBytes(source));
    } catch (IOException e) {
      throw new StorageIoException(source, e.toString());
    }
  }

  @Override
  public Path write(WalletDto wallet, Path target, boolean overwrite) {
    Path file = exportFiles.targetFor(wallet.owner(), FILE_EXTENSION, target);
    try {
      exportFiles.write(file, codec.encode(wallet), overwrite);
    } catch (IOException e) {
      throw new StorageIoException(file, e.toString());
    }
    return file;
  }
}
