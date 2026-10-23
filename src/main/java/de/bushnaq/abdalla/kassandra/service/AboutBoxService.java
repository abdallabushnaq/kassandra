/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.service;

import de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionConfig;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionException;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.config.KassandraProperties;
import de.bushnaq.abdalla.kassandra.dao.AboutImageDAO;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.repository.AboutImageRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;

/**
 * Service that manages the About View banner image.
 * <p>
 * On application startup a background daemon thread attempts to generate a banner image per theme
 * via Stable Diffusion. The result is cached both in memory (for the current process) and
 * in the database (across restarts). A new image is generated once per theme per calendar day. When
 * Stable Diffusion is unavailable the last successfully generated image from the database
 * is used, so the About View always shows something useful.
 * </p>
 */
@Service
@Slf4j
public class AboutBoxService {

    static final         String[]                   SD_LORA          = {
            "<lora:- SDXL - ptrn-no1_style_V1.0:1> ptrn-no1",
            "<lora:Abstract_Mosaic_Pattern:0.5> Abstract_Mosaic_Pattern",
            "<lora:AbstractPatternStyleXL:1> AbstractPatternStyle",
            "<lora:Circuit_pattern:1> circuitpattern",
            "<lora:novuschroma66 style_:1.3> novuschroma66 style swirl patterns",
            "<lora:ral-dstgrtptrn-sdxl:1> ral-dstgrtptrn",
            "<lora:Test_Golden_Patterns.:1> gold patterns, gold and black spirit, liquid gold explosion, golden smoke magic, star dust, golden milky way, magic fog.",
            "<lora:Zarabi:1> adrr-zrb, patterns, intricate weavings, rich colors",
    };
    /**
     * Stable Diffusion prompt used to generate the daily About View banner image.
     */
    static final         String                     SD_PROMPT        =
            """
                    Artistic, based on patterns and lines. Abstract futuristic digital art, AI circuit boards.
                    Futuristic neon color.
                    The connections shape the barely visible face of kassandra of troy, very futuristic AI robot.
                    Ultra-sharp details.
                    Intricate micro-details, 8k resolution, colorful.
                    """;
    /**
     * Sentinel value stored in {@link #cachedImages} when no image is available at all
     * (SD unavailable <em>and</em> no DB record), so callers can distinguish
     * "not yet known" ({@code null}) from "definitely nothing to show" (empty array).
     */
    private static final byte[]                     SD_UNAVAILABLE   = new byte[0];
    @Autowired
    private              AboutImageRepository       aboutImageRepository;
    /**
     * Set to {@code false} by {@link #destroy()} so that any in-flight or future call to
     * {@link #getOrGenerateImage(ETheme)} returns the sentinel value without touching the database.
     * This prevents Hibernate "table not found" warnings when the JPA schema is dropped
     * during Spring context teardown (e.g. between {@code @DirtiesContext} tests).
     */
    private volatile     boolean                    active           = true;
    /**
     * Reference to the background pre-load thread so that {@link #destroy()} can join it.
     */
    private              Thread                     backgroundThread = null;
    private final        EnumMap<ETheme, LocalDate> cacheDates       = new EnumMap<>(ETheme.class);
    private final        EnumMap<ETheme, byte[]>    cachedImages     = new EnumMap<>(ETheme.class);
    @Autowired
    private              StableDiffusionConfig      stableDiffusionConfig;
    @Autowired
    private              StableDiffusionService     stableDiffusionService;
    @Autowired
    private              KassandraProperties        kassandraProperties;
    /**
     * The application version, resolved from {@code kassandra.version} in
     * {@code application.properties} via Maven resource filtering at build time.
     * Example: {@code "0.0.1"}.
     */
    @Getter
    @Value("${kassandra.version:0.0.1}")
    private              String                     version;

    private void cacheFromDb(AboutImageDAO dao) {
        cachedImages.put(dao.getTheme(), dao.getImageData());
        cacheDates.put(dao.getTheme(), dao.getImageDate());
    }

