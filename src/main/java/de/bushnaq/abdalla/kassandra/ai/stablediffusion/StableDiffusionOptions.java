/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.ai.stablediffusion;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the options/configuration returned by the Stable Diffusion WebUI API /sdapi/v1/options endpoint.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StableDiffusionOptions {
    private Integer             CLIP_stop_at_last_layers;
    private Integer             eta_noise_seed_delta;
    // For unknown/extra fields
    private Map<String, Object> other = new HashMap<>();
    private String              outdir_img2img_samples;
    private String              outdir_samples;
    private String              outdir_txt2img_samples;
    private String              samples_format;
    private Boolean             samples_save;
    private String              sd_model_checkpoint;
    // Add more fields as needed
    private String              sd_vae;

    @JsonAnyGetter
    public Map<String, Object> getOther() {
        return other;
    }

    @JsonAnySetter
    public void setOther(String key, Object value) {
        other.put(key, value);
    }

    @Override
    public String toString() {
        try {
            JsonMapper mapper = new JsonMapper();
            mapper.writerWithDefaultPrettyPrinter();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (Exception e) {
            return super.toString();
        }
    }
}
