package org.example.finmanager.adapter.inbound.cli.input;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class DateParserTest {

  @Test
  public void parsesDayMonthYear() {
    assertThat(DateParser.parse("08.11.2026")).contains(LocalDate.of(2026, 11, 8));
  }

  @Test
  public void acceptsDayAndMonthWithoutLeadingZeroAndSurroundingSpaces() {
    assertThat(DateParser.parse(" 8.1.2026 ")).contains(LocalDate.of(2026, 1, 8));
  }

  @Test
  public void acceptsLeapDay() {
    assertThat(DateParser.parse("29.02.2028")).contains(LocalDate.of(2028, 2, 29));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "abc",
        "2026-11-08",
        "11/08/2026",
        "08.11.26",
        "08.11",
        "32.01.2026",
        "31.02.2026",
        "29.02.2027",
        "00.01.2026",
        "01.13.2026"
      })
  public void rejectsWrongFormatAndNonExistentDates(String text) {
    assertThat(DateParser.parse(text)).isEmpty();
  }
}
