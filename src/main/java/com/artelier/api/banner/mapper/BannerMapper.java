package com.artelier.api.banner.mapper;

import com.artelier.api.banner.dto.BannerResponse;
import com.artelier.api.banner.entity.Banner;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring",
        uses = ItemBannerMapper.class
)
public interface BannerMapper {

    BannerResponse toResponse(Banner banner);
}