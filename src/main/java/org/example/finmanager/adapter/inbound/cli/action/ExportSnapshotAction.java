package org.example.finmanager.adapter.inbound.cli.action;

import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.port.inbound.ReportPort;
import org.springframework.stereotype.Component;

/** Пункт «Экспорт JSON-снимка». */
@Component
@RequiredArgsConstructor
public class ExportSnapshotAction implements MenuAction {

  private final ExportDialog dialog;
  private final ReportPort reports;

  @Override
  public MenuOutcome execute() {
    dialog.run(reports::exportSnapshot, MessageKeys.EXPORT_SNAPSHOT_DONE);
    return MenuOutcome.STAY;
  }
}
