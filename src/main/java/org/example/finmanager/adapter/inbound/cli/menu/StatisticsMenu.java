package org.example.finmanager.adapter.inbound.cli.menu;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.action.SummaryAction;
import org.example.finmanager.adapter.inbound.cli.action.TotalsByCategoriesAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.Menu;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuItem;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.springframework.stereotype.Component;

/** Меню статистики: сводка и подсчёт по выбранным категориям. */
@Component
@RequiredArgsConstructor
public class StatisticsMenu {

  private static final int SUMMARY_NUMBER = 1;
  private static final int TOTALS_NUMBER = 2;

  private final MessageCatalog messages;
  private final SummaryAction summaryAction;
  private final TotalsByCategoriesAction totalsAction;

  /** Строит меню. */
  public Menu build() {
    String title = messages.get(MessageKeys.MENU_STATISTICS_TITLE);
    String summaryLabel = messages.get(MessageKeys.MENU_STATISTICS_SUMMARY);
    MenuItem summary = new MenuItem(SUMMARY_NUMBER, summaryLabel, summaryAction);
    String totalsLabel = messages.get(MessageKeys.MENU_STATISTICS_TOTALS);
    MenuItem totals = new MenuItem(TOTALS_NUMBER, totalsLabel, totalsAction);
    MenuItem back = MenuItem.leave(messages.get(MessageKeys.MENU_BACK));
    return new Menu(title, List.of(summary, totals, back));
  }
}
