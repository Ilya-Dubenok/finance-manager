package org.example.finmanager.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finmanager.application.dto.command.CreateCategoryCommand;
import org.example.finmanager.application.dto.command.DeleteCategoryCommand;
import org.example.finmanager.application.dto.command.RenameCategoryCommand;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.CategoryViewDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.example.finmanager.application.port.inbound.CategoryPort;
import org.example.finmanager.application.port.inbound.WalletPort;
import org.springframework.stereotype.Service;

/** Категории кошелька: создание, переименование, удаление, список. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService implements CategoryPort {

  private final WalletPort wallets;

  @Override
  public CategoryDto createCategory(CreateCategoryCommand command) {
    String name = validNewName(command.name());
    WalletDto wallet = wallets.get(command.login());
    if (WalletRules.findCategory(wallet, command.type(), name).isPresent()) {
      throw new ApplicationException(ErrorCode.CATEGORY_ALREADY_EXISTS, name, command.type());
    }
    CategoryDto category = new CategoryDto(name, command.type());
    List<CategoryDto> categories = new ArrayList<>(wallet.categories());
    categories.add(category);
    wallets.save(wallet.toBuilder().categories(categories).build());
    log.info("Created category '{}' ({}) for '{}'", name, command.type(), command.login());
    return category;
  }

  @Override
  public CategoryDto renameCategory(RenameCategoryCommand command) {
    WalletDto wallet = wallets.get(command.login());
    CategoryDto category = editableCategory(wallet, command.type(), command.name());
    String newName = validNewName(command.newName());
    boolean takenByAnother =
        WalletRules.findCategory(wallet, command.type(), newName)
            .filter(existing -> !existing.name().equalsIgnoreCase(category.name()))
            .isPresent();
    if (takenByAnother) {
      throw new ApplicationException(ErrorCode.CATEGORY_ALREADY_EXISTS, newName, command.type());
    }

    CategoryDto renamed = new CategoryDto(newName, command.type());
    wallets.save(renamedWallet(wallet, category, renamed));
    log.info(
        "Renamed category '{}' ({}) to '{}' for '{}'",
        category.name(),
        command.type(),
        newName,
        command.login());
    return renamed;
  }

  @Override
  public void deleteCategory(DeleteCategoryCommand command) {
    WalletDto wallet = wallets.get(command.login());
    CategoryDto category = editableCategory(wallet, command.type(), command.name());
    long operations = countOperations(wallet, category);
    if (operations > 0) {
      throw new ApplicationException(
          ErrorCode.CATEGORY_HAS_OPERATIONS, category.name(), operations);
    }
    List<CategoryDto> categories = new ArrayList<>(wallet.categories());
    categories.remove(category);
    WalletDto.WalletDtoBuilder updated = wallet.toBuilder().categories(categories);
    if (category.type() == OperationType.EXPENSE) {
      List<BudgetDto> budgets = new ArrayList<>(wallet.budgets());
      budgets.removeIf(budget -> budget.category().equalsIgnoreCase(category.name()));
      updated.budgets(budgets);
    }
    wallets.save(updated.build());
    log.info(
        "Deleted category '{}' ({}) of '{}'", category.name(), category.type(), command.login());
  }

  @Override
  public List<CategoryViewDto> listCategories(String login) {
    WalletDto wallet = wallets.get(login);
    return wallet.categories().stream()
        .sorted(Comparator.comparing(CategoryDto::name, String.CASE_INSENSITIVE_ORDER))
        .map(category -> view(wallet, category))
        .toList();
  }

  private String validNewName(String rawName) {
    String name = rawName == null ? null : rawName.trim();
    InputRules.requireCategoryName(name);
    if (WalletRules.isSystemName(name)) {
      throw new ApplicationException(ErrorCode.SYSTEM_CATEGORY);
    }
    if (WalletRules.CANCEL_WORD.equalsIgnoreCase(name)) {
      throw new ApplicationException(ErrorCode.RESERVED_NAME, name);
    }
    return name;
  }

  private CategoryDto editableCategory(WalletDto wallet, OperationType type, String rawName) {
    InputRules.requireNotBlank(rawName);
    String name = rawName.trim();
    CategoryDto category =
        WalletRules.findCategory(wallet, type, name)
            .orElseThrow(() -> WalletRules.categoryNotFound(wallet, type, name));
    if (WalletRules.isSystemName(category.name())) {
      throw new ApplicationException(ErrorCode.SYSTEM_CATEGORY);
    }
    return category;
  }

  private long countOperations(WalletDto wallet, CategoryDto category) {
    return wallet.operations().stream().filter(operation -> belongsTo(operation, category)).count();
  }

  private boolean belongsTo(OperationDto operation, CategoryDto category) {
    return operation.type() == category.type()
        && operation.category().equalsIgnoreCase(category.name());
  }

  private WalletDto renamedWallet(WalletDto wallet, CategoryDto old, CategoryDto renamed) {
    List<CategoryDto> categories =
        wallet.categories().stream().map(c -> c.equals(old) ? renamed : c).toList();
    List<OperationDto> operations =
        wallet.operations().stream().map(o -> renamedOperation(o, old, renamed)).toList();
    List<BudgetDto> budgets =
        wallet.budgets().stream().map(b -> renamedBudget(b, old, renamed)).toList();
    return wallet.toBuilder()
        .categories(categories)
        .operations(operations)
        .budgets(budgets)
        .build();
  }

  private OperationDto renamedOperation(
      OperationDto operation, CategoryDto old, CategoryDto renamed) {
    if (!belongsTo(operation, old)) {
      return operation;
    }
    return new OperationDto(
        operation.type(),
        renamed.name(),
        operation.amount(),
        operation.date(),
        operation.comment(),
        operation.counterparty());
  }

  private BudgetDto renamedBudget(BudgetDto budget, CategoryDto old, CategoryDto renamed) {
    boolean ofCategory =
        old.type() == OperationType.EXPENSE && budget.category().equalsIgnoreCase(old.name());
    return ofCategory ? new BudgetDto(renamed.name(), budget.limit()) : budget;
  }

  private CategoryViewDto view(WalletDto wallet, CategoryDto category) {
    boolean system = WalletRules.isSystemName(category.name());
    BigDecimal limit = budgetLimit(wallet, category);
    return new CategoryViewDto(category.name(), category.type(), system, limit);
  }

  private BigDecimal budgetLimit(WalletDto wallet, CategoryDto category) {
    if (category.type() != OperationType.EXPENSE) {
      return null;
    }
    return WalletRules.findBudget(wallet, category.name()).map(BudgetDto::limit).orElse(null);
  }
}
