package com.artelier.api.service;

import com.artelier.api.dto.request.BannerRequest;
import com.artelier.api.dto.response.BannerResponse;
import com.artelier.api.entity.Banner;
import com.artelier.api.exception.ArtelierException;
import com.artelier.api.integration.cloudinary.service.CloudinaryService;
import com.artelier.api.repository.BannerRepository;
import com.artelier.api.util.ImageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerService {

    private static final String FOLDER = "artelier/banners";
    private static final String NOT_FOUND = "Banner not found";
    /** Ruta interna ("/products") o URL http(s). Bloquea javascript:, data:, "//host", etc. */
    private static final Pattern BUTTON_URL = Pattern.compile("^(/(?!/)\\S*|https?://\\S+)$");

    private final BannerRepository bannerRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional(readOnly = true)
    public List<BannerResponse> listActive() {
        return bannerRepository.findByIsActiveTrueOrderBySortOrderAsc().stream().map(BannerResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BannerResponse> listAll() {
        return bannerRepository.findAllByOrderBySortOrderAsc().stream().map(BannerResponse::from).toList();
    }

    @Transactional
    public BannerResponse create(BannerRequest data, MultipartFile image) {
        validateButton(data);
        if (image == null || image.isEmpty()) {
            throw ArtelierException.badRequest("La imagen es obligatoria");
        }
        ImageValidator.validate(image);

        var uploaded = cloudinaryService.upload(image, FOLDER);
        Banner banner = new Banner();
        apply(banner, data);
        banner.setImageUrl(uploaded.getSecureUrl());
        banner.setCloudinaryId(uploaded.getPublicId());
        return BannerResponse.from(bannerRepository.save(banner));
    }

    @Transactional
    public BannerResponse update(UUID id, BannerRequest data, MultipartFile image) {
        validateButton(data);
        Banner banner = bannerRepository.findById(id).orElseThrow(() -> ArtelierException.notFound(NOT_FOUND));

        String oldCloudinaryId = null;
        if (image != null && !image.isEmpty()) {
            ImageValidator.validate(image);
            var uploaded = cloudinaryService.upload(image, FOLDER);
            oldCloudinaryId = banner.getCloudinaryId();
            banner.setImageUrl(uploaded.getSecureUrl());
            banner.setCloudinaryId(uploaded.getPublicId());
        }
        apply(banner, data);
        Banner saved = bannerRepository.save(banner);
        deleteQuietly(oldCloudinaryId);
        return BannerResponse.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Banner banner = bannerRepository.findById(id).orElseThrow(() -> ArtelierException.notFound(NOT_FOUND));
        bannerRepository.delete(banner);
        deleteQuietly(banner.getCloudinaryId());
    }

    @Transactional
    public BannerResponse toggle(UUID id) {
        Banner banner = bannerRepository.findById(id).orElseThrow(() -> ArtelierException.notFound(NOT_FOUND));
        banner.setIsActive(!Boolean.TRUE.equals(banner.getIsActive()));
        return BannerResponse.from(bannerRepository.save(banner));
    }

    private void apply(Banner b, BannerRequest d) {
        b.setTitle(d.title().trim());
        b.setDescription(d.description().trim());
        b.setButtonLabel(blankToNull(d.buttonLabel()));
        b.setButtonUrl(blankToNull(d.buttonUrl()));
        b.setSortOrder(d.sortOrder());
        b.setIsActive(d.isActive() == null || d.isActive());
    }

    private void validateButton(BannerRequest d) {
        String label = blankToNull(d.buttonLabel());
        String url = blankToNull(d.buttonUrl());
        if ((label == null) != (url == null)) {
            throw ArtelierException.badRequest("buttonLabel y buttonUrl van juntos: ambos o ninguno");
        }
        if (url != null && !BUTTON_URL.matcher(url).matches()) {
            throw ArtelierException.badRequest("buttonUrl debe ser una ruta interna (/products) o una URL http(s)");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private void deleteQuietly(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinaryService.delete(publicId);
        } catch (Exception e) {
            log.warn("No se pudo borrar la imagen {} de Cloudinary: {}", publicId, e.getMessage());
        }
    }
}
