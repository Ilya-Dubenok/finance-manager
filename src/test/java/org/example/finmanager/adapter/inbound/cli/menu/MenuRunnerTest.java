package org.example.finmanager.adapter.inbound.cli.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.example.finmanager.adapter.inbound.cli.TestConstants;
import org.example.finmanager.adapter.inbound.cli.input.DialogCancelledException;
import org.example.finmanager.adapter.inbound.cli.io.InputClosedException;
import org.example.finmanager.adapter.inbound.cli.io.ScriptedConsoleIo;
import org.example.finmanager.adapter.inbound.cli.menu.model.Menu;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuItem;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.junit.jupiter.api.Test;

public class MenuRunnerTest {

  private static final String MENU_TITLE = "=== Test ===";
  private static final String ACTION_LABEL = "Action";
  private static final String LEAVE_LABEL = "Back";
  private static final int ACTION_NUMBER = 1;
  private static final String CHOICE_ACTION = Integer.toString(ACTION_NUMBER);

  private final MessageCatalog messages = new MessageCatalog();
  private final AtomicInteger actionCalls = new AtomicInteger();

  @Test
  public void closesMenuWhenLeaveItemChosen() {
    ScriptedConsoleIo io = new ScriptedConsoleIo(TestConstants.CHOICE_LEAVE);

    MenuOutcome outcome = new MenuRunner(io, messages).run(this::countingMenu);

    assertThat(outcome).isEqualTo(MenuOutcome.LEAVE);
    assertThat(actionCalls).hasValue(0);
  }

  @Test
  public void showsTitleAndNumberedItems() {
    ScriptedConsoleIo io = new ScriptedConsoleIo(TestConstants.CHOICE_LEAVE);

    new MenuRunner(io, messages).run(this::countingMenu);

    assertThat(io.output())
        .contains(MENU_TITLE)
        .contains(CHOICE_ACTION + ". " + ACTION_LABEL)
        .contains(TestConstants.CHOICE_LEAVE + ". " + LEAVE_LABEL)
        .contains("Choose an option: ");
  }

  @Test
  public void runsActionAndShowsMenuAgainWhenActionStays() {
    ScriptedConsoleIo io =
        new ScriptedConsoleIo(CHOICE_ACTION, CHOICE_ACTION, TestConstants.CHOICE_LEAVE);

    new MenuRunner(io, messages).run(this::countingMenu);

    assertThat(actionCalls).hasValue(2);
  }

  @Test
  public void reportsUnknownNumberAndAsksAgain() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("9", TestConstants.CHOICE_LEAVE);

    new MenuRunner(io, messages).run(this::countingMenu);

