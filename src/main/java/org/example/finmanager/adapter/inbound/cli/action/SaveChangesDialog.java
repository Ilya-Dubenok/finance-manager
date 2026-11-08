package org.example.finmanager.adapter.inbound.cli.action;

import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.input.PromptReader;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.example.finmanager.application.port.inbound.WalletPort;
import org.springframework.stereotype.Component;

/**
 * Записывает изменённые кошельки при выходе из аккаунта и из приложения. Если запись не удалась,
 * показывает причину и спрашивает: повторить или выйти без сохранения (B-116).
 */
@Component
@RequiredArgsConstructor
public class SaveChangesDialog {

  private final WalletPort wallets;
  private final PromptReader prompts;

  /**
   * Сохраняет данные; возвращает управление после успешной записи или отказа повторять.
   *
   * @throws org.example.finmanager.adapter.inbound.cli.input.DialogCancelledException если
   *     вместо ответа введено слово отмены, тогда выход не происходит
   */
  public void run() {
    boolean finished = false;
    while (!finished) {
      try {
        wallets.saveChanges();
        finished = true;
      } catch (ApplicationException e) {
        if (e.getCode() != ErrorCode.SAVE_FAILED) {
          throw e;
        }
        boolean retry =
            prompts.readConfirmation(e.getCode().getCatalogId(), e.getArgs().toArray());
        finished = !retry;
      }
    }
  }
}
