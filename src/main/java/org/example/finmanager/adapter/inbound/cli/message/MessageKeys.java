package org.example.finmanager.adapter.inbound.cli.message;

import lombok.experimental.UtilityClass;

/**
 * Ключи сообщений из {@code messages_en.properties}.
 *
 * <p>Сообщения об отказах сервисов ({@code B-xxx}, {@code S-xxx}) здесь не перечислены: их ключи
 * приходят из {@code ErrorCode}.
 */
@UtilityClass
public class MessageKeys {

  /** Прощание при выходе из приложения. */
  public static final String APP_GOODBYE = "app.goodbye";

  /** Приглашение выбрать пункт меню. */
  public static final String MENU_PROMPT = "menu.prompt";

  /** Подпись пункта «Выход» в меню до входа. */
  public static final String MENU_EXIT = "menu.exit";

  /** Заголовок меню до входа в аккаунт. */
  public static final String MENU_GUEST_TITLE = "menu.guest.title";

  /** Подпись пункта «Войти». */
  public static final String MENU_GUEST_LOGIN = "menu.guest.login";

  /** Подпись пункта «Справка» в меню до входа. */
  public static final String MENU_GUEST_HELP = "menu.guest.help";

  /** Заголовок главного меню; подставляется логин. */
  public static final String MENU_MAIN_TITLE = "menu.main.title";

  /** Подпись пункта «Справка» в главном меню. */
  public static final String MENU_MAIN_HELP = "menu.main.help";

  /** Подпись пункта «Выйти из аккаунта». */
  public static final String MENU_MAIN_LOGOUT = "menu.main.logout";

  /** Подпись пункта «Выход из приложения». */
  public static final String MENU_MAIN_EXIT = "menu.main.exit";

  /** Подпись пункта «Операции» главного меню. */
  public static final String MENU_MAIN_OPERATIONS = "menu.main.operations";

  /** Подпись пункта «Категории и бюджеты» главного меню. */
  public static final String MENU_MAIN_CATEGORIES = "menu.main.categories";

  /** Подпись пункта «Назад» во вложенных меню. */
  public static final String MENU_BACK = "menu.back";

  /** Подпись пункта «Статистика» главного меню. */
  public static final String MENU_MAIN_STATISTICS = "menu.main.statistics";

  /** Подпись пункта «Перевод» главного меню. */
  public static final String MENU_MAIN_TRANSFER = "menu.main.transfer";

  /** Подпись пункта «Отчёты» главного меню. */
  public static final String MENU_MAIN_REPORTS = "menu.main.reports";

  /** Заголовок меню отчётов. */
  public static final String MENU_REPORTS_TITLE = "menu.reports.title";

  /** Подпись пункта «Экспорт JSON-снимка». */
  public static final String MENU_REPORTS_EXPORT_JSON = "menu.reports.export-json";

  /** Подпись пункта «Экспорт операций в CSV». */
  public static final String MENU_REPORTS_EXPORT_CSV = "menu.reports.export-csv";

  /** Подпись пункта «Импорт JSON-снимка». */
  public static final String MENU_REPORTS_IMPORT = "menu.reports.import";

  /** Заголовок меню операций. */
  public static final String MENU_OPERATIONS_TITLE = "menu.operations.title";

  /** Подпись пункта «Добавить доход». */
  public static final String MENU_OPERATIONS_ADD_INCOME = "menu.operations.add-income";

  /** Подпись пункта «Добавить расход». */
  public static final String MENU_OPERATIONS_ADD_EXPENSE = "menu.operations.add-expense";

  /** Подпись пункта «Список операций». */
  public static final String MENU_OPERATIONS_LIST = "menu.operations.list";

  /** Заголовок меню категорий и бюджетов. */
  public static final String MENU_CATEGORIES_TITLE = "menu.categories.title";

  /** Подпись пункта «Создать категорию». */
  public static final String MENU_CATEGORIES_CREATE = "menu.categories.create";

  /** Подпись пункта «Список категорий». */
  public static final String MENU_CATEGORIES_LIST = "menu.categories.list";

  /** Подпись пункта «Переименовать категорию». */
  public static final String MENU_CATEGORIES_RENAME = "menu.categories.rename";

  /** Подпись пункта «Удалить категорию». */
  public static final String MENU_CATEGORIES_DELETE = "menu.categories.delete";

  /** Подпись пункта «Установить или изменить бюджет». */
  public static final String MENU_CATEGORIES_SET_BUDGET = "menu.categories.set-budget";

  /** Подпись пункта «Удалить бюджет». */
  public static final String MENU_CATEGORIES_REMOVE_BUDGET = "menu.categories.remove-budget";

  /** Заголовок меню статистики. */
  public static final String MENU_STATISTICS_TITLE = "menu.statistics.title";

