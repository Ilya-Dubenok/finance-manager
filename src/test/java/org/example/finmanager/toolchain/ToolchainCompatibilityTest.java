package org.example.finmanager.toolchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** Проверяет, что Lombok и MapStruct работают с records на текущем JDK. */
public class ToolchainCompatibilityTest {

  private static final String NAME = "a";
  private static final String CHANGED_NAME = "b";
  private static final String SMALL_AMOUNT = "1.50";
  private static final String LARGE_AMOUNT = "1000.50";

  private final SampleMapper mapper = Mappers.getMapper(SampleMapper.class);

  @Test
  public void lombokBuilderCopiesRecordWithChange() {
    SampleDto original =
        SampleDto.builder().name(NAME).amount(new BigDecimal(SMALL_AMOUNT)).build();

    SampleDto changed = original.toBuilder().name(CHANGED_NAME).build();

    assertThat(changed.name()).isEqualTo(CHANGED_NAME);
    assertThat(changed.amount()).isEqualByComparingTo(SMALL_AMOUNT);
  }

  @Test
  public void mapStructConvertsRecordsAndBigDecimalBothWays() {
    SampleDto dto = SampleDto.builder().name(NAME).amount(new BigDecimal(LARGE_AMOUNT)).build();

    SampleFileDto file = mapper.toFile(dto);
    SampleDto back = mapper.toDto(file);

    assertThat(file.amount()).isEqualTo(LARGE_AMOUNT);
    assertThat(back).isEqualTo(dto);
  }
}
