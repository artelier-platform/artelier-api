package com.artelier.api.mapper;

import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.entity.Banner;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring",
        uses = ItemBannerMapper.class
)
public interface BannerMapper {

    BannerResponse toResponse(Banner banner);
}