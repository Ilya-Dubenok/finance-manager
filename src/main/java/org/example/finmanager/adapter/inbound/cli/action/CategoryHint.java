package org.example.finmanager.adapter.inbound.cli.action;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.result.CategoryViewDto;
import org.example.finmanager.application.port.inbound.CategoryPort;
import org.springframework.stereotype.Component;

/**
 * Подсказка перед вопросом о категории: названия категорий нужного типа, чтобы пользователю не
 * приходилось вспоминать их или сначала открывать список.
 */
@Component
@RequiredArgsConstructor
public class CategoryHint {

  private final ConsoleIo io;
  private final MessageCatalog messages;
  private final CategoryPort categories;
  private final CliSession session;

  /**
   * Печатает названия категорий типа по алфавиту; если таких категорий нет, ничего не печатает.
   *
   * @param type тип категорий
   */
  public void show(OperationType type) {
    List<CategoryViewDto> all = categories.listCategories(session.login());
    List<String> names =
        all.stream()
            .filter(category -> category.type() == type)
            .map(CategoryViewDto::name)
            .toList();
    if (names.isEmpty()) {
      return;
    }
    io.println(messages.get(MessageKeys.HINT_CATEGORIES, names));
  }
}
