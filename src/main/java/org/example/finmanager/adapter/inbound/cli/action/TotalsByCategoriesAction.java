package org.example.finmanager.adapter.inbound.cli.action;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.dto.PeriodForm;
import org.example.finmanager.adapter.inbound.cli.input.PromptReader;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.mapper.CommandMapper;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.adapter.inbound.cli.render.TotalsRenderer;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.result.CategoryTotalsDto;
import org.example.finmanager.application.port.inbound.StatisticsPort;
import org.springframework.stereotype.Component;

/** Пункт «Подсчёт по выбранным категориям»: названия через запятую и необязательный период. */
@Component
@RequiredArgsConstructor
public class TotalsByCategoriesAction implements MenuAction {

  private final ConsoleIo io;
  private final PromptReader prompts;
  private final StatisticsPort statistics;
  private final CliSession session;
  private final CommandMapper commandMapper;
  private final TotalsRenderer renderer;

  @Override
  public MenuOutcome execute() {
    List<String> names = prompts.readList(MessageKeys.PROMPT_CATEGORIES);
    PeriodForm form = prompts.readPeriod();
    PeriodDto period = commandMapper.toPeriod(form);
    CategoryTotalsDto totals = statistics.totalsByCategories(session.login(), names, period);
    io.println(renderer.render(totals));
    return MenuOutcome.STAY;
  }
}
