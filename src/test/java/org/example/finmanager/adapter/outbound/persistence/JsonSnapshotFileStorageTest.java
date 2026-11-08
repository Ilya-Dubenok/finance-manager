package org.example.finmanager.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.example.finmanager.adapter.outbound.persistence.mapper.WalletFileMapperImpl;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.exception.StorageFileExistsException;
import org.example.finmanager.application.exception.StorageFormatException;
import org.example.finmanager.application.exception.StorageIoException;
import org.example.finmanager.application.service.FrExample;
import org.example.finmanager.config.AppConfig;
import org.example.finmanager.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JsonSnapshotFileStorageTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-08T12:34:56Z"), ZoneOffset.UTC);

  @TempDir private Path dataDir;

  private JsonSnapshotFileStorage storage;

  @BeforeEach
  public void createStorage() {
    ObjectMapper objectMapper = new AppConfig().objectMapper();
    WalletFileCodec codec = new WalletFileCodec(objectMapper, new WalletFileMapperImpl());
    ExportFiles exportFiles =
        new ExportFiles(new AppProperties(dataDir), CLOCK, new AtomicFileWriter());
    storage = new JsonSnapshotFileStorage(codec, exportFiles);
  }

  @Test
  public void writtenSnapshotIsReadBackUnchanged() {
    Path file = dataDir.resolve("snapshot.json");
    WalletDto wallet = FrExample.wallet();

    Path written = storage.write(wallet, file, false);

    assertThat(written).isEqualTo(file);
    assertThat(storage.read(file)).isEqualTo(wallet);
  }

  @Test
  public void defaultFileIsInTheExportsDirectory() {
    Path written = storage.write(FrExample.wallet(), null, false);

    assertThat(written)
        .isEqualTo(dataDir.resolve("exports").resolve("alice-20261108-123456.json"));
    assertThat(written).exists();
  }

  @Test
  public void existingFileIsNotReplacedWithoutPermission() throws IOException {
    Path file = dataDir.resolve("snapshot.json");
    Files.writeString(file, "precious");

    assertThatThrownBy(() -> storage.write(FrExample.wallet(), file, false))
        .isInstanceOf(StorageFileExistsException.class);
    assertThat(file).hasContent("precious");

    storage.write(FrExample.wallet(), file, true);

    assertThat(storage.read(file)).isEqualTo(FrExample.wallet());
  }

  @Test
  public void ownerFromTheFileIsKept() throws IOException {
    Path file = dataDir.resolve("foreign.json");
    Files.writeString(file, "{\"version\": 1, \"owner\": \"mallory\"}");

    assertThat(storage.read(file).owner()).isEqualTo("mallory");
  }

  @Test
  public void missingFileIsIoError() {
    Path file = dataDir.resolve("missing.json");

    assertThatThrownBy(() -> storage.read(file))
        .isInstanceOfSatisfying(
            StorageIoException.class, e -> assertThat(e.getFile()).isEqualTo(file));
  }

  @Test
  public void malformedFileIsFormatErrorWithTheFile() throws IOException {
    Path file = dataDir.resolve("broken.json");
    Files.writeString(file, "not json");

    assertThatThrownBy(() -> storage.read(file))
        .isInstanceOfSatisfying(
            StorageFormatException.class, e -> assertThat(e.getFile()).isEqualTo(file));
  }
}
