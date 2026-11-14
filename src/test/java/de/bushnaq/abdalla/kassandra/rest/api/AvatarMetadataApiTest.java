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

package de.bushnaq.abdalla.kassandra.rest.api;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.AvatarService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.CacheAwareAvatarService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionConfig;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionException;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.TiledDiffusionOptions;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.CozyStylePrompt;
import de.bushnaq.abdalla.kassandra.dto.AvatarUpdateRequest;
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.dto.util.AvatarPrompt;
import de.bushnaq.abdalla.kassandra.dto.util.HeaderPrompt;
import de.bushnaq.abdalla.kassandra.dto.util.IconPrompt;
import de.bushnaq.abdalla.kassandra.ui.dialog.ImagePromptDialog;
import de.bushnaq.abdalla.kassandra.util.AbstractTestUtil;
import de.bushnaq.abdalla.kassandra.util.PersistingEntityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Covers image metadata persistence, runtime prompt composition and shared editor acceptance.
 */
@Tag("UnitTest")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@WithMockUser(username = "admin-user", roles = "ADMIN")
public class AvatarMetadataApiTest extends AbstractTestUtil {
    @Autowired
    private AvatarService avatarService;
    @Autowired
    private PersistingEntityGenerator peg;

    @Override
    @BeforeEach
    protected void beforeEach(TestInfo testInfo) {
        super.beforeEach(testInfo);
        peg.init();
    }

    @ParameterizedTest
    @ValueSource(strings = {"product", "feature", "sprint", "user"})
    void metadataRoundTripsAndPartialUpdatesPreserveImages(String entity) {
        Consumer<AvatarUpdateRequest> update;
        Supplier<AvatarUpdateRequest> read;
        if ("user".equals(entity)) {
            User user = peg.addRandomUser(LocalDate.of(2026, 1, 1));
            update = request -> peg.userApi.updateAvatarFull(user.getId(), request);
            read   = () -> peg.userApi.getAvatarFull(user.getId());
        } else {
            Product product = peg.addProduct("Image metadata product");
            if ("product".equals(entity)) {
                update = request -> peg.productApi.updateAvatarFull(product.getId(), request);
                read   = () -> peg.productApi.getAvatarFull(product.getId());
            } else {
                Version version = peg.addVersion(product, "1.0");
                Feature feature = peg.addFeature(version, "Image metadata feature");
                if ("feature".equals(entity)) {
                    update = request -> peg.featureApi.updateAvatarFull(feature.getId(), request);
                    read   = () -> peg.featureApi.getAvatarFull(feature.getId());
                } else {
                    Sprint sprint = peg.addSprint(feature, "Image metadata sprint");
                    update = request -> peg.sprintApi.updateAvatarFull(sprint.getId(), request);
                    read   = () -> peg.sprintApi.getAvatarFull(sprint.getId());
                }
            }
        }
        AvatarUpdateRequest original = read.get();
        assertFalse(original.getAvatarPrompt().contains(AvatarService.LIGHT_PROMPT_SUFFIX));
        assertFalse(original.getAvatarPrompt().contains(AvatarService.DARK_PROMPT_SUFFIX));
        if (!"user".equals(entity)) {
            assertNotNull(original.getStyle());
            assertFalse(original.getHeaderPrompt().contains(AvatarService.LIGHT_PROMPT_SUFFIX));
        }

        AvatarUpdateRequest request = new AvatarUpdateRequest();
        request.setAvatarPrompt("A golden compass");
        request.setNegativePrompt("text, blur");
        if (!"user".equals(entity)) {
            request.setHeaderPrompt("A panoramic mountain landscape");
            request.setStyle(CozyStylePrompt.MARY_BLAIR);
        }
        update.accept(request);
        AvatarUpdateRequest stored = read.get();
        assertEquals(request.getAvatarPrompt(), stored.getAvatarPrompt());
        assertEquals(request.getNegativePrompt(), stored.getNegativePrompt());
        assertEquals(request.getHeaderPrompt(), stored.getHeaderPrompt());
        assertEquals(request.getStyle(), stored.getStyle());
        assertImagesEqual(original, stored);

        AvatarUpdateRequest partial = new AvatarUpdateRequest();
        partial.setNegativePrompt("");
        if (!"user".equals(entity)) {
            partial.setStyle(CozyStylePrompt.DEFAULT);
        }
        update.accept(partial);
        AvatarUpdateRequest patched = read.get();
        assertEquals("", patched.getNegativePrompt());
        assertEquals(request.getAvatarPrompt(), patched.getAvatarPrompt());
        assertEquals(request.getHeaderPrompt(), patched.getHeaderPrompt());
        assertEquals(partial.getStyle(), patched.getStyle());
        assertImagesEqual(original, patched);
    }

