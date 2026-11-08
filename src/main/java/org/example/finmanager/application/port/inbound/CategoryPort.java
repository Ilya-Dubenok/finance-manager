package org.example.finmanager.application.port.inbound;

import java.util.List;
import org.example.finmanager.application.dto.command.CreateCategoryCommand;
import org.example.finmanager.application.dto.command.DeleteCategoryCommand;
import org.example.finmanager.application.dto.command.RenameCategoryCommand;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.result.CategoryViewDto;

/** Категории кошелька. */
public interface CategoryPort {

  /**
   * Создаёт категорию.
   *
   * @param command тип и название
   * @return созданная категория
   * @throws org.example.finmanager.application.exception.ApplicationException если название не
   *     проходит проверку, зарезервировано или такая категория уже есть
   */
  CategoryDto createCategory(CreateCategoryCommand command);

  /**
   * Переименовывает категорию: название меняется в самой категории, во всех её операциях и в её
   * бюджете. Переименование на то же название с другим регистром разрешено.
   *
   * @param command тип, текущее и новое название
   * @return категория с новым названием
   * @throws org.example.finmanager.application.exception.ApplicationException если категории нет,
   *     она системная, новое название не проходит проверку, зарезервировано или занято
   */
  CategoryDto renameCategory(RenameCategoryCommand command);

  /**
   * Удаляет категорию без операций вместе с её бюджетом.
   *
   * @param command тип и название
   * @throws org.example.finmanager.application.exception.ApplicationException если категории нет,
   *     она системная или по ней есть операции
   */
  void deleteCategory(DeleteCategoryCommand command);

  /**
   * Возвращает все категории по алфавиту без учёта регистра.
   *
   * @param login логин владельца
   * @return категории обоих типов
   */
  List<CategoryViewDto> listCategories(String login);
}
