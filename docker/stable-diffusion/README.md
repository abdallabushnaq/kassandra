# Stable Diffusion Docker Setup
Quick start guide for running Stable Diffusion WebUI in Docker.
## Quick Start
1. Pull image: `docker pull universonic/stable-diffusion-webui:full`
2. Start: `docker-compose up -d`
3. Monitor: `docker-compose logs -f`
4. Open: http://localhost:7860
## Commands
- Start: `docker-compose up -d`
- Stop: `docker-compose down`
- Logs: `docker-compose logs -f`
- Restart: `docker-compose restart`
## Configuration
- Port: 7860
- GPU: NVIDIA with CUDA
- Volumes: kassandra-sd-models, kassandra-sd-outputs
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
- API Docs: http://localhost:7860/docs