    @Test
    void styleOnlyUpdateCreatesGenerationMetadataWithoutImages() {
        Product product = new Product();
        product.setName("Style-only product");
        Product             saved   = peg.productApi.persist(product);
        AvatarUpdateRequest request = new AvatarUpdateRequest();
        request.setStyle(CozyStylePrompt.MARY_BLAIR);
        peg.productApi.updateAvatarFull(saved.getId(), request);
        AvatarUpdateRequest stored = peg.productApi.getAvatarFull(saved.getId());
        assertEquals(CozyStylePrompt.MARY_BLAIR, stored.getStyle());
        assertNull(stored.getAvatarPrompt());
        assertNull(stored.getHeaderPrompt());
        assertNull(stored.getLightAvatarImage());
    }

    @Test
    void jsonContainsOnlySharedMetadataAndAnEnumStyle() {
        AvatarUpdateRequest request = new AvatarUpdateRequest();
        request.setAvatarPrompt("Compass");
        request.setHeaderPrompt("Mountains");
        request.setNegativePrompt("text");
        request.setStyle(CozyStylePrompt.MARY_BLAIR);
        JsonMapper mapper = JsonMapper.builder().build();
        String     json   = mapper.writeValueAsString(request);
        var        node   = mapper.readTree(json);
        assertEquals("MARY_BLAIR", node.get("style").asString());
        assertFalse(node.has("lightAvatarPrompt"));
        assertFalse(node.has("darkAvatarPrompt"));
        assertFalse(node.has("lightHeaderPrompt"));
        assertFalse(node.has("darkHeaderPrompt"));
        assertFalse(node.has("lightAvatarNegativePrompt"));
        assertFalse(node.has("darkAvatarNegativePrompt"));
        assertEquals(request, mapper.readValue(json, AvatarUpdateRequest.class));
    }

