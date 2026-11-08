package org.example.finmanager.adapter.inbound.cli.action;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.dto.TransferForm;
import org.example.finmanager.adapter.inbound.cli.input.PromptReader;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.mapper.CommandMapper;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.adapter.inbound.cli.render.AlertRenderer;
import org.example.finmanager.application.dto.command.TransferCommand;
import org.example.finmanager.application.dto.result.TransferResultDto;
import org.example.finmanager.application.port.inbound.TransferPort;
import org.springframework.stereotype.Component;

/** Пункт «Перевод»: спрашивает логин получателя, сумму и комментарий. */
@Component
@RequiredArgsConstructor
public class TransferAction implements MenuAction {

  private final ConsoleIo io;
  private final MessageCatalog messages;
  private final PromptReader prompts;
  private final TransferPort transfers;
  private final CliSession session;
  private final CommandMapper commandMapper;
  private final AlertRenderer alertRenderer;

  @Override
  public MenuOutcome execute() {
    String recipient = prompts.readRequired(MessageKeys.PROMPT_RECIPIENT);
    BigDecimal amount = prompts.readAmount(MessageKeys.PROMPT_AMOUNT);
    String comment = prompts.readOptional(MessageKeys.PROMPT_COMMENT);

    TransferForm form = new TransferForm(recipient, amount, comment);
    TransferCommand command = commandMapper.toCommand(session.login(), form);
    TransferResultDto result = transfers.transfer(command);

    BigDecimal transferred = result.fromOperation().amount();
    io.println(messages.get(MessageKeys.TRANSFER_DONE, transferred, result.toLogin()));
    alertRenderer.render(result.alerts()).forEach(io::println);
    return MenuOutcome.STAY;
  }
}
