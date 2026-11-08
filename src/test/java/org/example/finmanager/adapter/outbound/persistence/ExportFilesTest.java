package org.example.finmanager.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.example.finmanager.application.exception.StorageFileExistsException;
import org.example.finmanager.application.exception.StorageIoException;
import org.example.finmanager.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ExportFilesTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-08T12:34:56Z"), ZoneOffset.UTC);
  private static final byte[] CONTENT = {1, 2, 3};

  @TempDir private Path dataDir;

  private ExportFiles exportFiles;

  @BeforeEach
  public void createExportFiles() {
    exportFiles = new ExportFiles(new AppProperties(dataDir), CLOCK, new AtomicFileWriter());
  }

  @Test
  public void requestedPathIsUsedAsIs() {
    Path requested = dataDir.resolve("mine.json");

    assertThat(exportFiles.targetFor("alice", ".json", requested)).isEqualTo(requested);
  }

  @Test
  public void defaultPathIsInTheExportsDirectoryAndNamedByLoginAndTime() {
    Path target = exportFiles.targetFor("alice", ".csv", null);

    assertThat(target)
        .isEqualTo(dataDir.resolve("exports").resolve("alice-20261108-123456.csv"));
  }

  @Test
  public void writeCreatesMissingDirectories() throws IOException {
    Path target = dataDir.resolve("a").resolve("b").resolve("file.bin");

    exportFiles.write(target, CONTENT, false);

    assertThat(Files.readAllBytes(target)).isEqualTo(CONTENT);
  }

  @Test
  public void existingFileIsNotReplacedWithoutPermission() throws IOException {
    Path target = dataDir.resolve("file.bin");
    Files.writeString(target, "old");

    assertThatThrownBy(() -> exportFiles.write(target, CONTENT, false))
        .isInstanceOfSatisfying(
            StorageFileExistsException.class, e -> assertThat(e.getFile()).isEqualTo(target));
    assertThat(target).hasContent("old");
  }

  @Test
  public void existingFileIsReplacedWithPermission() throws IOException {
    Path target = dataDir.resolve("file.bin");
    Files.writeString(target, "old");

    exportFiles.write(target, CONTENT, true);

    assertThat(Files.readAllBytes(target)).isEqualTo(CONTENT);
  }

  @Test
  public void failedWriteIsReportedWithTheFile() throws IOException {
    Path blocker = dataDir.resolve("blocker");
    Files.writeString(blocker, "a file where a directory is needed");
    Path target = blocker.resolve("file.bin");

    assertThatThrownBy(() -> exportFiles.write(target, CONTENT, true))
        .isInstanceOfSatisfying(
            StorageIoException.class, e -> assertThat(e.getFile()).isEqualTo(target));
  }
}