    @Test
    void serviceComposesThemeAndHeaderStyleOnlyAtGeneration() throws Exception {
        StableDiffusionService diffusion = mock(StableDiffusionService.class);
        when(diffusion.isAvailable()).thenReturn(true);
        GeneratedImageResult avatar = new GeneratedImageResult(new byte[]{1}, "composed", new byte[]{2}, 42L);
        GeneratedImageResult header = new GeneratedImageResult(new byte[]{3}, "composed", new byte[]{4}, 43L);
        when(diffusion.text2ImgWithOriginal(anyString(), anyString(), anyInt(), isNull(), anyLong(), anyDouble())).thenReturn(avatar);
        when(diffusion.text2ImgWithOriginal(anyString(), anyString(), anyInt(), anyInt(), isNull(), anyLong(), anyDouble(),
                any(TiledDiffusionOptions.class))).thenReturn(header);
        StableDiffusionConfig config  = new StableDiffusionConfig();
        AvatarService         service = avatarService(diffusion, config);

        service.generateLightAvatar("Compass", "text", null);
        service.generateDarkAvatar("Compass", "text", avatar, null);
        verify(diffusion).text2ImgWithOriginal("Compass" + AvatarService.LIGHT_PROMPT_SUFFIX, "text",
                config.getAvatarOutputSize(), null, -1L, config.getCfgScale());
        verify(diffusion).text2ImgWithOriginal("Compass" + AvatarService.DARK_PROMPT_SUFFIX, "text",
                config.getAvatarOutputSize(), null, 42L, config.getCfgScale());

        GeneratedImageResult lightHeader = service.generateLightHeaderWithFallback("Mountains", "cube", null, CozyStylePrompt.MARY_BLAIR);
        service.generateDarkHeaderWithFallback("Mountains", lightHeader, "cube", null, null);
        verify(diffusion).text2ImgWithOriginal(eq("Mountains" + AvatarService.LIGHT_PROMPT_SUFFIX + ", " + CozyStylePrompt.MARY_BLAIR.getPrompt()),
                eq(HeaderPrompt.getDefaultHeaderNegativePrompt()), eq(1024), eq(AvatarService.HEADER_GENERATION_HEIGHT),
                isNull(), eq(-1L), eq(config.getCfgScale()), any(TiledDiffusionOptions.class));
        verify(diffusion).text2ImgWithOriginal(eq("Mountains" + AvatarService.DARK_PROMPT_SUFFIX + ", " + CozyStylePrompt.MARY_BLAIR.getPrompt()),
                eq(HeaderPrompt.getDefaultHeaderNegativePrompt()), eq(1024), eq(AvatarService.HEADER_GENERATION_HEIGHT),
                isNull(), eq(43L), eq(config.getCfgScale()), any(TiledDiffusionOptions.class));
        assertEquals(CozyStylePrompt.MARY_BLAIR, lightHeader.getStyle());
        service.generateLightHeaderWithFallback("Mountains", "cube", null, CozyStylePrompt.DEFAULT);
        verify(diffusion).text2ImgWithOriginal(eq("Mountains" + AvatarService.LIGHT_PROMPT_SUFFIX),
                eq(HeaderPrompt.getDefaultHeaderNegativePrompt()), eq(1024), eq(AvatarService.HEADER_GENERATION_HEIGHT),
                isNull(), eq(-1L), eq(config.getCfgScale()), any(TiledDiffusionOptions.class));

        service.generateLightAvatarWithFallback("Compass", "text", "cube", null, 17L);
        service.generateLightHeaderWithFallback("Mountains", "cube", null,
                TiledDiffusionOptions.forHeader(AvatarService.HEADER_WIDTH, AvatarService.HEADER_HEIGHT),
                CozyStylePrompt.MARY_BLAIR, 23L);
        verify(diffusion).text2ImgWithOriginal("Compass" + AvatarService.LIGHT_PROMPT_SUFFIX, "text",
                config.getAvatarOutputSize(), null, 17L, config.getCfgScale());
        verify(diffusion).text2ImgWithOriginal(eq("Mountains" + AvatarService.LIGHT_PROMPT_SUFFIX + ", " + CozyStylePrompt.MARY_BLAIR.getPrompt()),
                eq(HeaderPrompt.getDefaultHeaderNegativePrompt()), eq(1024), eq(AvatarService.HEADER_GENERATION_HEIGHT),
                isNull(), eq(23L), eq(config.getCfgScale()), any(TiledDiffusionOptions.class));
    }

    @Test
    void fallbackRetainsRequestedPromptAndResolvedHeaderStyle() {
        StableDiffusionService diffusion = mock(StableDiffusionService.class);
        StableDiffusionConfig  config    = new StableDiffusionConfig();
        when(diffusion.generateDefaultLightAvatar("cube", config.getAvatarLightBackgroundColor()))
                .thenReturn(new GeneratedImageResult(new byte[]{1}, "placeholder", new byte[]{2}));
        when(diffusion.generateDefaultHeader("cube", false))
                .thenReturn(new GeneratedImageResult(new byte[]{3}, "placeholder", new byte[]{4}));
        AvatarService        service = avatarService(diffusion, config);
        GeneratedImageResult avatar  = service.generateLightAvatarWithFallback("Compass", "text", "cube", null);
        assertEquals("Compass" + AvatarService.LIGHT_PROMPT_SUFFIX, avatar.getPrompt());
        assertEquals("text", avatar.getNegativePrompt());
        assertNull(avatar.getStyle());
        GeneratedImageResult header = service.generateLightHeaderWithFallback("Mountains", "cube", null, CozyStylePrompt.MARY_BLAIR);
        assertEquals("Mountains" + AvatarService.LIGHT_PROMPT_SUFFIX + ", " + CozyStylePrompt.MARY_BLAIR.getPrompt(), header.getPrompt());
        assertEquals(HeaderPrompt.getDefaultHeaderNegativePrompt(), header.getNegativePrompt());
        assertEquals(CozyStylePrompt.MARY_BLAIR, header.getStyle());
    }

