# Stable Diffusion Docker Setup

Quick start guide for running Stable Diffusion WebUI in Docker.

## Quick Start

1. Build image including Tiled Diffusion: `docker compose build stable-diffusion`
2. Start: `docker compose up -d stable-diffusion`
3. Monitor: `docker-compose logs -f`
4. Open: http://localhost:7861

## Commands

- Start: `docker-compose up -d`
- Stop: `docker-compose down`
- Logs: `docker-compose logs -f`
- Restart: `docker-compose restart`

## Configuration

- Port: 7861 (container port 7860)
- GPU: NVIDIA with CUDA
- Model volumes: `sd-models-stable-diffusion`, `sd-models-lora` (must already exist)

## Tiled Diffusion headers

The Dockerfile installs a pinned revision of
[multidiffusion-upscaler-for-automatic1111](https://github.com/pkuliyi2015/multidiffusion-upscaler-for-automatic1111).
Rebuild and recreate the service after upgrading an existing installation:

```bash
docker compose up -d --build stable-diffusion
curl -s http://localhost:7861/sdapi/v1/scripts
```

The `txt2img` scripts must include `tiled diffusion`. Kassandra checks this before
each tiled request and reports a missing extension rather than silently accepting
an ordinary square image. Existing avatar and project-icon requests do not enable
the extension.

Header generation uses a 1024x1024 base request with canvas-size overwrite enabled.
The browser measures the navigation/breadcrumb band; the generated height is 256,
and width is `ceil(displayWidth * 256 / displayHeight / 16) * 16` (minimum 256,
maximum 16384). For a 2560x76 band this gives 8624x256. Calls without browser
dimensions default to 4096x256. Very wide aspect ratios exceeding the maximum are
reported rather than silently distorted.

Settings: MultiDiffusion, latent tiles 256x256, overlap 48, tile batch size 4.
Tile values are latent units (eight image pixels per latent unit); the extension
clamps tiles to the actual canvas. Region Prompt Control and noise inversion are
disabled. Light/dark generation shares dimensions and seed. The complete canvas
is stored without the former center-strip crop; CSS scales it proportionally to
the current header band, allowing minor centered cropping when its dimensions change.

## Memory Settings

Edit docker-compose.yml for your VRAM:

- 6-8GB: `--lowvram`
- 8-12GB: `--medvram` (default)
- 12GB+: remove memory flags

## Troubleshooting

- Check logs: `docker-compose logs`
- Verify GPU: `docker run --rm --gpus all nvidia/cuda:11.8.0-base-ubuntu22.04 nvidia-smi`
- Port conflict: Change port in docker-compose.yml

## Performance (RTX 2090 Ti)

- 512x512: 3-7 seconds
- 768x768: 8-12 seconds

## Resources

- WebUI: https://github.com/AUTOMATIC1111/stable-diffusion-webui
- API Docs: http://localhost:7861/docs

## Adding Model

- rum add-model.bat after editing
