# Copyright (C) 2025-2026 Abdalla Bushnaq
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
FROM johnguan/stable-diffusion-webui

ARG TILED_DIFFUSION_REVISION=22798f6822bc9c8a905b51da8954ee313b973331
RUN git clone https://github.com/pkuliyi2015/multidiffusion-upscaler-for-automatic1111.git \
        /stable-diffusion-webui/extensions/multidiffusion-upscaler-for-automatic1111 \
    && git -C /stable-diffusion-webui/extensions/multidiffusion-upscaler-for-automatic1111 \
        checkout -b kassandra-pinned "${TILED_DIFFUSION_REVISION}"