    @Test
    void dialogAcceptsSharedEditsWithoutRegenerationAndRestoresThem() {
        StableDiffusionService diffusion = mock(StableDiffusionService.class);
        when(diffusion.isAvailable()).thenReturn(true);
        AvatarService       service = avatarService(diffusion, new StableDiffusionConfig());
        AvatarUpdateRequest initial = new AvatarUpdateRequest();
        initial.setAvatarPrompt("Compass");
        initial.setNegativePrompt("text");
        initial.setHeaderPrompt("Mountains");
        initial.setStyle(CozyStylePrompt.MARY_BLAIR);
        initial.setLightAvatarImage(new byte[]{1});
        initial.setLightAvatarImageOriginal(new byte[]{2});
        initial.setDarkAvatarImage(new byte[]{3});
        initial.setDarkAvatarImageOriginal(new byte[]{4});
        initial.setLightHeaderImage(new byte[]{5});
        initial.setDarkHeaderImage(new byte[]{6});
        AtomicReference<AvatarUpdateRequest> accepted = new AtomicReference<>();
        ImagePromptDialog                    dialog   = new ImagePromptDialog(service, diffusion, initial, "cube", true, accepted::set);
        component(dialog, ImagePromptDialog.ID_AVATAR_PROMPT_FIELD, TextArea.class).setValue("Edited compass");
        component(dialog, ImagePromptDialog.ID_NEGATIVE_PROMPT_FIELD, TextArea.class).setValue("");
        component(dialog, ImagePromptDialog.ID_HEADER_PROMPT_FIELD, TextArea.class).setValue("Edited mountains");
        assertEquals(CozyStylePrompt.MARY_BLAIR, component(dialog, ImagePromptDialog.ID_STYLE_SELECT, Select.class).getValue());
        component(dialog, ImagePromptDialog.ID_ACCEPT_BUTTON, Button.class).click();

        AvatarUpdateRequest request = accepted.get();
        assertNotNull(request);
        assertEquals("Edited compass", request.getAvatarPrompt());
        assertEquals("", request.getNegativePrompt());
        assertEquals("Edited mountains", request.getHeaderPrompt());
        assertEquals(CozyStylePrompt.MARY_BLAIR, request.getStyle());
        assertImagesEqual(initial, request);
        assertEquals("Compass", initial.getAvatarPrompt());
        assertEquals("text", initial.getNegativePrompt());
        ImagePromptDialog reopened = new ImagePromptDialog(service, diffusion, request, "cube", true, accepted::set);
        assertEquals("Edited compass", component(reopened, ImagePromptDialog.ID_AVATAR_PROMPT_FIELD, TextArea.class).getValue());
        assertEquals("", component(reopened, ImagePromptDialog.ID_NEGATIVE_PROMPT_FIELD, TextArea.class).getValue());
        assertEquals("Edited mountains", component(reopened, ImagePromptDialog.ID_HEADER_PROMPT_FIELD, TextArea.class).getValue());
        assertEquals(CozyStylePrompt.MARY_BLAIR, component(reopened, ImagePromptDialog.ID_STYLE_SELECT, Select.class).getValue());
        verify(diffusion, times(2)).isAvailable();
        verifyNoMoreInteractions(diffusion);
    }

