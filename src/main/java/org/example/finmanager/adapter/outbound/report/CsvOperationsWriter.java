package org.example.finmanager.adapter.outbound.report;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.outbound.persistence.ExportFiles;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.port.outbound.OperationsCsvOutboundPort;
import org.springframework.stereotype.Component;

/**
 * Операции в CSV для Excel (D-27): UTF-8 с BOM, разделитель {@code ;}, перевод строки CRLF, первая
 * строка — заголовок. Значения с {@code ;}, кавычкой или переводом строки берутся в кавычки,
 * внутренние кавычки удваиваются.
 */
@Component
@RequiredArgsConstructor
public class CsvOperationsWriter implements OperationsCsvOutboundPort {

  private static final String FILE_EXTENSION = ".csv";
  private static final String BOM = String.valueOf((char) 0xFEFF);
  private static final String SEPARATOR = ";";
  private static final String LINE_BREAK = "\r\n";
  private static final String QUOTE = "\"";
  private static final String HEADER = "date;type;category;amount;counterparty;comment";
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final String EMPTY = "";
  private static final String NEW_LINE = String.valueOf((char) 10);
  private static final String CARRIAGE_RETURN = String.valueOf((char) 13);

  private final ExportFiles exportFiles;

  @Override
  public Path write(String login, List<OperationDto> operations, Path target, boolean overwrite) {
    Path file = exportFiles.targetFor(login, FILE_EXTENSION, target);
    byte[] content = text(operations).getBytes(StandardCharsets.UTF_8);
    exportFiles.write(file, content, overwrite);
    return file;
  }

  private String text(List<OperationDto> operations) {
    List<String> lines = new ArrayList<>();
    lines.add(HEADER);
    operations.stream().map(this::line).forEach(lines::add);
    return BOM + String.join(LINE_BREAK, lines) + LINE_BREAK;
  }

  private String line(OperationDto operation) {
    List<String> values =
        List.of(
            DATE_FORMAT.format(operation.date()),
            operation.type().name(),
            escape(operation.category()),
            operation.amount().toPlainString(),
            escape(operation.counterparty()),
            escape(operation.comment()));
    return String.join(SEPARATOR, values);
  }

  private String escape(String value) {
    if (value == null) {
      return EMPTY;
    }
    boolean needsQuotes =
        value.contains(SEPARATOR)
            || value.contains(QUOTE)
            || value.contains(NEW_LINE)
            || value.contains(CARRIAGE_RETURN);
    if (!needsQuotes) {
      return value;
    }
    return QUOTE + value.replace(QUOTE, QUOTE + QUOTE) + QUOTE;
  }
}
