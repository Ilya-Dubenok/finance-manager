package org.example.finmanager.adapter.inbound.cli.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.example.finmanager.adapter.inbound.cli.dto.OperationRow;
import org.example.finmanager.adapter.inbound.cli.render.CommentFormatter;
import org.example.finmanager.adapter.inbound.cli.render.DateFormatter;
import org.example.finmanager.adapter.inbound.cli.render.MoneyFormatter;
import org.example.finmanager.adapter.inbound.cli.render.TypeFormatter;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * Превращает операцию в строку таблицы: деньги, даты и типы форматируются для вывода.
 *
 * <p>Форматтеры — утилитные классы Lombok. MapStruct не видит их статические методы (Lombok
 * добавляет {@code static} позже) и пытался бы внедрить их как бины, поэтому вызываем их из
 * методов по умолчанию самого маппера.
 */
@Mapper(componentModel = "spring")
public interface OperationRowMapper {

  /** Имя метода сокращения комментария для {@code @Mapping(qualifiedByName)}. */
  String SHORT_COMMENT = "shortComment";

  /** Собирает строку таблицы. */
  @Mapping(target = "comment", source = "comment", qualifiedByName = SHORT_COMMENT)
  OperationRow toRow(OperationDto operation);

  /** Форматирует дату для вывода. */
  default String formatDate(LocalDate date) {
    return DateFormatter.format(date);
  }

  /** Форматирует сумму для вывода. */
  default String formatAmount(BigDecimal amount) {
    return MoneyFormatter.format(amount);
  }

  /** Возвращает название типа для вывода. */
  default String formatType(OperationType type) {
    return TypeFormatter.format(type);
  }

  /** Сокращает комментарий для таблицы. */
  @Named(SHORT_COMMENT)
  default String shortComment(String comment) {
    return CommentFormatter.truncate(comment);
  }
}
