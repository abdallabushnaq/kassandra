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
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionException;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dao.AboutImageDAO;
import de.bushnaq.abdalla.kassandra.repository.AboutImageRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Service that manages the About View banner image.
 * <p>
 * On application startup a background daemon thread attempts to generate a 512×512 image
 * via Stable Diffusion. The result is cached both in memory (for the current process) and
 * in the database (across restarts). A new image is generated once per calendar day. When
 * Stable Diffusion is unavailable the last successfully generated image from the database
 * is used, so the About View always shows something useful.
 * </p>
 */
@Service
@Slf4j
public class AboutBoxService {

    /**
     * Stable Diffusion prompt used to generate the daily About View banner image.
     */
    static final  String SD_PROMPT       =
            "oracle Kassandra prophesying in ancient Troy, dramatic moonlight, stars, digital painting, photorealistic, cinematic lighting";
    /**
     * Singleton row id used in the {@code about_images} table.
     */
    private static final Long   SINGLETON_ID    = 1L;
    /**
     * Sentinel value stored in {@link #cachedImage} when no image is available at all
     * (SD unavailable <em>and</em> no DB record), so callers can distinguish
     * "not yet known" ({@code null}) from "definitely nothing to show" (empty array).
     */
    private static final byte[] SD_UNAVAILABLE  = new byte[0];

    @Autowired
    private AboutImageRepository aboutImageRepository;
    @Autowired
    private StableDiffusionService stableDiffusionService;

    /**
     * The application version, resolved from {@code kassandra.version} in
     * {@code application.properties} via Maven resource filtering at build time.
     * Example: {@code "0.0.1"}.
     */
    @Getter
    @Value("${kassandra.version:0.0.1}")
    private String version;

    private volatile byte[]    cachedImage      = null;
    private volatile LocalDate cacheDate        = null;
    /**
     * Set to {@code false} by {@link #destroy()} so that any in-flight or future call to
     * {@link #getOrGenerateImage()} returns the sentinel value without touching the database.
     * This prevents Hibernate "table not found" warnings when the JPA schema is dropped
     * during Spring context teardown (e.g. between {@code @DirtiesContext} tests).
     */
    private volatile boolean   active           = true;
    /**
     * Reference to the background pre-load thread so that {@link #destroy()} can join it.
     */
    private          Thread    backgroundThread = null;

    /**
     * Kicks off a daemon background thread that pre-loads or generates the first daily
     * image so the About View can open immediately on first use.
     */
    @PostConstruct
    private void init() {
        backgroundThread = new Thread(this::getOrGenerateImage, "about-image-init");
        backgroundThread.setDaemon(true);
        backgroundThread.start();
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
     * Returns the banner image bytes for the About View.
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
     * @return PNG bytes, an empty array when no image is available at all, or {@code null}
     *         while the init thread is still running (should not be observed by callers
     *         that block on this method)
     */
    public synchronized byte[] getOrGenerateImage() {
        LocalDate today = LocalDate.now();

        // Guard: return immediately if the service is being shut down.
        if (!active) {
            return SD_UNAVAILABLE;
        }

        // 1. In-memory cache hit
        if (cachedImage != null && today.equals(cacheDate)) {
            return cachedImage;
        }

        // 2. Load the persisted row (singleton id = 1)
        Optional<AboutImageDAO> dbRecord = aboutImageRepository.findById(SINGLETON_ID);

        // 3. SD unavailable – return whatever is in the DB (even if stale)
        if (!stableDiffusionService.isAvailable()) {
            if (dbRecord.isPresent() && hasImageData(dbRecord.get())) {
                log.info("Stable Diffusion not available – using database image ({})", dbRecord.get().getImageDate());
                cacheFromDb(dbRecord.get());
            } else {
                log.info("Stable Diffusion not available and no database image – About View banner will be hidden");
                cachedImage = SD_UNAVAILABLE;
                cacheDate   = today;
            }
            return cachedImage;
        }

        // 4. DB already has today's image – no need to call SD
        if (dbRecord.isPresent() && today.equals(dbRecord.get().getImageDate()) && hasImageData(dbRecord.get())) {
            log.info("Loading today's About View image from database");
            cacheFromDb(dbRecord.get());
            return cachedImage;
        }

        // 5. Generate a fresh image via Stable Diffusion
        try {
            log.info("Generating About View banner image via Stable Diffusion");
            GeneratedImageResult result = stableDiffusionService.text2ImgWithOriginal(SD_PROMPT, 512, null, -1L, 7.0);
            cachedImage = result.getOriginalImage();
            cacheDate   = today;
            persistToDb(dbRecord, today, cachedImage);
            log.info("About View banner image generated and persisted for {}", today);
        } catch (StableDiffusionException e) {
            log.warn("Failed to generate About View image: {}", e.getMessage());
            // Fall back to any existing DB image rather than showing nothing
            if (dbRecord.isPresent() && hasImageData(dbRecord.get())) {
                log.info("Using previous database image as fallback ({})", dbRecord.get().getImageDate());
                cacheFromDb(dbRecord.get());
            } else {
                cachedImage = SD_UNAVAILABLE;
                cacheDate   = today;
            }
        }
        return cachedImage;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static boolean hasImageData(AboutImageDAO dao) {
        return dao.getImageData() != null && dao.getImageData().length > 0;
    }

    private void cacheFromDb(AboutImageDAO dao) {
        cachedImage = dao.getImageData();
        cacheDate   = dao.getImageDate();
    }

    private void persistToDb(Optional<AboutImageDAO> existing, LocalDate date, byte[] data) {
        AboutImageDAO dao = existing.orElse(new AboutImageDAO());
        dao.setId(SINGLETON_ID);
        dao.setImageDate(date);
        dao.setImageData(data);
        aboutImageRepository.save(dao);
    }
}