  /** Подпись пункта «Сводка». */
  public static final String MENU_STATISTICS_SUMMARY = "menu.statistics.summary";

  /** Подпись пункта «Подсчёт по выбранным категориям». */
  public static final String MENU_STATISTICS_TOTALS = "menu.statistics.totals";

  /** Подсказка в начале диалога входа. */
  public static final String LOGIN_HINT = "login.hint";

  /** Вопрос о логине. */
  public static final String PROMPT_LOGIN = "prompt.login";

  /** Вопрос о пароле. */
  public static final String PROMPT_PASSWORD = "prompt.password";

  /** Вопрос о типе: доход или расход. */
  public static final String PROMPT_TYPE = "prompt.type";

  /** Вопрос о названии категории операции. */
  public static final String PROMPT_CATEGORY = "prompt.category";

  /** Вопрос о названии создаваемой категории. */
  public static final String PROMPT_CATEGORY_NAME = "prompt.category-name";

  /** Вопрос о логине получателя перевода. */
  public static final String PROMPT_RECIPIENT = "prompt.recipient";

  /** Сообщение о выполненном переводе. */
  public static final String TRANSFER_DONE = "transfer.done";

  /** Вопрос о новом названии категории. */
  public static final String PROMPT_NEW_CATEGORY_NAME = "prompt.new-category-name";

  /** Вопрос о подтверждении удаления категории; подставляются название и тип. */
  public static final String PROMPT_CONFIRM_DELETE_CATEGORY = "prompt.confirm-delete-category";

  /** Сообщение о переименованной категории. */
  public static final String CATEGORY_RENAMED = "category.renamed";

  /** Сообщение об удалённой категории. */
  public static final String CATEGORY_DELETED = "category.deleted";

  /** Вопрос о пути к файлу экспорта. */
  public static final String PROMPT_EXPORT_PATH = "prompt.export-path";

  /** Вопрос о пути к файлу импорта. */
  public static final String PROMPT_IMPORT_PATH = "prompt.import-path";

  /** Вопрос о перезаписи существующего файла; подставляется путь. */
  public static final String PROMPT_CONFIRM_OVERWRITE = "prompt.confirm-overwrite";

  /** Вопрос о замене кошелька содержимым снимка. */
  public static final String PROMPT_CONFIRM_IMPORT = "prompt.confirm-import";

  /** Сообщение об экспортированном снимке; подставляется путь. */
  public static final String EXPORT_SNAPSHOT_DONE = "export.snapshot-done";

  /** Сообщение об экспортированных операциях; подставляется путь. */
  public static final String EXPORT_CSV_DONE = "export.csv-done";

  /** Сообщение об импорте: число категорий, бюджетов и операций. */
  public static final String IMPORT_DONE = "import.done";

  /** Уведомление: владелец снимка не совпадает с текущим пользователем. */
  public static final String IMPORT_OWNER_MISMATCH = "import.owner-mismatch";

  /** Подсказка перед вопросом о категории: названия категорий нужного типа. */
  public static final String HINT_CATEGORIES = "hint.categories";

  /** Вопрос о сумме. */
  public static final String PROMPT_AMOUNT = "prompt.amount";

  /** Вопрос о дате операции. */
  public static final String PROMPT_DATE = "prompt.date";

  /** Вопрос о комментарии. */
  public static final String PROMPT_COMMENT = "prompt.comment";

  /** Вопрос о начале периода. */
  public static final String PROMPT_PERIOD_FROM = "prompt.period-from";

  /** Вопрос о конце периода. */
  public static final String PROMPT_PERIOD_TO = "prompt.period-to";

  /** Вопрос о лимите бюджета. */
  public static final String PROMPT_LIMIT = "prompt.limit";

  /** Вопрос о названиях категорий через запятую. */
  public static final String PROMPT_CATEGORIES = "prompt.categories";

  /** Вопрос о подтверждении удаления бюджета; подставляется название категории. */
  public static final String PROMPT_CONFIRM_REMOVE_BUDGET = "prompt.confirm-remove-budget";

  /** Сообщение о добавленном доходе. */
  public static final String OPERATION_INCOME_ADDED = "operation.income-added";

  /** Сообщение о добавленном расходе. */
  public static final String OPERATION_EXPENSE_ADDED = "operation.expense-added";

  /** Сообщение о созданной категории. */
  public static final String CATEGORY_CREATED = "category.created";

  /** Пометка системной категории в списке. */
  public static final String CATEGORY_SYSTEM_LABEL = "category.system-label";

  /** Сообщение об установленном бюджете. */
  public static final String BUDGET_SET = "budget.set";

  /** Сообщение об удалённом бюджете. */
  public static final String BUDGET_REMOVED = "budget.removed";

  /** Оповещение A-1: превышен лимит бюджета категории. */
  public static final String ALERT_BUDGET_EXCEEDED = "alert.budget-exceeded";

