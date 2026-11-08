package org.example.finmanager.adapter.inbound.cli.menu;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.action.HelpAction;
import org.example.finmanager.adapter.inbound.cli.action.LoginAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.Menu;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuItem;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.springframework.stereotype.Component;

/** Меню до входа в аккаунт. */
@Component
@RequiredArgsConstructor
public class GuestMenu {

  private static final int LOGIN_NUMBER = 1;
  private static final int HELP_NUMBER = 2;

  private final MessageCatalog messages;
  private final LoginAction loginAction;
  private final HelpAction helpAction;

  /** Строит меню: войти, справка, выход. */
  public Menu build() {
    String title = messages.get(MessageKeys.MENU_GUEST_TITLE);
    String loginLabel = messages.get(MessageKeys.MENU_GUEST_LOGIN);
    MenuItem login = new MenuItem(LOGIN_NUMBER, loginLabel, loginAction);
    String helpLabel = messages.get(MessageKeys.MENU_GUEST_HELP);
    MenuItem help = new MenuItem(HELP_NUMBER, helpLabel, helpAction);
    MenuItem exit = MenuItem.leave(messages.get(MessageKeys.MENU_EXIT));
    return new Menu(title, List.of(login, help, exit));
  }
}
