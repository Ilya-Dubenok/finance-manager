package org.example.finmanager.adapter.inbound.cli.menu;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.action.CreateCategoryAction;
import org.example.finmanager.adapter.inbound.cli.action.DeleteCategoryAction;
import org.example.finmanager.adapter.inbound.cli.action.ListCategoriesAction;
import org.example.finmanager.adapter.inbound.cli.action.RemoveBudgetAction;
import org.example.finmanager.adapter.inbound.cli.action.RenameCategoryAction;
import org.example.finmanager.adapter.inbound.cli.action.SetBudgetAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.Menu;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuItem;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.springframework.stereotype.Component;

/**
 * Меню категорий и бюджетов.
 *
 * <p>Номера пунктов — из спецификации.
 */
@Component
@RequiredArgsConstructor
public class CategoriesMenu {

  private static final int CREATE_NUMBER = 1;
  private static final int LIST_NUMBER = 2;
  private static final int RENAME_NUMBER = 3;
  private static final int DELETE_NUMBER = 4;
  private static final int SET_BUDGET_NUMBER = 5;
  private static final int REMOVE_BUDGET_NUMBER = 6;

  private final MessageCatalog messages;
  private final CreateCategoryAction createCategoryAction;
  private final ListCategoriesAction listCategoriesAction;
  private final RenameCategoryAction renameCategoryAction;
  private final DeleteCategoryAction deleteCategoryAction;
  private final SetBudgetAction setBudgetAction;
  private final RemoveBudgetAction removeBudgetAction;

  /** Строит меню. */
  public Menu build() {
    String title = messages.get(MessageKeys.MENU_CATEGORIES_TITLE);
    String createLabel = messages.get(MessageKeys.MENU_CATEGORIES_CREATE);
    MenuItem create = new MenuItem(CREATE_NUMBER, createLabel, createCategoryAction);
    String listLabel = messages.get(MessageKeys.MENU_CATEGORIES_LIST);
    MenuItem list = new MenuItem(LIST_NUMBER, listLabel, listCategoriesAction);
    String renameLabel = messages.get(MessageKeys.MENU_CATEGORIES_RENAME);
    MenuItem rename = new MenuItem(RENAME_NUMBER, renameLabel, renameCategoryAction);
    String deleteLabel = messages.get(MessageKeys.MENU_CATEGORIES_DELETE);
    MenuItem delete = new MenuItem(DELETE_NUMBER, deleteLabel, deleteCategoryAction);
    String setLabel = messages.get(MessageKeys.MENU_CATEGORIES_SET_BUDGET);
    MenuItem setBudget = new MenuItem(SET_BUDGET_NUMBER, setLabel, setBudgetAction);
    String removeLabel = messages.get(MessageKeys.MENU_CATEGORIES_REMOVE_BUDGET);
    MenuItem removeBudget = new MenuItem(REMOVE_BUDGET_NUMBER, removeLabel, removeBudgetAction);
    MenuItem back = MenuItem.leave(messages.get(MessageKeys.MENU_BACK));
    return new Menu(title, List.of(create, list, rename, delete, setBudget, removeBudget, back));
  }
}