    @Test
    void avatarOnlyDialogCanAcceptMetadataWithoutImages() {
        StableDiffusionService diffusion = mock(StableDiffusionService.class);
        when(diffusion.isAvailable()).thenReturn(true);
        AvatarUpdateRequest initial = new AvatarUpdateRequest();
        initial.setAvatarPrompt(AvatarPrompt.getDefaultAvatarPrompt("Developer"));
        initial.setNegativePrompt(AvatarPrompt.getDefaultAvatarNegativePrompt());
        AtomicReference<AvatarUpdateRequest> accepted = new AtomicReference<>();
        ImagePromptDialog dialog = new ImagePromptDialog(avatarService(diffusion, new StableDiffusionConfig()), diffusion,
                initial, "user", false, accepted::set);
        assertTrue(components(dialog).noneMatch(component -> component.getId().orElse("").equals(ImagePromptDialog.ID_HEADER_PROMPT_FIELD)));
        component(dialog, ImagePromptDialog.ID_AVATAR_PROMPT_FIELD, TextArea.class).setValue("Updated portrait");
        component(dialog, ImagePromptDialog.ID_ACCEPT_BUTTON, Button.class).click();
        assertEquals("Updated portrait", accepted.get().getAvatarPrompt());
        assertNull(accepted.get().getStyle());
        assertNull(accepted.get().getHeaderPrompt());
        assertNull(accepted.get().getLightAvatarImage());
    }

    @Test
    void defaultsAreThemeIndependent() {
        assertFalse(AvatarPrompt.getDefaultAvatarPrompt("Developer").contains(AvatarService.LIGHT_PROMPT_SUFFIX));
        assertFalse(IconPrompt.getDefaultAvatarPrompt("Product").contains(AvatarService.DARK_PROMPT_SUFFIX));
        assertFalse(HeaderPrompt.getDefaultHeaderPrompt("Product").contains(AvatarService.LIGHT_PROMPT_SUFFIX));
    }

    @Test
    void testContextInjectsCacheAwareAvatarService() {
        assertInstanceOf(CacheAwareAvatarService.class, avatarService);
        assertSame(avatarService, ReflectionTestUtils.getField(peg, "avatarService"));
    }

    @Test
    void cachePersistsResultsAndSeparatesAvatarParameters(@TempDir Path directory) throws Exception {
        StableDiffusionService diffusion = mock(StableDiffusionService.class);
        StableDiffusionConfig config = new StableDiffusionConfig();
        when(diffusion.isAvailable()).thenReturn(true);
        when(diffusion.text2ImgWithOriginal(anyString(), nullable(String.class), anyInt(), isNull(), anyLong(), anyDouble()))
                .thenAnswer(invocation -> {
                    GeneratedImageResult result = new GeneratedImageResult(new byte[]{1, 2}, invocation.getArgument(0),
                            new byte[]{3, 4}, 42L);
                    String negative = invocation.getArgument(1);
                    result.setNegativePrompt(negative == null || negative.isBlank() ? StableDiffusionService.NEGATIVE_PROMPT : negative);
                    return result;
                });
        CacheAwareAvatarService first = cachedAvatarService(directory, diffusion, config);
        CacheAwareAvatarService second = cachedAvatarService(directory, diffusion, config);
        StableDiffusionService.setEnabled(true);
        try {
            GeneratedImageResult original = first.generateLightAvatar("Compass", "text", null);
            when(diffusion.isAvailable()).thenReturn(false);
            GeneratedImageResult cached = second.generateLightAvatar("Compass", "text", null);
            assertEquals(original, cached);
            assertEquals(42L, cached.getSeed());
            assertArrayEquals(new byte[]{1, 2}, cached.getOriginalImage());
            assertArrayEquals(new byte[]{3, 4}, cached.getResizedImage());
            assertEquals("Compass" + AvatarService.LIGHT_PROMPT_SUFFIX, cached.getPrompt());
            assertEquals("text", cached.getNegativePrompt());
            when(diffusion.isAvailable()).thenReturn(true);
            second.generateLightAvatar("Other", "text", null);
            second.generateLightAvatar("Compass", "letters", null);
            second.generateLightAvatar("Compass", "text", null, 1L);
            second.generateDarkAvatar("Compass", "text", cached, null);
            config.setAvatarOutputSize(config.getAvatarOutputSize() + 1);
            second.generateLightAvatar("Compass", "text", null);
            config.setGenerationSize(config.getGenerationSize() + 1);
            second.generateLightAvatar("Compass", "text", null);
            config.setModelName("another-model");
            second.generateLightAvatar("Compass", "text", null);
            config.setDefaultSampler("another-sampler");
            second.generateLightAvatar("Compass", "text", null);
            config.setDefaultSteps(config.getDefaultSteps() + 1);
            second.generateLightAvatar("Compass", "text", null);
            config.setCfgScale(config.getCfgScale() + 1);
            second.generateLightAvatar("Compass", "text", null);
            config.setApiUrl("http://another-host");
            second.generateLightAvatar("Compass", "text", null);
            GeneratedImageResult defaultNegative = second.generateLightAvatar("No negative", null, null);
            assertEquals(defaultNegative, second.generateLightAvatar("No negative", " ", null));
            verify(diffusion, times(13)).text2ImgWithOriginal(anyString(), nullable(String.class), anyInt(), isNull(), anyLong(), anyDouble());
            verify(diffusion).text2ImgWithOriginal(eq("Compass" + AvatarService.DARK_PROMPT_SUFFIX), eq("text"),
                    anyInt(), isNull(), eq(42L), anyDouble());
        } finally {
            StableDiffusionService.setEnabled(false);
        }
    }

