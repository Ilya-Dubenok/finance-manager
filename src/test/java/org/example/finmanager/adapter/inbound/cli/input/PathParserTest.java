package org.example.finmanager.adapter.inbound.cli.input;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PathParserTest {

  @Test
  public void parsesRelativeAndAbsolutePaths() {
    assertThat(PathParser.parse("data/out.json")).contains(Path.of("data/out.json"));
    assertThat(PathParser.parse("C:\\data\\out.json")).contains(Path.of("C:\\data\\out.json"));
  }

  @Test
  public void surroundingSpacesAndQuotesAreDropped() {
    assertThat(PathParser.parse("  \"C:\\my data\\out.json\"  "))
        .contains(Path.of("C:\\my data\\out.json"));
  }

  @Test
  public void loneQuoteIsNotStripped() {
    Optional<Path> parsed = PathParser.parse("\"abc");

    parsed.ifPresent(path -> assertThat(path.toString()).startsWith("\""));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "\"\"", "\"  \""})
  public void emptyTextGivesNoPath(String text) {
    assertThat(PathParser.parse(text)).isEmpty();
  }

  @Test
  public void invalidPathTextGivesNoPath() {
    assertThat(PathParser.parse("bad\u0000name")).isEmpty();
  }
}