    /**
     * Signals the background thread to stop and waits for it to finish (up to 2 s).
     * <p>
     * Spring destroys beans in reverse dependency order, so this method runs before
     * the JPA infrastructure is torn down. Joining the thread here ensures it has
     * exited before Hibernate drops the {@code about_images} table, preventing
     * spurious {@code HHH000247} WARN messages during context teardown.
     * </p>
     */
    @PreDestroy
    private void destroy() {
        active = false;
        if (backgroundThread != null && backgroundThread.isAlive()) {
            backgroundThread.interrupt();
            try {
                backgroundThread.join(2_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for about-image-init thread to finish");
            }
        }
    }

    /**
     * Returns the banner image bytes for the About View in the requested theme.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>In-memory cache – returned immediately when valid for today.</li>
     *   <li>Database – today's image loaded from the {@code about_images} table without
     *       contacting Stable Diffusion.</li>
     *   <li>Stable Diffusion – new image generated, saved to DB, cached in memory.</li>
     *   <li>Database fallback – if SD fails, the most recently stored image (any date) is
     *       used so the view never appears blank after the first successful generation.</li>
     *   <li>{@code byte[0]} sentinel – returned only when no image exists anywhere and SD
     *       is unreachable; the view should hide the image area in this case.</li>
     * </ol>
     * </p>
     * <p>
     * This method is {@code synchronized}: only one thread calls SD at a time.
     * </p>
     *
     * @param theme the light/dark theme to generate or retrieve
     * @return PNG bytes, or an empty array when no image is available for this theme
     * @throws NullPointerException if theme is null
     */
    public synchronized byte[] getOrGenerateImage(ETheme theme) {
        Objects.requireNonNull(theme, "Theme must not be null");
        LocalDate today = LocalDate.now();

        // Guard: return immediately if the service is being shut down.
        if (!active) {
            return SD_UNAVAILABLE;
        }

        // 1. In-memory cache hit
        if (cachedImages.containsKey(theme) && today.equals(cacheDates.get(theme))) {
            return cachedImages.get(theme);
        }

        // 2. Load the persisted image for this theme
        Optional<AboutImageDAO> dbRecord = aboutImageRepository.findById(theme);

        if (dbRecord.isPresent() && today.equals(dbRecord.get().getImageDate()) && hasImageData(dbRecord.get())) {
            log.info("Loading today's {} About View image from database", theme);
            cacheFromDb(dbRecord.get());
            return cachedImages.get(theme);
        }

        // SD unavailable – return this theme's DB image (even if stale)
        if (!stableDiffusionService.isAvailable()) {
            if (dbRecord.isPresent() && hasImageData(dbRecord.get())) {
                log.info("Stable Diffusion not available – using {} database image ({})", theme, dbRecord.get().getImageDate());
                cacheFromDb(dbRecord.get());
            } else {
                log.info("Stable Diffusion not available and no {} database image – About View banner will be hidden", theme);
                cachedImages.put(theme, SD_UNAVAILABLE);
            }
            return cachedImages.get(theme);
        }

        // Generate a fresh image for this theme via Stable Diffusion
        try {
            log.info("Generating {} About View banner image via Stable Diffusion", theme);
            String background = theme == ETheme.dark
                    ? "Dark background, low-key lighting, background color " + stableDiffusionConfig.getAvatarDarkBackgroundColor()
                    : "Light background, high-key lighting, background color " + stableDiffusionConfig.getAvatarLightBackgroundColor();
            Long configuredSeed = kassandraProperties.getAbout().getImageSeed();
            long seed = configuredSeed != null ? configuredSeed : -1L;
            String configuredLora = kassandraProperties.getAbout().getImageLora();
            String lora = configuredLora != null && !configuredLora.isBlank()
                    ? configuredLora
                    : SD_LORA[(int) (Math.random() * SD_LORA.length)];
            String               prompt = SD_PROMPT + " " + lora + " " + background;
            GeneratedImageResult result = stableDiffusionService.text2ImgWithOriginal(prompt, 1024, null, seed, 3.0);
            byte[]               image  = result.getOriginalImage();
            persistToDb(dbRecord, theme, today, image);
            cachedImages.put(theme, image);
            cacheDates.put(theme, today);
            log.info("{} About View banner image generated and persisted for {}", theme, today);
        } catch (StableDiffusionException e) {
            log.warn("Failed to generate {} About View image: {}", theme, e.getMessage());
            // Fall back to this theme's existing DB image rather than showing nothing
            if (dbRecord.isPresent() && hasImageData(dbRecord.get())) {
                log.info("Using previous {} database image as fallback ({})", theme, dbRecord.get().getImageDate());
                cacheFromDb(dbRecord.get());
            } else {
                cachedImages.put(theme, SD_UNAVAILABLE);
                cacheDates.put(theme, today);
            }
        }
        return cachedImages.get(theme);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static boolean hasImageData(AboutImageDAO dao) {
        return dao.getImageData() != null && dao.getImageData().length > 0;
    }

    /**
     * Kicks off a daemon background thread that pre-loads or generates both daily
     * themed images so the About View can open immediately on first use.
     */
    @PostConstruct
    private void init() {
        backgroundThread = new Thread(() -> {
            for (ETheme theme : ETheme.values()) {
                getOrGenerateImage(theme);
            }
        }, "about-image-init");
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }

    private void persistToDb(Optional<AboutImageDAO> existing, ETheme theme, LocalDate date, byte[] data) {
        AboutImageDAO dao = existing.orElse(new AboutImageDAO());
        dao.setTheme(theme);
        dao.setImageDate(date);
        dao.setImageData(data);
        aboutImageRepository.save(dao);
    }
}
