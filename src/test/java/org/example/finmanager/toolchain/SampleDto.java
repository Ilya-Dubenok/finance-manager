package org.example.finmanager.toolchain;

import java.math.BigDecimal;
import lombok.Builder;

/** Образец application DTO: record с билдером Lombok. */
@Builder(toBuilder = true)
public record SampleDto(String name, BigDecimal amount) {}
