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

package de.bushnaq.abdalla.kassandra.util;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.select.Select;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.*;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.CozyStylePrompt;
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.rest.api.FeatureApi;
import de.bushnaq.abdalla.kassandra.rest.api.ProductApi;
import de.bushnaq.abdalla.kassandra.rest.api.SprintApi;
import de.bushnaq.abdalla.kassandra.ui.dialog.ImagePromptDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Checks shared Cozy styling without contacting a Stable Diffusion server.
 */
@Tag("UnitTest")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AvatarStyleTest extends AbstractTestUtil {
    private       AvatarService          avatarService;
    private       StableDiffusionService diffusion;
    private final TiledDiffusionOptions  options = TiledDiffusionOptions.forHeader(4096, 256);

    private void assertSharedGenerationStyle(AvatarService service) {
        var calls = mockingDetails(service).getInvocations().stream()
                .filter(call -> call.getMethod().getName().contains("HeaderWithFallback"))
                .filter(call -> Arrays.stream(call.getMethod().getParameterTypes()).noneMatch(type -> type == TiledDiffusionOptions.class))
                .toList();
        assertEquals(2, calls.size());
        Object[]        firstArguments = calls.getFirst().getArguments();
        CozyStylePrompt style          = assertInstanceOf(CozyStylePrompt.class, firstArguments[firstArguments.length - 1]);
        for (var call : calls) {
            Object[] arguments = call.getArguments();
            assertSame(style, arguments[arguments.length - 1]);
        }
    }

    @Test
    void dialogDisplaysAConcreteStyleAndRandomButtonSelectsAnotherStyle() {
        ImagePromptDialog dialog = new ImagePromptDialog(avatarService, diffusion, "A garden", "cube", null,
                (light, dark, header, darkHeader) -> {
                }, null, null, null, null,
                "A garden at night", "Avoid text", "Avoid text", "Panoramic garden", "Panoramic garden at night");
        Select<?> select = assertInstanceOf(Select.class, ReflectionTestUtils.getField(dialog, "styleSelect"));
        assertNotNull(select.getValue());
        assertFalse(select.isEmptySelectionAllowed());
        assertEquals(CozyStylePrompt.values().length, select.getListDataView().getItemCount());

        ReflectionTestUtils.invokeMethod(select, "setValue", CozyStylePrompt.MARY_BLAIR);
        assertSame(CozyStylePrompt.MARY_BLAIR, select.getValue());
        Button randomButton = assertInstanceOf(Button.class, ReflectionTestUtils.getField(dialog, "randomStyleButton"));
        assertEquals(ImagePromptDialog.ID_RANDOM_STYLE_BUTTON, randomButton.getId().orElseThrow());
        randomButton.click();
        assertNotNull(select.getValue());
        assertTrue(select.getListDataView().getItems().anyMatch(style -> style == select.getValue()));
        ReflectionTestUtils.invokeMethod(dialog, "beginGeneration");
        assertFalse(select.isEnabled());
        assertFalse(randomButton.isEnabled());
        ReflectionTestUtils.invokeMethod(dialog, "finishGeneration");
        assertTrue(select.isEnabled());
        assertTrue(randomButton.isEnabled());
        var styleRow   = select.getParent().orElseThrow();
        var layout     = styleRow.getParent().orElseThrow().getChildren().toList();
        int styleIndex = layout.indexOf(styleRow);
        assertEquals("preview-row", layout.get(styleIndex - 1).getId().orElseThrow());
        assertEquals("light-header-column", layout.get(styleIndex + 1).getChildren().findFirst().orElseThrow().getId().orElseThrow());
    }

    @Test
    void entityGeneratorPassesOneExplicitStyleOnlyToHeaders() {
        PersistingEntityGenerator generator = new PersistingEntityGenerator();
        AvatarService             service   = spy(avatarService);
        generator.avatarService = service;
        generator.productApi    = mock(ProductApi.class);
        generator.featureApi    = mock(FeatureApi.class);
        generator.sprintApi     = mock(SprintApi.class);
        when(generator.productApi.persist(any(Product.class))).thenAnswer(call -> call.getArgument(0));
        when(generator.featureApi.persist(any(Feature.class))).thenAnswer(call -> call.getArgument(0));
        when(generator.sprintApi.persist(any(Sprint.class))).thenAnswer(call -> call.getArgument(0));

        generator.addProduct("A garden");
        assertSharedGenerationStyle(service);
        clearInvocations(service);
        generator.addFeature(new Version(), "A garden feature");
        assertSharedGenerationStyle(service);
        clearInvocations(service);
        generator.addSprint(new Feature(), "A garden sprint");
        assertSharedGenerationStyle(service);
    }

    @Test
    void generationFailureUsesTheRequestedStyleForFallback() throws StableDiffusionException {
        when(diffusion.text2ImgWithOriginal(anyString(), anyString(), anyInt(), anyInt(), any(), anyLong(), anyDouble(), any()))
                .thenThrow(new StableDiffusionException("Generation failed"));
        GeneratedImageResult header = avatarService.generateLightHeaderWithFallback("A garden", "Avoid text", "cube", null, options, CozyStylePrompt.MARY_BLAIR);

        assertSame(CozyStylePrompt.MARY_BLAIR, header.getStyle());
        verify(diffusion).generateDefaultHeader("cube", false);
    }

    @Test
    void legacyAvatarPromptsLoseCozyStylesButRetainTheirSeed() throws StableDiffusionException {
        GeneratedImageResult legacyLight = result("Legacy prompt", 42);
        legacyLight.setStyle(CozyStylePrompt.MARY_BLAIR);
        GeneratedImageResult dark = avatarService.generateDarkAvatar("A garden at night, " + CozyStylePrompt.MARY_BLAIR.getPrompt(),
                "Avoid text", legacyLight, null);
        GeneratedImageResult light = avatarService.generateLightAvatar("A garden, " + CozyStylePrompt.BRAMBLY_HEDGE.getPrompt(), "Avoid text", null);

        assertNull(dark.getStyle());
        assertNull(light.getStyle());
        assertEquals("A garden at night", dark.getPrompt());
        assertEquals("A garden", light.getPrompt());
        assertEquals(42, dark.getSeed());
    }

    @Test
    void noStyleCallsChooseRandomlyAndDarkVariantsInherit() {
        GeneratedImageResult light      = avatarService.generateLightAvatarWithFallback("A garden", "Avoid text", "cube");
        GeneratedImageResult dark       = avatarService.generateDarkAvatarWithFallback("A garden at night", "Avoid text", light, "cube");
        GeneratedImageResult header     = avatarService.generateLightHeaderWithFallback("Panoramic garden", "Avoid text", "cube");
        GeneratedImageResult darkHeader = avatarService.generateDarkHeaderWithFallback("Panoramic garden at night", "Avoid text", header, "cube");

        assertNull(light.getStyle());
        assertNull(dark.getStyle());
        assertNotNull(header.getStyle());
        assertSame(header.getStyle(), darkHeader.getStyle());
        assertTrue(header.getPrompt().endsWith(header.getStyle().getPrompt()));
    }

    @BeforeEach
    void prepareGeneration() throws StableDiffusionException {
        diffusion     = mock(StableDiffusionService.class);
        avatarService = new AvatarService();
        ReflectionTestUtils.setField(avatarService, "config", new StableDiffusionConfig());
        ReflectionTestUtils.setField(avatarService, "stableDiffusionService", diffusion);
        when(diffusion.isAvailable()).thenReturn(true);
        when(diffusion.text2ImgWithOriginal(anyString(), anyString(), anyInt(), any(), anyLong(), anyDouble()))
                .thenAnswer(call -> result(call.getArgument(0), call.getArgument(4)));
        when(diffusion.text2ImgWithOriginal(anyString(), anyString(), anyInt(), anyInt(), any(), anyLong(), anyDouble(), any()))
                .thenAnswer(call -> result(call.getArgument(0), call.getArgument(5)));
        when(diffusion.generateDefaultHeader(anyString(), anyBoolean())).thenAnswer(call -> result("Placeholder", -1));
        when(diffusion.generateDefaultAvatar(anyString(), anyString())).thenAnswer(call -> result("Placeholder", -1));
        when(diffusion.generateDefaultDarkAvatar(anyString(), anyString())).thenAnswer(call -> result("Placeholder", -1));
    }

    @Test
    void rejectsBlankHeaderDescriptions() {
        assertThrows(IllegalArgumentException.class, () -> avatarService.generateLightHeaderWithFallback(" ", "Avoid text", "cube"));
    }

    private GeneratedImageResult result(String prompt, long seed) {
        GeneratedImageResult result = new GeneratedImageResult(new byte[]{1}, prompt, new byte[]{1}, seed == -1 ? 1234 : seed);
        result.setNegativePrompt("Avoid text");
        return result;
    }

    @Test
    void savedPromptsDoNotAccumulateStyles() {
        CozyStylePrompt      first    = CozyStylePrompt.BRAMBLY_HEDGE;
        CozyStylePrompt      second   = CozyStylePrompt.MARY_BLAIR;
        GeneratedImageResult original = avatarService.generateLightHeaderWithFallback("Panoramic garden", "Avoid text", "cube", null, options, first);
        GeneratedImageResult repeated = avatarService.generateLightHeaderWithFallback(original.getPrompt(), "Avoid text", "cube", null, options, first);
        GeneratedImageResult changed  = avatarService.generateLightHeaderWithFallback(repeated.getPrompt(), "Avoid text", "cube", null, options, second);

        assertEquals(original.getPrompt(), repeated.getPrompt());
        assertEquals("Panoramic garden, " + second.getPrompt(), changed.getPrompt());
    }

    @Test
    void stylesOnlyHeadersAndKeepsAvatarsPlain() throws StableDiffusionException {
        CozyStylePrompt      style      = CozyStylePrompt.BRAMBLY_HEDGE;
        GeneratedImageResult light      = avatarService.generateLightAvatar("A garden, white background", "Avoid text", null);
        GeneratedImageResult dark       = avatarService.generateDarkAvatar("A garden, dark background", "Avoid text", light, null);
        GeneratedImageResult header     = avatarService.generateLightHeaderWithFallback("Panoramic garden", "Avoid text", "cube", null, style);
        GeneratedImageResult darkHeader = avatarService.generateDarkHeaderWithFallback("Panoramic garden at night", "Avoid text", header, "cube", null, style);

        for (GeneratedImageResult image : new GeneratedImageResult[]{header, darkHeader}) {
            assertSame(style, image.getStyle());
            assertTrue(image.getPrompt().endsWith(", " + style.getPrompt()));
        }
        assertNull(light.getStyle());
        assertNull(dark.getStyle());
        assertEquals("A garden, white background", light.getPrompt());
        assertEquals("A garden, dark background", dark.getPrompt());
        assertEquals(light.getSeed(), dark.getSeed());
        assertEquals(header.getSeed(), darkHeader.getSeed());
        verify(diffusion).text2ImgWithOriginal(eq("Panoramic garden at night, " + style.getPrompt()), eq("Avoid text"),
                eq(1024), eq(AvatarService.HEADER_GENERATION_HEIGHT), isNull(), eq(header.getSeed()), eq(avatarService.getConfig().getCfgScale()),
                argThat(canvas -> canvas.getImageWidth() == AvatarService.HEADER_WIDTH
                        && canvas.getImageHeight() == AvatarService.HEADER_HEIGHT));
    }

    @Test
    void unavailableGenerationKeepsStyleForMatchingFallbacks() {
        when(diffusion.isAvailable()).thenReturn(false);
        GeneratedImageResult light      = avatarService.generateLightAvatarWithFallback("A garden", "Avoid text", "cube");
        GeneratedImageResult dark       = avatarService.generateDarkAvatarWithFallback("A garden at night", "Avoid text", light, "cube");
        GeneratedImageResult header     = avatarService.generateLightHeaderWithFallback("Panoramic garden", "Avoid text", "cube");
        GeneratedImageResult darkHeader = avatarService.generateDarkHeaderWithFallback("Panoramic garden at night", "Avoid text", header, "cube");

        assertNull(light.getStyle());
        assertNull(dark.getStyle());
        assertNotNull(header.getStyle());
        assertSame(header.getStyle(), darkHeader.getStyle());
        assertEquals("Panoramic garden, " + header.getStyle().getPrompt(), header.getPrompt());
        verify(diffusion).generateDefaultHeader("cube", true);
    }

    @Test
    void userDialogDoesNotShowHeaderStyleControls() {
        ImagePromptDialog dialog = new ImagePromptDialog(avatarService, diffusion, "A portrait", "user",
                (light, dark) -> {
                }, null, null, "A portrait at night", "Avoid text", "Avoid text");
        Select<?> select = assertInstanceOf(Select.class, ReflectionTestUtils.getField(dialog, "styleSelect"));
        assertTrue(select.getParent().orElseThrow().getParent().isEmpty());
    }
}
