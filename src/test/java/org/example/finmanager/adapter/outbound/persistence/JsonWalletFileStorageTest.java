package org.example.finmanager.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.example.finmanager.adapter.outbound.persistence.mapper.WalletFileMapper;
import org.example.finmanager.adapter.outbound.persistence.mapper.WalletFileMapperImpl;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.exception.StorageFormatException;
import org.example.finmanager.application.exception.StorageIoException;
import org.example.finmanager.config.AppConfig;
import org.example.finmanager.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class JsonWalletFileStorageTest {

  private static final String LOGIN = "alice";
  private static final LocalDate DATE = LocalDate.of(2026, 11, 8);

  @TempDir private Path dataDir;

  private final ObjectMapper objectMapper = new AppConfig().objectMapper();
  private final WalletFileMapper fileMapper = new WalletFileMapperImpl();

  private JsonWalletFileStorage storage;

  @BeforeEach
  public void createStorage() {
    WalletFileCodec codec = new WalletFileCodec(objectMapper, fileMapper);
    storage =
        new JsonWalletFileStorage(new AppProperties(dataDir), codec, new AtomicFileWriter());
  }

  @Test
  public void fileIsNamedAfterTheLoginInTheWalletsDirectory() {
    assertThat(storage.fileOf(LOGIN)).isEqualTo(dataDir.resolve("wallets").resolve("alice.json"));
  }

  @Test
  public void missingFileGivesNoWallet() {
    assertThat(storage.load(LOGIN)).isEmpty();
  }

  @Test
  public void savedWalletIsLoadedBackUnchanged() {
    WalletDto wallet = wallet();

    storage.save(wallet);

    assertThat(storage.load(LOGIN)).contains(wallet);
  }

  @Test
  public void fileHasTheFormatFromTheSpecification() throws IOException {
    storage.save(wallet());

    JsonNode root = objectMapper.readTree(storage.fileOf(LOGIN).toFile());

    assertThat(root.get("version").asInt()).isEqualTo(1);
    assertThat(root.get("owner").asText()).isEqualTo(LOGIN);
    assertThat(root.get("categories").get(0).get("type").asText()).isEqualTo("EXPENSE");
    assertThat(root.get("budgets").get(0).get("limit").isTextual()).isTrue();
    assertThat(root.get("budgets").get(0).get("limit").asText()).isEqualTo("4000.00");
    JsonNode operation = root.get("operations").get(0);
    assertThat(operation.get("amount").asText()).isEqualTo("300.00");
    assertThat(operation.get("date").asText()).isEqualTo("2026-11-08");
    assertThat(operation.get("comment").isNull()).isTrue();
    assertThat(operation.get("counterparty").isNull()).isTrue();
  }

  @Test
  public void savingTwiceReplacesTheFileAndLeavesNoTemporaryFiles() throws IOException {
    storage.save(wallet());
    storage.save(wallet().toBuilder().budgets(List.of()).build());

    assertThat(storage.load(LOGIN).orElseThrow().budgets()).isEmpty();
    try (Stream<Path> files = Files.list(storage.fileOf(LOGIN).getParent())) {
      assertThat(files.map(path -> path.getFileName().toString()).toList())
          .containsExactly("alice.json");
    }
  }

  @Test
  public void ownerIsTakenFromTheLoginNotFromTheFile() throws IOException {
    write("{\"version\": 1, \"owner\": \"mallory\"}");

    assertThat(storage.load(LOGIN).orElseThrow().owner()).isEqualTo(LOGIN);
  }

  @Test
  public void unknownFieldsAreIgnoredAndMissingListsAreEmpty() throws IOException {
    write("{\"version\": 1, \"future\": {\"x\": 1}}");

    Optional<WalletDto> wallet = storage.load(LOGIN);

    assertThat(wallet).isPresent();
    assertThat(wallet.get().categories()).isEmpty();
    assertThat(wallet.get().operations()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "not json",
        "null",
        "{}",
        "{\"owner\": \"alice\"}",
        "{\"version\": 2}",
        "{\"version\": \"one\"}",
        "{\"version\": 1, \"categories\": [null]}",
        "{\"version\": 1, \"categories\": [{\"name\": \"Food\", \"type\": \"SPEND\"}]}",
        "{\"version\": 1, \"budgets\": [{\"category\": \"Food\", \"limit\": \"abc\"}]}",
        "{\"version\": 1, \"operations\": [{\"type\": \"EXPENSE\", \"category\": \"Food\", "
            + "\"amount\": \"1.5.2\", \"date\": \"2026-11-08\"}]}",
        "{\"version\": 1, \"operations\": [{\"type\": \"EXPENSE\", \"category\": \"Food\", "
            + "\"amount\": \"5\", \"date\": \"08.11.2026\"}]}"
      })
  public void contentThatDoesNotMatchTheFormatIsReportedWithTheFile(String content)
      throws IOException {
    write(content);

    assertThatThrownBy(() -> storage.load(LOGIN))
        .isInstanceOfSatisfying(
            StorageFormatException.class,
            e -> {
              assertThat(e.getFile()).isEqualTo(storage.fileOf(LOGIN));
              assertThat(e.getMessage()).isNotBlank();
            });
  }

  @Test
  public void invalidUtf8IsFormatError() throws IOException {
    Files.createDirectories(storage.fileOf(LOGIN).getParent());
    Files.write(storage.fileOf(LOGIN), new byte[] {'{', '"', (byte) 0xC3, (byte) 0x28, '"', '}'});

    assertThatThrownBy(() -> storage.load(LOGIN)).isInstanceOf(StorageFormatException.class);
  }

  @Test
  public void corruptedFileIsNeverModifiedByLoading() throws IOException {
    write("garbage");

    assertThatThrownBy(() -> storage.load(LOGIN)).isInstanceOf(StorageFormatException.class);

    assertThat(storage.fileOf(LOGIN)).hasContent("garbage");
  }

  @Test
  public void failedWriteIsReportedAsIoErrorWithTheFile() throws IOException {
    Files.writeString(dataDir.resolve("wallets"), "a file where a directory is needed");

    assertThatThrownBy(() -> storage.save(wallet()))
        .isInstanceOfSatisfying(
            StorageIoException.class,
            e -> assertThat(e.getFile()).isEqualTo(storage.fileOf(LOGIN)));
  }

  private void write(String content) throws IOException {
    Path file = storage.fileOf(LOGIN);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content);
  }

  private WalletDto wallet() {
    OperationDto operation =
        new OperationDto(
            OperationType.EXPENSE, "Food", new BigDecimal("300.00"), DATE, null, null);
    OperationDto transfer =
        new OperationDto(
            OperationType.INCOME, "Transfers", new BigDecimal("12.50"), DATE, "gift", "bob");
    return new WalletDto(
        LOGIN,
        List.of(
            new CategoryDto("Food", OperationType.EXPENSE),
            new CategoryDto("Transfers", OperationType.INCOME)),
        List.of(new BudgetDto("Food", new BigDecimal("4000.00"))),
        List.of(operation, transfer));
  }
}
