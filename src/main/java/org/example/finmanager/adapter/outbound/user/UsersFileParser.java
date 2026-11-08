package org.example.finmanager.adapter.outbound.user;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.example.finmanager.application.dto.model.UserDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * Разбирает строки файла пользователей формата {@code login=password}.
 *
 * <p>Пустые строки и строки с {@code #} в начале пропускаются. Пароль берётся как есть: всё после
 * первого знака равенства, без обрезки пробелов. Ошибка в любой строке — {@link
 * ApplicationException} с номером строки.
 */
@Component
public class UsersFileParser {

  private static final char SEPARATOR = '=';
  private static final String COMMENT_PREFIX = "#";
  private static final char BOM = '﻿';
  private static final Pattern LOGIN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{3,32}");

  /**
   * Разбирает файл.
   *
   * @param fileName имя файла для сообщений об ошибках
   * @param lines строки файла
   * @return пользователи по логину
   */
  public Map<String, UserDto> parse(String fileName, List<String> lines) {
    Map<String, UserDto> users = new LinkedHashMap<>();
    Map<String, Integer> lineByLowerLogin = new HashMap<>();
    for (int index = 0; index < lines.size(); index++) {
      int lineNumber = index + 1;
      String line = stripBom(lines.get(index), index);
      if (isSkipped(line)) {
        continue;
      }
      UserDto user = parseLine(fileName, lineNumber, line);
      String lowerLogin = user.login().toLowerCase(Locale.ROOT);
      Integer firstLine = lineByLowerLogin.putIfAbsent(lowerLogin, lineNumber);
      if (firstLine != null) {
        throw new ApplicationException(
            ErrorCode.USERS_FILE_DUPLICATE_LOGIN, fileName, lineNumber, user.login(), firstLine);
      }
      users.put(user.login(), user);
    }
    if (users.isEmpty()) {
      throw new ApplicationException(ErrorCode.USERS_FILE_NO_USERS, fileName);
    }
    return users;
  }

  private UserDto parseLine(String fileName, int lineNumber, String line) {
    int separator = line.indexOf(SEPARATOR);
    if (separator < 0) {
      throw new ApplicationException(ErrorCode.USERS_FILE_NO_SEPARATOR, fileName, lineNumber);
    }
    String login = line.substring(0, separator).trim();
    String password = line.substring(separator + 1);
    if (!LOGIN_PATTERN.matcher(login).matches()) {
      throw new ApplicationException(ErrorCode.USERS_FILE_BAD_LOGIN, fileName, lineNumber, login);
    }
    if (password.isBlank()) {
      throw new ApplicationException(
          ErrorCode.USERS_FILE_EMPTY_PASSWORD, fileName, lineNumber, login);
    }
    return new UserDto(login, password);
  }

  private String stripBom(String line, int index) {
    if (index == 0 && !line.isEmpty() && line.charAt(0) == BOM) {
      return line.substring(1);
    }
    return line;
  }

  private boolean isSkipped(String line) {
    return line.isBlank() || line.stripLeading().startsWith(COMMENT_PREFIX);
  }
}
