package org.example.finmanager.adapter.outbound.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.example.finmanager.adapter.outbound.persistence.AtomicFileWriter;
import org.example.finmanager.adapter.outbound.persistence.ExportFiles;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.exception.StorageFileExistsException;
import org.example.finmanager.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CsvOperationsWriterTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-08T12:34:56Z"), ZoneOffset.UTC);
  private static final String BOM = String.valueOf((char) 0xFEFF);
  private static final String CRLF = "\r\n";
  private static final String HEADER = "date;type;category;amount;counterparty;comment";
  private static final LocalDate DATE = LocalDate.of(2026, 11, 8);

  @TempDir private Path dataDir;

  private CsvOperationsWriter writer;

  @BeforeEach
  public void createWriter() {
    ExportFiles exportFiles =
        new ExportFiles(new AppProperties(dataDir), CLOCK, new AtomicFileWriter());
    writer = new CsvOperationsWriter(exportFiles);
  }

  @Test
  public void fileStartsWithBomAndHeaderAndUsesCrlf() throws IOException {
    Path file = dataDir.resolve("ops.csv");
    OperationDto operation = operation(OperationType.EXPENSE, "Food", "300.00", null, null);

    writer.write("alice", List.of(operation), file, false);

    assertThat(text(file))
        .isEqualTo(BOM + HEADER + CRLF + "08.11.2026;EXPENSE;Food;300.00;;" + CRLF);
  }

  @Test
  public void bytesStartWithTheUtf8Bom() throws IOException {
    Path file = dataDir.resolve("ops.csv");

    writer.write("alice", List.of(), file, false);

    byte[] bytes = Files.readAllBytes(file);
    assertThat(bytes[0]).isEqualTo((byte) 0xEF);
    assertThat(bytes[1]).isEqualTo((byte) 0xBB);
    assertThat(bytes[2]).isEqualTo((byte) 0xBF);
  }

  @Test
  public void emptyWalletGivesOnlyTheHeader() throws IOException {
    Path file = dataDir.resolve("ops.csv");

    writer.write("alice", List.of(), file, false);

    assertThat(text(file)).isEqualTo(BOM + HEADER + CRLF);
  }

  @Test
  public void counterpartyAndCommentAreWritten() throws IOException {
    Path file = dataDir.resolve("ops.csv");
    OperationDto operation = operation(OperationType.INCOME, "Transfers", "12.5", "bob", "gift");

    writer.write("alice", List.of(operation), file, false);

    assertThat(text(file)).contains("08.11.2026;INCOME;Transfers;12.5;bob;gift" + CRLF);
  }

  @Test
  public void valuesWithSeparatorsQuotesOrLineBreaksAreQuoted() throws IOException {
    Path file = dataDir.resolve("ops.csv");
    String comment = "a;b \"c\"" + System.lineSeparator() + "d";
    OperationDto operation = operation(OperationType.EXPENSE, "Food", "1", null, comment);

    writer.write("alice", List.of(operation), file, false);

    String expectedComment = "\"a;b \"\"c\"\"" + System.lineSeparator() + "d\"";
    assertThat(text(file)).contains(";1;;" + expectedComment + CRLF);
  }

  @Test
  public void cyrillicTextSurvivesAsUtf8() throws IOException {
    Path file = dataDir.resolve("ops.csv");
    String comment = "пример";
    OperationDto operation = operation(OperationType.EXPENSE, "Food", "1", null, comment);

    writer.write("alice", List.of(operation), file, false);

    assertThat(text(file)).contains(comment);
  }

  @Test
  public void operationsKeepTheirOrder() throws IOException {
    Path file = dataDir.resolve("ops.csv");
    OperationDto first = operation(OperationType.EXPENSE, "Food", "1", null, null);
    OperationDto second = operation(OperationType.INCOME, "Salary", "2", null, null);

    writer.write("alice", List.of(first, second), file, false);

    assertThat(text(file)).containsSubsequence("EXPENSE;Food", "INCOME;Salary");
  }

  @Test
  public void defaultFileIsInTheExportsDirectory() {
    Path written = writer.write("alice", List.of(), null, false);

    assertThat(written)
        .isEqualTo(dataDir.resolve("exports").resolve("alice-20261108-123456.csv"));
    assertThat(written).exists();
  }

  @Test
  public void existingFileIsNotReplacedWithoutPermission() throws IOException {
    Path file = dataDir.resolve("ops.csv");
    Files.writeString(file, "precious");

    assertThatThrownBy(() -> writer.write("alice", List.of(), file, false))
        .isInstanceOf(StorageFileExistsException.class);
    assertThat(file).hasContent("precious");

    writer.write("alice", List.of(), file, true);

    assertThat(text(file)).isEqualTo(BOM + HEADER + CRLF);
  }

  private String text(Path file) throws IOException {
    return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
  }

  private OperationDto operation(
      OperationType type, String category, String amount, String counterparty, String comment) {
    return new OperationDto(type, category, new BigDecimal(amount), DATE, comment, counterparty);
  }
}
