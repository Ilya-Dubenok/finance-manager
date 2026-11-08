package org.example.finmanager.adapter.outbound.persistence.mapper;

import org.example.finmanager.adapter.outbound.persistence.dto.WalletFileDto;
import org.example.finmanager.application.dto.model.WalletDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Превращает кошелёк в содержимое файла и обратно. Суммы и даты переводятся в строки и из строк
 * средствами MapStruct; неверное значение даёт исключение, которое хранилище считает ошибкой
 * формата.
 */
@Mapper(componentModel = "spring", imports = WalletFileDto.class)
public interface WalletFileMapper {

  /** Собирает содержимое файла; версия — текущая. */
  @Mapping(target = "version", expression = "java(WalletFileDto.CURRENT_VERSION)")
  WalletFileDto toFile(WalletDto wallet);

  /** Собирает кошелёк из содержимого файла. */
  WalletDto toWallet(WalletFileDto file);
}
