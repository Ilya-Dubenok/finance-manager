package org.example.finmanager.adapter.inbound.cli.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.MissingResourceException;
import org.example.finmanager.adapter.inbound.cli.TestConstants;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.junit.jupiter.api.Test;

public class MessageCatalogTest {

  private static final String UNKNOWN_KEY = "no-such-key";

  private final MessageCatalog messages = new MessageCatalog();

  @Test
  public void readsTextFromBundle() {
    assertThat(messages.get(MessageKeys.APP_GOODBYE)).isEqualTo(TestConstants.GOODBYE);
  }

  @Test
  public void substitutesArguments() {
    String message = messages.get(MessageKeys.ERROR_UNKNOWN_MENU_ITEM, "x", "7");

    assertThat(message).isEqualTo("No such option: x. Enter a number from 0 to 7.");
  }

  @Test
  public void formatsNumbersAsPlainTextWithoutGrouping() {
    String message = messages.get(MessageKeys.ERROR_UNKNOWN_MENU_ITEM, "x", 1234567);

    assertThat(message).isEqualTo("No such option: x. Enter a number from 0 to 1234567.");
  }

  @Test
  public void formatsMoneyDatesTypesAndLists() {
    String message =
        messages.get(
            MessageKeys.OPERATION_INCOME_ADDED,
            "Salary",
            new BigDecimal("60000"),
            LocalDate.of(2026, 11, 8));

    assertThat(message).isEqualTo("Income added: Salary, 60,000.00 (08.11.2026).");
    assertThat(messages.get(MessageKeys.CATEGORY_CREATED, "Food", OperationType.EXPENSE))
        .isEqualTo("Category \"Food\" (expense) created.");
    assertThat(messages.get("B-105", "Taxi", List.of("Food", "Salary")))
        .isEqualTo("Category \"Taxi\" not found. Available categories: Food, Salary.");
  }

  @Test
  public void describesRefusalByItsCatalogId() {
    ApplicationException refusal =
        new ApplicationException(ErrorCode.CATEGORY_ALREADY_EXISTS, "Food", OperationType.INCOME);

    assertThat(messages.describe(refusal)).isEqualTo("Category \"Food\" (income) already exists.");
  }

  @Test
  public void failsOnUnknownKey() {
    assertThatThrownBy(() -> messages.get(UNKNOWN_KEY))
        .isInstanceOf(MissingResourceException.class);
  }

  @Test
  public void everyMessageKeyHasTextInTheCatalog() throws IllegalAccessException {
    for (Field field : MessageKeys.class.getFields()) {
      String key = (String) field.get(null);
      assertThat(messages.get(key, "a", "b", "c", "d", "e")).as(key).isNotBlank();
    }
  }

  @Test
  public void everyErrorCodeHasMessageInTheCatalog() {
    for (ErrorCode code : ErrorCode.values()) {
      assertThat(messages.get(code.getCatalogId(), "a", "b", "c", "d")).isNotBlank();
    }
  }
}
