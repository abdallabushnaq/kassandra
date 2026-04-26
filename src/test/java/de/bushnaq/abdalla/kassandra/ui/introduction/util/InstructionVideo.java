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

package de.bushnaq.abdalla.kassandra.ui.introduction.util;

import lombok.Data;

import java.util.List;

/**
 * Metadata descriptor for a single introduction video.
 * <p>
 * Carries both recording parameters (title, version) and the YouTube upload
 * metadata that is written to the JSON sidecar file produced by
 * {@code AbstractIntroductionVideo#createSideCar()}.
 */
@Data
public class InstructionVideo {
    /**
     * Subtitle shown on the Apache License end-card overlay.
     */
    public static final String COPYLEFT_SUBTITLE     = "Apache License, version 2.0";
    /**
     * Root sub-folder (relative to {@code test-recordings/}) for all introduction videos.
     */
    public static final String TARGET_FOLDER         = "introduction";
    public static final int    VIDEO_EXTENDED_HEIGHT = 1300;
    /**
     * Recording height in pixels.
     */
    public static final int    VIDEO_HEIGHT          = 1200;
    /**
     * Sub-title shown on the intro overlay.
     */
    public static final String VIDEO_SUBTITLE        = "Introduction Video";
    /**
     * Recording width in pixels.
     */
    public static final int    VIDEO_WIDTH           = 1750;

    /**
     * YouTube numeric category ID (e.g. {@code "28"} for Science &amp; Technology).
     */
    private String       categoryId    = "28";
    /**
     * Full video description; may contain embedded newlines.
     */
    private String       description;
    /**
     * YouTube playlist ID to which the video should be added after upload.
     */
    private String       playlistId    = "PL1FdjPuGzg7LDRGZeP6uQAPet1_fZePGs";
    /**
     * YouTube privacy status: {@code "public"}, {@code "unlisted"}, or {@code "private"}.
     */
    private String       privacyStatus = "public";
    /**
     * Searchable keyword tags for the YouTube upload.
     */
    private List<String> tags          = List.of("instruction", "project management", "kassandra");
    /**
     * Human-readable video title used for both the overlay and the output file name.
     */
    private String       title;
    /**
     * Monotonically increasing version number appended to the output file name.
     */
    private int          version;
}
