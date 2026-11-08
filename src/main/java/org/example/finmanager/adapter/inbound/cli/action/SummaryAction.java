package org.example.finmanager.adapter.inbound.cli.action;

import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.dto.PeriodForm;
import org.example.finmanager.adapter.inbound.cli.input.PromptReader;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.mapper.CommandMapper;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.render.SummaryRenderer;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.result.SummaryDto;
import org.example.finmanager.application.port.inbound.StatisticsPort;
import org.springframework.stereotype.Component;

/** Пункт «Сводка»: спрашивает необязательный период и показывает сводку. */
@Component
@RequiredArgsConstructor
public class SummaryAction implements MenuAction {

  private final ConsoleIo io;
  private final PromptReader prompts;
  private final StatisticsPort statistics;
  private final CliSession session;
  private final CommandMapper commandMapper;
  private final SummaryRenderer renderer;

  @Override
  public MenuOutcome execute() {
    PeriodForm form = prompts.readPeriod();
    PeriodDto period = commandMapper.toPeriod(form);
    SummaryDto summary = statistics.summary(session.login(), period);
    io.println(renderer.render(summary));
    return MenuOutcome.STAY;
  }
}
