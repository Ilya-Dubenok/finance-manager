package org.example.finmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.example.finmanager.RefusalAssert;
import org.example.finmanager.application.dto.command.CreateCategoryCommand;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.result.CategoryViewDto;
import org.example.finmanager.application.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

public class CategoryServiceTest {

  private static final String LOGIN = "alice";
  private static final String FOOD = "Food";

  private final FakeWalletPort wallets = new FakeWalletPort();
  private final CategoryService service = new CategoryService(wallets);

  @Test
  public void createsCategoryAndStoresItInTheWallet() {
    CategoryDto created = create(OperationType.EXPENSE, FOOD);

    assertThat(created).isEqualTo(new CategoryDto(FOOD, OperationType.EXPENSE));
    assertThat(wallets.get(LOGIN).categories()).contains(created);
  }

  @Test
  public void trimsNameAndKeepsEnteredCase() {
    CategoryDto created = create(OperationType.INCOME, "  sAlArY  ");

    assertThat(created.name()).isEqualTo("sAlArY");
  }

  @Test
  public void sameNameIsAllowedInTheOtherType() {
    create(OperationType.EXPENSE, FOOD);

    CategoryDto income = create(OperationType.INCOME, FOOD);

    assertThat(income.type()).isEqualTo(OperationType.INCOME);
  }

  @Test
  public void rejectsDuplicateInTheSameTypeIgnoringCase() {
    create(OperationType.EXPENSE, FOOD);

    RefusalAssert.assertRefusalWithArgs(
        () -> create(OperationType.EXPENSE, " fOOD "),
        ErrorCode.CATEGORY_ALREADY_EXISTS,
        "fOOD",
        OperationType.EXPENSE);
  }

  @ParameterizedTest
  @EnumSource(OperationType.class)
  public void rejectsSystemCategoryNameInAnyCase(OperationType type) {
    RefusalAssert.assertRefusal(() -> create(type, "transfers"), ErrorCode.SYSTEM_CATEGORY);
    RefusalAssert.assertRefusal(() -> create(type, "TRANSFERS"), ErrorCode.SYSTEM_CATEGORY);
  }

  @ParameterizedTest
  @ValueSource(strings = {"cancel", "CANCEL", " Cancel "})
  public void rejectsReservedWord(String name) {
    RefusalAssert.assertRefusalWithArgs(
        () -> create(OperationType.EXPENSE, name), ErrorCode.RESERVED_NAME, name.trim());
  }

  @Test
  public void rejectsBlankName() {
    RefusalAssert.assertRefusal(() -> create(OperationType.EXPENSE, "   "), ErrorCode.EMPTY_VALUE);
  }

  @Test
  public void rejectsTooLongName() {
    RefusalAssert.assertRefusalWithArgs(
        () -> create(OperationType.EXPENSE, "n".repeat(51)), ErrorCode.VALUE_TOO_LONG, 50);
  }

  @Test
  public void acceptsNameOfMaximumLength() {
    CategoryDto created = create(OperationType.EXPENSE, "n".repeat(50));

    assertThat(created.name()).hasSize(50);
  }

  @ParameterizedTest
  @ValueSource(strings = {"a,b", "a\tb"})
  public void rejectsCommaAndControlCharacters(String name) {
    RefusalAssert.assertRefusal(
        () -> create(OperationType.EXPENSE, name), ErrorCode.NAME_INVALID_CHARS);
  }

  @Test
  public void rejectedCategoryDoesNotChangeTheWallet() {
    List<CategoryDto> before = wallets.get(LOGIN).categories();

    RefusalAssert.assertRefusal(
        () -> create(OperationType.EXPENSE, "a,b"), ErrorCode.NAME_INVALID_CHARS);

    assertThat(wallets.get(LOGIN).categories()).isEqualTo(before);
  }

  @Test
  public void listsAllCategoriesAlphabeticallyIgnoringCaseAndMarksSystemOnes() {
    create(OperationType.EXPENSE, "banana");
    create(OperationType.INCOME, "Apple");

    List<CategoryViewDto> categories = service.listCategories(LOGIN);

    assertThat(categories)
        .extracting(CategoryViewDto::name)
        .containsExactly("Apple", "banana", "Transfers", "Transfers");
    assertThat(categories)
        .extracting(CategoryViewDto::system)
        .containsExactly(false, false, true, true);
  }

  @Test
  public void newWalletListsOnlySystemCategories() {
    List<CategoryViewDto> categories = service.listCategories("bob");

    assertThat(categories).hasSize(2).allMatch(CategoryViewDto::system);
  }

  @Test
  public void listShowsBudgetLimitsOnlyForExpenseCategoriesThatHaveOne() {
    wallets.save(FrExample.wallet());

    List<CategoryViewDto> categories = service.listCategories(LOGIN);

    assertThat(limitOf(categories, "Food", OperationType.EXPENSE)).isEqualByComparingTo("4000");
    assertThat(limitOf(categories, "Taxi", OperationType.EXPENSE)).isNull();
    assertThat(limitOf(categories, "Salary", OperationType.INCOME)).isNull();
  }

  private BigDecimal limitOf(List<CategoryViewDto> categories, String name, OperationType type) {
    return categories.stream()
        .filter(category -> category.name().equals(name) && category.type() == type)
        .findFirst()
        .orElseThrow()
        .budgetLimit();
  }

  private CategoryDto create(OperationType type, String name) {
    return service.createCategory(new CreateCategoryCommand(LOGIN, type, name));
  }
}
