package org.example.finmanager.adapter.inbound.cli.action;

import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.port.inbound.ReportPort;
import org.springframework.stereotype.Component;

/** Пункт «Экспорт операций в CSV». */
@Component
@RequiredArgsConstructor
public class ExportCsvAction implements MenuAction {

  private final ExportDialog dialog;
  private final ReportPort reports;

  @Override
  public MenuOutcome execute() {
    dialog.run(reports::exportOperationsCsv, MessageKeys.EXPORT_CSV_DONE);
    return MenuOutcome.STAY;
  }
}
