package org.example.finmanager.adapter.inbound.cli.menu;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.action.ExportCsvAction;
import org.example.finmanager.adapter.inbound.cli.action.ExportSnapshotAction;
import org.example.finmanager.adapter.inbound.cli.action.ImportSnapshotAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.Menu;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuItem;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.springframework.stereotype.Component;

/** Меню отчётов: экспорт JSON-снимка, экспорт операций в CSV, импорт JSON-снимка. */
@Component
@RequiredArgsConstructor
public class ReportsMenu {

  private static final int EXPORT_SNAPSHOT_NUMBER = 1;
  private static final int EXPORT_CSV_NUMBER = 2;
  private static final int IMPORT_NUMBER = 3;

  private final MessageCatalog messages;
  private final ExportSnapshotAction exportSnapshotAction;
  private final ExportCsvAction exportCsvAction;
  private final ImportSnapshotAction importSnapshotAction;

  /** Строит меню. */
  public Menu build() {
    String title = messages.get(MessageKeys.MENU_REPORTS_TITLE);
    String snapshotLabel = messages.get(MessageKeys.MENU_REPORTS_EXPORT_JSON);
    MenuItem snapshot = new MenuItem(EXPORT_SNAPSHOT_NUMBER, snapshotLabel, exportSnapshotAction);
    String csvLabel = messages.get(MessageKeys.MENU_REPORTS_EXPORT_CSV);
    MenuItem csv = new MenuItem(EXPORT_CSV_NUMBER, csvLabel, exportCsvAction);
    String importLabel = messages.get(MessageKeys.MENU_REPORTS_IMPORT);
    MenuItem importItem = new MenuItem(IMPORT_NUMBER, importLabel, importSnapshotAction);
    MenuItem back = MenuItem.leave(messages.get(MessageKeys.MENU_BACK));
    return new Menu(title, List.of(snapshot, csv, importItem, back));
  }
}
