package org.example.finmanager.adapter.inbound.cli.action;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.adapter.inbound.cli.render.MoneyFormatter;
import org.example.finmanager.adapter.inbound.cli.render.TableRenderer;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.result.CategoryViewDto;
import org.example.finmanager.application.port.inbound.CategoryPort;
import org.springframework.stereotype.Component;

/**
 * Пункт «Список категорий»: две таблицы, доходов и расходов, по алфавиту; в таблице расходов
 * видны лимиты бюджетов.
 */
@Component
@RequiredArgsConstructor
public class ListCategoriesAction implements MenuAction {

  private static final String LABEL_SEPARATOR = " ";
  private static final String NO_BUDGET = "-";
  private static final Set<Integer> BUDGET_COLUMN = Set.of(1);

  private final ConsoleIo io;
  private final MessageCatalog messages;
  private final CategoryPort categories;
  private final CliSession session;

  @Override
  public MenuOutcome execute() {
    List<CategoryViewDto> all = categories.listCategories(session.login());
    printIncome(categoriesOf(all, OperationType.INCOME));
    io.println();
    printExpense(categoriesOf(all, OperationType.EXPENSE));
    return MenuOutcome.STAY;
  }

  private List<CategoryViewDto> categoriesOf(List<CategoryViewDto> all, OperationType type) {
    return all.stream().filter(category -> category.type() == type).toList();
  }

  private void printIncome(List<CategoryViewDto> income) {
    List<String> headers = List.of(messages.get(MessageKeys.TABLE_CATEGORY));
    List<List<String>> rows = income.stream().map(category -> List.of(label(category))).toList();
    io.println(messages.get(MessageKeys.CATEGORIES_INCOME_TITLE));
    io.println(TableRenderer.render(headers, rows, Set.of()));
  }

  private void printExpense(List<CategoryViewDto> expense) {
    List<String> headers =
        List.of(
            messages.get(MessageKeys.TABLE_CATEGORY), messages.get(MessageKeys.TABLE_BUDGET));
    List<List<String>> rows =
        expense.stream().map(category -> List.of(label(category), budget(category))).toList();
    io.println(messages.get(MessageKeys.CATEGORIES_EXPENSE_TITLE));
    io.println(TableRenderer.render(headers, rows, BUDGET_COLUMN));
  }

  private String label(CategoryViewDto category) {
    if (!category.system()) {
      return category.name();
    }
    return category.name() + LABEL_SEPARATOR + messages.get(MessageKeys.CATEGORY_SYSTEM_LABEL);
  }

  private String budget(CategoryViewDto category) {
    if (category.budgetLimit() == null) {
      return NO_BUDGET;
    }
    return MoneyFormatter.format(category.budgetLimit());
  }
}
