package org.example.finmanager.adapter.inbound.cli.input;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class AmountParserTest {

  @ParameterizedTest
  @CsvSource({
    "1500, 1500",
    "1500.50, 1500.50",
    "'1500,50', 1500.50",
    "' 42 ', 42",
    "-5, -5",
    "+5, 5",
    "0, 0",
    "10.999, 10.999",
    "007, 7"
  })
  public void parsesSupportedFormats(String text, String expected) {
    Optional<BigDecimal> amount = AmountParser.parse(text);

    assertThat(amount).isPresent();
    assertThat(amount.get()).isEqualByComparingTo(expected);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "", " ", "abc", "1e3", "1E3", "1,500.50", "1.", ".5", "10 20", "1,2,3", "--5", "5-"
      })
  public void rejectsUnsupportedFormats(String text) {
    assertThat(AmountParser.parse(text)).isEmpty();
  }
}