    assertThat(io.output()).contains("No such option: 9. Enter a number from 0 to 1.");
    assertThat(actionCalls).hasValue(0);
  }

  @Test
  public void reportsNonNumericInput() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("abc", TestConstants.CHOICE_LEAVE);

    new MenuRunner(io, messages).run(this::countingMenu);

    assertThat(io.output()).contains("No such option: abc.");
  }

  @Test
  public void reportsEmptyInputAndTrimsSpaces() {
    ScriptedConsoleIo io =
        new ScriptedConsoleIo("   ", "  " + CHOICE_ACTION + "  ", TestConstants.CHOICE_LEAVE);

    new MenuRunner(io, messages).run(this::countingMenu);

    assertThat(io.output()).contains("The value must not be empty.");
    assertThat(actionCalls).hasValue(1);
  }

  @Test
  public void propagatesEndOfInput() {
    ScriptedConsoleIo io = new ScriptedConsoleIo();
    MenuRunner runner = new MenuRunner(io, messages);

    assertThatThrownBy(() -> runner.run(this::countingMenu))
        .isInstanceOf(InputClosedException.class);
  }

  @Test
  public void propagatesEndOfInputRaisedByAction() {
    ScriptedConsoleIo io = new ScriptedConsoleIo(CHOICE_ACTION);
    MenuAction closing =
        () -> {
          throw new InputClosedException();
        };
    MenuRunner runner = new MenuRunner(io, messages);

    assertThatThrownBy(() -> runner.run(() -> menuWith(closing)))
        .isInstanceOf(InputClosedException.class);
  }

  @Test
  public void showsRefusalMessageAndKeepsMenuWhenActionRefuses() {
    ScriptedConsoleIo io = new ScriptedConsoleIo(CHOICE_ACTION, TestConstants.CHOICE_LEAVE);
    MenuAction refusing =
        () -> {
          throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS);
        };

    new MenuRunner(io, messages).run(() -> menuWith(refusing));

    assertThat(io.output()).contains("Invalid login or password.");
    assertThat(occurrences(io.output(), MENU_TITLE)).isEqualTo(2);
  }

  @Test
  public void showsCancelledMessageAndKeepsMenuWhenDialogIsCancelled() {
    ScriptedConsoleIo io = new ScriptedConsoleIo(CHOICE_ACTION, TestConstants.CHOICE_LEAVE);
    MenuAction cancelling =
        () -> {
          throw new DialogCancelledException();
        };

    new MenuRunner(io, messages).run(() -> menuWith(cancelling));

    assertThat(io.output()).contains("Cancelled.");
    assertThat(occurrences(io.output(), MENU_TITLE)).isEqualTo(2);
  }

  @Test
  public void survivesUnexpectedExceptionAndShowsInternalError() {
    ScriptedConsoleIo io = new ScriptedConsoleIo(CHOICE_ACTION, TestConstants.CHOICE_LEAVE);
    MenuAction broken =
        () -> {
          throw new IllegalStateException("boom");
        };

    MenuOutcome outcome = new MenuRunner(io, messages).run(() -> menuWith(broken));

    assertThat(outcome).isEqualTo(MenuOutcome.LEAVE);
    assertThat(io.output())
        .contains("Internal error. See logs/finmanager.log for details.")
        .doesNotContain("boom");
    assertThat(occurrences(io.output(), MENU_TITLE)).isEqualTo(2);
  }

  @Test
  public void returnsExitWhenActionAsksToExit() {
    ScriptedConsoleIo io = new ScriptedConsoleIo(CHOICE_ACTION);

    MenuOutcome outcome = new MenuRunner(io, messages).run(() -> menuWith(() -> MenuOutcome.EXIT));

    assertThat(outcome).isEqualTo(MenuOutcome.EXIT);
  }

  @Test
  public void openReturnsStayAfterLeaveSoParentMenuStays() {
    ScriptedConsoleIo io = new ScriptedConsoleIo(TestConstants.CHOICE_LEAVE);

    MenuOutcome outcome = new MenuRunner(io, messages).open(this::countingMenu);

    assertThat(outcome).isEqualTo(MenuOutcome.STAY);
  }

  @Test
  public void openReturnsExitWhenUserLeavesTheApplication() {
    ScriptedConsoleIo io = new ScriptedConsoleIo(CHOICE_ACTION);

    MenuOutcome outcome = new MenuRunner(io, messages).open(() -> menuWith(() -> MenuOutcome.EXIT));

    assertThat(outcome).isEqualTo(MenuOutcome.EXIT);
  }

  @Test
  public void buildsMenuAgainBeforeEveryShow() {
    ScriptedConsoleIo io = new ScriptedConsoleIo("9", TestConstants.CHOICE_LEAVE);
    AtomicInteger builds = new AtomicInteger();
    Supplier<Menu> source =
        () -> {
          String title = MENU_TITLE + " " + builds.incrementAndGet();
          return new Menu(title, List.of(MenuItem.leave(LEAVE_LABEL)));
        };

    new MenuRunner(io, messages).run(source);

    assertThat(io.output()).contains(MENU_TITLE + " 1").contains(MENU_TITLE + " 2");
  }

  private Menu countingMenu() {
    MenuAction counting =
        () -> {
          actionCalls.incrementAndGet();
          return MenuOutcome.STAY;
        };
    return menuWith(counting);
  }

  private Menu menuWith(MenuAction action) {
    MenuItem item = new MenuItem(ACTION_NUMBER, ACTION_LABEL, action);
    return new Menu(MENU_TITLE, List.of(item, MenuItem.leave(LEAVE_LABEL)));
  }

  private int occurrences(String text, String part) {
    return text.split(Pattern.quote(part), -1).length - 1;
  }
}
