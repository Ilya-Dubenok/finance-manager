package org.example.finmanager.toolchain;

import org.mapstruct.Mapper;

/** Образец маппера между application DTO и файловым DTO. */
@Mapper
public interface SampleMapper {

  /** Преобразует DTO приложения в файловый DTO. */
  SampleFileDto toFile(SampleDto dto);

  /** Преобразует файловый DTO в DTO приложения. */
  SampleDto toDto(SampleFileDto file);
}
