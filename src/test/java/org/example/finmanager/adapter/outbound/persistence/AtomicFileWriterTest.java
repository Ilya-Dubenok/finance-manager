package org.example.finmanager.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AtomicFileWriterTest {

  @TempDir private Path tempDir;

  private final AtomicFileWriter writer = new AtomicFileWriter();

  @Test
  public void createsMissingDirectoriesAndWritesTheContent() throws IOException {
    Path target = tempDir.resolve("a").resolve("b").resolve("file.json");

    writer.write(target, bytes("content"));

    assertThat(target).hasContent("content");
  }

  @Test
  public void replacesExistingFile() throws IOException {
    Path target = tempDir.resolve("file.json");
    Files.writeString(target, "old");

    writer.write(target, bytes("new"));

    assertThat(target).hasContent("new");
  }

  @Test
  public void leavesNoTemporaryFilesBehind() throws IOException {
    Path target = tempDir.resolve("file.json");

    writer.write(target, bytes("one"));
    writer.write(target, bytes("two"));

    assertThat(namesIn(tempDir)).containsExactly("file.json");
  }

  @Test
  public void failedWriteKeepsThePreviousStateAndCleansUp() throws IOException {
    Path target = tempDir.resolve("target");
    Files.createDirectory(target);
    Files.writeString(target.resolve("keep.txt"), "keep");

    assertThatThrownBy(() -> writer.write(target, bytes("data"))).isInstanceOf(IOException.class);

    assertThat(target.resolve("keep.txt")).hasContent("keep");
    assertThat(namesIn(tempDir)).containsExactly("target");
  }

  @Test
  public void writesUtf8BytesAsGiven() throws IOException {
    Path target = tempDir.resolve("file.json");

    writer.write(target, bytes("héllo"));

    assertThat(Files.readAllBytes(target)).isEqualTo(bytes("héllo"));
  }

  private byte[] bytes(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  private List<String> namesIn(Path directory) throws IOException {
    try (Stream<Path> files = Files.list(directory)) {
      return files.map(path -> path.getFileName().toString()).sorted().toList();
    }
  }
}