    @Test
    void cacheSeparatesHeaderStylesAndTiledOptions(@TempDir Path directory) throws Exception {
        StableDiffusionService diffusion = mock(StableDiffusionService.class);
        StableDiffusionConfig config = new StableDiffusionConfig();
        when(diffusion.isAvailable()).thenReturn(true);
        when(diffusion.text2ImgWithOriginal(anyString(), anyString(), anyInt(), anyInt(), isNull(), anyLong(), anyDouble(),
                any(TiledDiffusionOptions.class))).thenAnswer(invocation -> {
            GeneratedImageResult result = new GeneratedImageResult(new byte[]{1}, invocation.getArgument(0), new byte[]{2}, 73L);
            result.setNegativePrompt(invocation.getArgument(1));
            return result;
        });
        CacheAwareAvatarService service = cachedAvatarService(directory, diffusion, config);
        TiledDiffusionOptions options = TiledDiffusionOptions.forHeader(4096, 256);
        StableDiffusionService.setEnabled(true);
        try {
            GeneratedImageResult light = service.generateLightHeaderWithFallback("Mountains", "cube", null, options, CozyStylePrompt.MARY_BLAIR, null);
            GeneratedImageResult cached = service.generateLightHeaderWithFallback("Mountains", "cube", null, options, CozyStylePrompt.MARY_BLAIR, null);
            assertEquals(light, cached);
            assertEquals(CozyStylePrompt.MARY_BLAIR, cached.getStyle());
            service.generateDarkHeaderWithFallback("Mountains", cached, "cube", null, options, null);
            service.generateLightHeaderWithFallback("Mountains", "cube", null, options, CozyStylePrompt.DEFAULT, null);
            service.generateLightHeaderWithFallback("Mountains", "cube", null, TiledDiffusionOptions.forHeader(2048, 256), CozyStylePrompt.MARY_BLAIR, null);
            TiledDiffusionOptions changedTile = TiledDiffusionOptions.builder().enabled(true).overwriteImageSize(true)
                    .imageWidth(4096).imageHeight(256).latentTileWidth(64).build();
            service.generateLightHeaderWithFallback("Mountains", "cube", null, changedTile, CozyStylePrompt.MARY_BLAIR, null);
            List<TiledDiffusionOptions> variants = List.of(
                    TiledDiffusionOptions.builder().enabled(true).overwriteImageSize(true).imageWidth(4096).imageHeight(512).build(),
                    TiledDiffusionOptions.builder().enabled(true).overwriteImageSize(true).imageWidth(4096).imageHeight(256).latentTileHeight(64).build(),
                    TiledDiffusionOptions.builder().enabled(true).overwriteImageSize(true).imageWidth(4096).imageHeight(256).latentTileOverlap(32).build(),
                    TiledDiffusionOptions.builder().enabled(true).overwriteImageSize(true).imageWidth(4096).imageHeight(256).latentTileBatchSize(2).build(),
                    TiledDiffusionOptions.builder().enabled(true).overwriteImageSize(false).imageWidth(4096).imageHeight(256).build(),
                    TiledDiffusionOptions.builder().enabled(false).build());
            for (TiledDiffusionOptions variant : variants) {
                service.generateLightHeaderWithFallback("Mountains", "cube", null, variant, CozyStylePrompt.MARY_BLAIR, null);
            }
            service.generateLightHeaderWithFallback("Mountains", "cube", null, null, CozyStylePrompt.MARY_BLAIR, null);
            verify(diffusion, times(11)).text2ImgWithOriginal(anyString(), anyString(), anyInt(), anyInt(), isNull(), anyLong(), anyDouble(),
                    any(TiledDiffusionOptions.class));
            verify(diffusion).text2ImgWithOriginal(contains(AvatarService.DARK_PROMPT_SUFFIX), anyString(), anyInt(), anyInt(),
                    isNull(), eq(73L), anyDouble(), any(TiledDiffusionOptions.class));
        } finally {
            StableDiffusionService.setEnabled(false);
        }
    }

