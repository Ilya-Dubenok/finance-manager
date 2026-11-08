package org.example.finmanager.adapter.inbound.cli.action;

import java.nio.file.Path;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.dto.ExportForm;
import org.example.finmanager.adapter.inbound.cli.input.PromptReader;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.mapper.CommandMapper;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.dto.command.ExportCommand;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * Общий диалог экспорта: спрашивает путь, а если файл уже есть — разрешение его заменить.
 */
@Component
@RequiredArgsConstructor
public class ExportDialog {

  private final ConsoleIo io;
  private final MessageCatalog messages;
  private final PromptReader prompts;
  private final CliSession session;
  private final CommandMapper commandMapper;

  /**
   * Проводит диалог и выполняет экспорт.
   *
   * @param export выполняет экспорт и возвращает путь к записанному файлу
   * @param successKey ключ сообщения об успехе; в него подставляется путь
   */
  public void run(Function<ExportCommand, Path> export, String successKey) {
    Path target = prompts.readOptionalPath(MessageKeys.PROMPT_EXPORT_PATH);
    Path written = exportWithOverwriteQuestion(export, target);
    if (written != null) {
      io.println(messages.get(successKey, written));
    }
  }

  private Path exportWithOverwriteQuestion(Function<ExportCommand, Path> export, Path target) {
    try {
      return export.apply(command(target, false));
    } catch (ApplicationException e) {
      if (e.getCode() != ErrorCode.FILE_ALREADY_EXISTS) {
        throw e;
      }
      boolean overwrite =
          prompts.readConfirmation(MessageKeys.PROMPT_CONFIRM_OVERWRITE, e.getArgs().get(0));
      if (!overwrite) {
        io.println(messages.get(MessageKeys.DIALOG_CANCELLED));
        return null;
      }
      return export.apply(command(target, true));
    }
  }

  private ExportCommand command(Path target, boolean overwrite) {
    return commandMapper.toCommand(session.login(), new ExportForm(target, overwrite));
  }
}