  /** Оповещение A-2: расходы больше доходов. */
  public static final String ALERT_EXPENSES_EXCEED_INCOME = "alert.expenses-exceed-income";

  /** Строка «Общий доход» сводки. */
  public static final String SUMMARY_TOTAL_INCOME = "summary.total-income";

  /** Строка «Общие расходы» сводки. */
  public static final String SUMMARY_TOTAL_EXPENSES = "summary.total-expenses";

  /** Строка «Баланс» сводки. */
  public static final String SUMMARY_BALANCE = "summary.balance";

  /** Заголовок блока доходов по категориям. */
  public static final String SUMMARY_INCOME_TITLE = "summary.income-title";

  /** Заголовок блока расходов по категориям. */
  public static final String SUMMARY_EXPENSES_TITLE = "summary.expenses-title";

  /** Заголовок блока бюджетов. */
  public static final String SUMMARY_BUDGETS_TITLE = "summary.budgets-title";

  /** Строка периода с двумя границами. */
  public static final String SUMMARY_PERIOD = "summary.period";

  /** Строка периода «за всё время». */
  public static final String TOTALS_PERIOD_ALL = "totals.period-all";

  /** Итог доходов по выбранным категориям. */
  public static final String TOTALS_INCOME = "totals.income";

  /** Итог расходов по выбранным категориям. */
  public static final String TOTALS_EXPENSES = "totals.expenses";

  /** Заголовок таблицы категорий доходов. */
  public static final String CATEGORIES_INCOME_TITLE = "categories.income-title";

  /** Заголовок таблицы категорий расходов. */
  public static final String CATEGORIES_EXPENSE_TITLE = "categories.expense-title";

  /** Заголовок колонки с номером строки. */
  public static final String TABLE_NUMBER = "table.number";

  /** Заголовок колонки «Дата». */
  public static final String TABLE_DATE = "table.date";

  /** Заголовок колонки «Тип». */
  public static final String TABLE_TYPE = "table.type";

  /** Заголовок колонки «Категория». */
  public static final String TABLE_CATEGORY = "table.category";

  /** Заголовок колонки «Сумма». */
  public static final String TABLE_AMOUNT = "table.amount";

  /** Заголовок колонки «Контрагент». */
  public static final String TABLE_COUNTERPARTY = "table.counterparty";

  /** Заголовок колонки «Комментарий». */
  public static final String TABLE_COMMENT = "table.comment";

  /** Заголовок колонки «Бюджет». */
  public static final String TABLE_BUDGET = "table.budget";

  /** Заголовок колонки «Лимит». */
  public static final String TABLE_LIMIT = "table.limit";

  /** Заголовок колонки «Потрачено». */
  public static final String TABLE_SPENT = "table.spent";

  /** Заголовок колонки «Оставшийся бюджет». */
  public static final String TABLE_REMAINING = "table.remaining";

  /** Слово, которым пользователь отменяет диалог. */
  public static final String DIALOG_CANCEL_WORD = "dialog.cancel-word";

  /** Сообщение об отмене диалога. */
  public static final String DIALOG_CANCELLED = "dialog.cancelled";

  /** Текст справки. */
  public static final String HELP_TEXT = "help.text";

  /** Ошибка: пустой ввод. */
  public static final String ERROR_EMPTY_INPUT = "E-001";

  /** Ошибка: в меню нет такого пункта. */
  public static final String ERROR_UNKNOWN_MENU_ITEM = "E-002";

  /** Ошибка: недопустимый путь к файлу. */
  public static final String ERROR_INVALID_PATH = "E-014";

  /** Ошибка: сумма не распознана. */
  public static final String ERROR_UNKNOWN_AMOUNT = "E-003";

  /** Ошибка: дата не распознана. */
  public static final String ERROR_UNKNOWN_DATE = "E-007";

  /** Ошибка: ответ не «yes» и не «no». */
  public static final String ERROR_UNKNOWN_CONFIRMATION = "E-012";

  /** Предупреждение: часть названных категорий не найдена. */
  public static final String WARNING_CATEGORIES_NOT_FOUND = "B-114";

  /** Предупреждение: ни одна из названных категорий не найдена. */
  public static final String WARNING_NO_CATEGORY_FOUND = "B-114.none";

  /** Ошибка: тип не 1 и не 2. */
  public static final String ERROR_UNKNOWN_TYPE = "E-013";

  /** Сообщение: операций за период нет. */
  public static final String INFO_NO_OPERATIONS = "B-113";

  /** Сообщение: данные не удалось записать при закрытии приложения. */
  public static final String SHUTDOWN_SAVE_FAILED = "shutdown.save-failed";

  /** Сообщение о непредвиденной ошибке. */
  public static final String ERROR_INTERNAL = "error.internal";
}