    @Test
    void cacheDoesNotStoreFallbacksAndRejectsCorruptEntries(@TempDir Path directory) throws Exception {
        StableDiffusionService diffusion = mock(StableDiffusionService.class);
        StableDiffusionConfig config = new StableDiffusionConfig();
        when(diffusion.generateDefaultLightAvatar(anyString(), anyString()))
                .thenReturn(new GeneratedImageResult(new byte[]{8}, "fallback", new byte[]{9}));
        when(diffusion.generateDefaultHeader("cube", false))
                .thenReturn(new GeneratedImageResult(new byte[]{6}, "fallback", new byte[]{7}));
        CacheAwareAvatarService service = cachedAvatarService(directory, diffusion, config);
        StableDiffusionService.setEnabled(true);
        try {
            GeneratedImageResult fallback = service.generateLightAvatarWithFallback("Compass", "text", "cube", null);
            assertArrayEquals(new byte[]{9}, fallback.getResizedImage());
            GeneratedImageResult header = service.generateLightHeaderWithFallback("Mountains", "cube", null, CozyStylePrompt.MARY_BLAIR);
            assertArrayEquals(new byte[]{7}, header.getResizedImage());
            try (Stream<Path> files = Files.list(directory)) {
                assertEquals(0, files.filter(path -> path.toString().endsWith(".json")).count());
            }
            when(diffusion.isAvailable()).thenReturn(true);
            when(diffusion.text2ImgWithOriginal(anyString(), anyString(), anyInt(), isNull(), anyLong(), anyDouble()))
                    .thenThrow(new StableDiffusionException("generation failed"));
            assertArrayEquals(new byte[]{9}, service.generateLightAvatarWithFallback("Compass", "text", "cube", null).getResizedImage());
            try (Stream<Path> files = Files.list(directory)) {
                assertEquals(0, files.filter(path -> path.toString().endsWith(".json")).count());
            }
            doReturn(new GeneratedImageResult(new byte[]{1}, "generated", new byte[]{2}, 42))
                    .when(diffusion).text2ImgWithOriginal(anyString(), anyString(), anyInt(), isNull(), anyLong(), anyDouble());
            service.generateLightAvatar("Compass", "text", null);
            StableDiffusionService.setEnabled(false);
            when(diffusion.isAvailable()).thenReturn(false);
            assertThrows(StableDiffusionException.class, () -> service.generateLightAvatar("Compass", "text", null));
            StableDiffusionService.setEnabled(true);
            Path entry;
            try (Stream<Path> files = Files.list(directory)) {
                entry = files.filter(path -> path.toString().endsWith(".json")).findFirst().orElseThrow();
            }
            Files.writeString(entry, "not json");
            IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.generateLightAvatar("Compass", "text", null));
            assertTrue(error.getMessage().contains(entry.toString()));
        } finally {
            StableDiffusionService.setEnabled(false);
        }
    }

    @Test
    void cacheSurfacesStorageErrors(@TempDir Path directory) throws Exception {
        Path notDirectory = Files.createFile(directory.resolve("not-a-directory"));
        StableDiffusionService diffusion = mock(StableDiffusionService.class);
        CacheAwareAvatarService service = cachedAvatarService(notDirectory, diffusion, new StableDiffusionConfig());
        StableDiffusionService.setEnabled(true);
        try {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> service.generateLightAvatarWithFallback("Compass", "text", "cube", null));
            assertTrue(error.getMessage().contains(notDirectory.toString()));
            assertNotNull(error.getCause());
            verifyNoInteractions(diffusion);
        } finally {
            StableDiffusionService.setEnabled(false);
        }
    }

    @Test
    void concurrentRequestsGenerateOnlyOnce(@TempDir Path directory) throws Exception {
        StableDiffusionService diffusion = mock(StableDiffusionService.class);
        StableDiffusionConfig config = new StableDiffusionConfig();
        when(diffusion.isAvailable()).thenReturn(true);
        when(diffusion.text2ImgWithOriginal(anyString(), anyString(), anyInt(), isNull(), anyLong(), anyDouble()))
                .thenReturn(new GeneratedImageResult(new byte[]{1}, "generated", new byte[]{2}, 42));
        CacheAwareAvatarService first = cachedAvatarService(directory, diffusion, config);
        CacheAwareAvatarService second = cachedAvatarService(directory, diffusion, config);
        StableDiffusionService.setEnabled(true);
        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            List<Future<GeneratedImageResult>> results = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                CacheAwareAvatarService service = i % 2 == 0 ? first : second;
                results.add(executor.submit(() -> service.generateLightAvatar("Compass", "text", null)));
            }
            for (Future<GeneratedImageResult> result : results) {
                assertEquals(42L, result.get().getSeed());
            }
            verify(diffusion, times(1)).text2ImgWithOriginal(anyString(), anyString(), anyInt(), isNull(), anyLong(), anyDouble());
        } finally {
            StableDiffusionService.setEnabled(false);
        }
    }

    private CacheAwareAvatarService cachedAvatarService(Path directory, StableDiffusionService diffusion, StableDiffusionConfig config) {
        CacheAwareAvatarService service = new CacheAwareAvatarService(directory);
        ReflectionTestUtils.setField(service, "stableDiffusionService", diffusion);
        ReflectionTestUtils.setField(service, "config", config);
        return service;
    }

    private AvatarService avatarService(StableDiffusionService diffusion, StableDiffusionConfig config) {
        AvatarService service = new AvatarService();
        ReflectionTestUtils.setField(service, "stableDiffusionService", diffusion);
        ReflectionTestUtils.setField(service, "config", config);
        return service;
    }

    private void assertImagesEqual(AvatarUpdateRequest expected, AvatarUpdateRequest actual) {
        assertArrayEquals(expected.getLightAvatarImage(), actual.getLightAvatarImage());
        assertArrayEquals(expected.getLightAvatarImageOriginal(), actual.getLightAvatarImageOriginal());
        assertArrayEquals(expected.getDarkAvatarImage(), actual.getDarkAvatarImage());
        assertArrayEquals(expected.getDarkAvatarImageOriginal(), actual.getDarkAvatarImageOriginal());
        assertArrayEquals(expected.getLightHeaderImage(), actual.getLightHeaderImage());
        assertArrayEquals(expected.getDarkHeaderImage(), actual.getDarkHeaderImage());
    }

    private Stream<Component> components(Component root) {
        return Stream.concat(Stream.of(root), root.getChildren().flatMap(this::components));
    }

    private <T extends Component> T component(Component root, String id, Class<T> type) {
        return components(root).filter(component -> component.getId().orElse("").equals(id)).map(type::cast).findFirst().orElseThrow();
    }
}
