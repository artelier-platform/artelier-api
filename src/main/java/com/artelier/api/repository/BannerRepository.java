package com.artelier.api.repository;

import com.artelier.api.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BannerRepository extends JpaRepository<Banner, UUID> {

    List<Banner> findByIsActiveTrueOrderBySortOrderAsc();

    List<Banner> findAllByOrderBySortOrderAsc();
}
