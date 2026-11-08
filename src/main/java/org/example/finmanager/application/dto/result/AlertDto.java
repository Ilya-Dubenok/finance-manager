package org.example.finmanager.application.dto.result;

/**
 * Оповещение о состоянии кошелька. Тексты строит CLI по типу оповещения (D-39).
 *
 * <p>Два правила (D-07): превышен лимит бюджета категории (A-1) и расходы больше доходов (A-2).
 */
public sealed interface AlertDto permits BudgetExceededAlert, ExpensesExceedIncomeAlert {}
