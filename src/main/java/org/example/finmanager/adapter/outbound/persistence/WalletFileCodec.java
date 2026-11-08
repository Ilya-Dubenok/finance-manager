package org.example.finmanager.adapter.outbound.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.outbound.persistence.dto.WalletFileDto;
import org.example.finmanager.adapter.outbound.persistence.mapper.WalletFileMapper;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.exception.StorageFormatException;
import org.springframework.stereotype.Component;

/**
 * Кодирует кошелёк в JSON формата из спецификации и разбирает его обратно. Общий для файла
 * кошелька и снимка. Проверяет только формат: версию, JSON, значения сумм, дат и типов.
 */
@Component
@RequiredArgsConstructor
public class WalletFileCodec {

  private static final String NO_DOCUMENT = "the file contains no JSON document";
  private static final String UNSUPPORTED_VERSION = "unsupported format version %d";
  private static final String INVALID_VALUE = "invalid value: %s";

  private final ObjectMapper objectMapper;
  private final WalletFileMapper fileMapper;

  /**
   * Кодирует кошелёк.
   *
   * @param wallet кошелёк
   * @return JSON в UTF-8
   * @throws JsonProcessingException если кошелёк не удалось превратить в JSON
   */
  public byte[] encode(WalletDto wallet) throws JsonProcessingException {
    WalletFileDto content = fileMapper.toFile(wallet);
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(content);
  }

  /**
   * Разбирает содержимое файла. Владелец в результате — тот, что указан в файле, он может быть
   * {@code null}.
   *
   * @param file файл, откуда взяты данные; нужен для сообщений об ошибках
   * @param bytes содержимое файла
   * @return кошелёк
   * @throws StorageFormatException если содержимое не соответствует формату
   */
  public WalletDto decode(Path file, byte[] bytes) {
    WalletFileDto content = parse(file, bytes);
    if (content == null) {
      throw new StorageFormatException(file, NO_DOCUMENT);
    }
    if (content.version() != WalletFileDto.CURRENT_VERSION) {
      throw new StorageFormatException(file, UNSUPPORTED_VERSION.formatted(content.version()));
    }
    try {
      return fileMapper.toWallet(content);
    } catch (RuntimeException e) {
      throw new StorageFormatException(file, INVALID_VALUE.formatted(e.getMessage()));
    }
  }

  private WalletFileDto parse(Path file, byte[] bytes) {
    try {
      return objectMapper.readValue(bytes, WalletFileDto.class);
    } catch (JsonProcessingException e) {
      throw new StorageFormatException(file, e.getOriginalMessage());
    } catch (IOException e) {
      throw new StorageFormatException(file, e.toString());
    }
  }
}
