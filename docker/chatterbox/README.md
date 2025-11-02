# Chatterbox TTS Docker Container

A containerized REST API server for Chatterbox TTS (Text-to-Speech), providing OpenAI-compatible endpoints for speech
synthesis with GPU acceleration.

## Overview

This Docker container runs a FastAPI-based REST server that wraps the Chatterbox TTS engine, making it accessible via
HTTP API. It's designed to work with the Java client `ChatterboxTTS.java` in the kassandra application.

## Features

- 🎯 **OpenAI-compatible API** - Drop-in replacement for OpenAI TTS endpoints
- 🚀 **GPU Acceleration** - Optimized for NVIDIA GPUs (RTX 2080 Ti tested)
- 🔊 **High-Quality Speech** - Leverages Chatterbox TTS for natural-sounding voices
- 🌍 **Multi-language Support** - 16+ languages supported
- 📦 **Docker-based** - Easy deployment and isolation
- 🔧 **Configurable** - Control temperature, exaggeration, and CFG weight
- 📊 **Health Checks** - Built-in monitoring and readiness checks
- ✅ **Pinned Dependencies** - Uses exact versions from official Chatterbox TTS pyproject.toml

## Prerequisites

### Required

- Docker Desktop (with WSL2 backend on Windows)
- Docker Compose
- 8GB+ RAM recommended
- Internet connection (for initial model download)

### For GPU Support

- NVIDIA GPU (RTX 2080 Ti or similar)
- NVIDIA GPU drivers (latest recommended)
- NVIDIA Container Toolkit

To verify GPU support:

```bash
nvidia-smi
docker run --rm --gpus all nvidia/cuda:11.8.0-base-ubuntu22.04 nvidia-smi
```

## Quick Start

### 1. Build the Container

```bash
cd E:\github\kassandra\docker\chatterbox
chatterbox-helper.bat build
```

This will:

- Build the Docker image with Python 3.11 and all dependencies
- Install Chatterbox TTS and FastAPI
- Set up GPU support

**Note**: First build may take 10-15 minutes.

### 2. Start the Service

```bash
chatterbox-helper.bat start
```

The service will start on `http://localhost:4123`. Initial startup may take 1-2 minutes for model loading.

### 3. Test the API

```bash
chatterbox-helper.bat test
```

This will:

- Check service health
- List available languages
- Generate a test audio file (`test-output.wav`)

### 4. View Logs

```bash
chatterbox-helper.bat logs
```

Press `Ctrl+C` to exit log view.

## API Endpoints

### POST /v1/audio/speech

Generate speech from text.

**Request:**

```json
{
  "input": "Hello, world!",
  "temperature": 0.7,
  "exaggeration": 1.0,
  "cfg_weight": 3.0
}
```

**Parameters:**

- `input` (string, required) - Text to convert to speech
- `temperature` (float, 0.0-1.0, default: 0.7) - Sampling temperature (higher = more varied)
- `exaggeration` (float, 0.0-2.0, default: 1.0) - Expression exaggeration
- `cfg_weight` (float, 0.0-10.0, default: 3.0) - Classifier-free guidance weight

**Response:** Audio file in WAV format

**Example (curl):**

```bash
curl -X POST http://localhost:4123/v1/audio/speech \
  -H "Content-Type: application/json" \
  -d "{\"input\":\"Hello world\",\"temperature\":0.7,\"exaggeration\":1.0,\"cfg_weight\":3.0}" \
  --output speech.wav
```

**Example (Java):**

```java
byte[] audio = ChatterboxTTS.generateSpeech("Hello world", 0.7f, 1.0f, 3.0f);
```

### GET /languages

Get list of supported languages.

**Response:**

```json
{
  "languages": [
    {
      "code": "en",
      "name": "English"
    },
    {
      "code": "es",
      "name": "Spanish"
    },
    {
      "code": "fr",
      "name": "French"
    },
    ...
  ]
}
```

**Example (curl):**

```bash
curl http://localhost:4123/languages
```

**Example (Java):**

```java
String[] languages = ChatterboxTTS.getLanguages();
```

### GET /health

Health check endpoint.

**Response:**

```json
{
  "status": "healthy",
  "device": "cuda",
  "message": "Chatterbox TTS is ready"
}
```

### GET /

API information and documentation.

**Interactive Docs:** `http://localhost:4123/docs`

## Helper Script Commands

The `chatterbox-helper.bat` script provides convenient container management:


| Command   | Description                                  |
| --------- | -------------------------------------------- |
| `start`   | Start the container                          |
| `stop`    | Stop the container                           |
| `restart` | Restart the container                        |
| `logs`    | View container logs (follow mode)            |
| `build`   | Build container from scratch                 |
| `test`    | Test API endpoints and generate sample audio |
| `status`  | Show container status and recent logs        |
| `shell`   | Open bash shell in container                 |

## Configuration

### Environment Variables

Edit `docker-compose-chatterbox.yml` to customize:

```yaml
environment:
  - HOST=0.0.0.0          # Bind address
  - PORT=4123             # Server port
  - DEVICE=cuda           # Device: 'cuda' or 'cpu'
  - CUDA_VISIBLE_DEVICES=0 # GPU index (if multiple GPUs)
```

### Port Configuration

To change the port (e.g., to 5000):

1. Edit `docker-compose-chatterbox.yml`:

   ```yaml
   ports:
     - "5000:4123"  # External:Internal
   ```
2. Rebuild and restart:

   ```bash
   chatterbox-helper.bat build
   chatterbox-helper.bat start
   ```

### CPU-Only Mode

If you don't have a GPU or want to use CPU:

1. Edit `docker-compose-chatterbox.yml`:

   ```yaml
   environment:
     - DEVICE=cpu
   # Comment out or remove:
   # deploy:
   #   resources:
   #     reservations:
   #       devices:
   #         - driver: nvidia
   ```
2. Rebuild and restart

**Note**: CPU mode will be significantly slower (10-20x) than GPU mode.

## Troubleshooting

### Container won't start

**Check logs:**

```bash
chatterbox-helper.bat logs
```

**Common issues:**

- Port 4123 already in use: Change port in docker-compose file
- GPU driver issues: Verify `nvidia-smi` works
- Out of memory: Increase Docker memory limit or use CPU mode

### Health check failing

**Check status:**

```bash
chatterbox-helper.bat status
```

**Possible causes:**

- Model still loading (wait 1-2 minutes)
- Out of GPU memory (restart container or use CPU)
- Dependency issues (rebuild container)

### Audio generation fails

**Check:**

1. Service is healthy: `curl http://localhost:4123/health`
2. Text is not empty
3. Parameters are within valid ranges
4. Container has sufficient memory

### GPU not detected

**Verify GPU setup:**

```bash
# Check host GPU
nvidia-smi

# Check Docker GPU access
docker run --rm --gpus all nvidia/cuda:11.8.0-base-ubuntu22.04 nvidia-smi
```

**Install NVIDIA Container Toolkit:**

- Windows: Ensure Docker Desktop has GPU support enabled
- Linux: Install `nvidia-docker2` package

### Performance issues

**GPU mode optimization:**

- Ensure CUDA drivers are up to date
- Monitor GPU usage with `nvidia-smi -l 1`
- Check for thermal throttling

**CPU mode:**

- Reduce concurrent requests
- Consider upgrading to GPU mode
- Use shorter text inputs

## Development

### Hot-Reload (No Rebuild Required!)

The server code is mounted as a volume for fast development:

**Edit server.py without rebuilding:**

```bash
# 1. Edit server.py in your IDE
#    File: E:\github\kassandra\docker\chatterbox\server.py

# 2. Restart container (NOT rebuild!)
chatterbox-helper.bat restart    # Takes ~30 seconds

# 3. Test your changes
chatterbox-helper.bat test
```

**What this means:**

- ✅ Fix bugs in seconds, not minutes
- ✅ Adjust Chatterbox API calls on-the-fly
- ✅ Test different parameters quickly
- ✅ No need to rebuild the entire container
- ✅ Model cache persists between restarts

**Full details:** See `HOT_RELOAD_GUIDE.md`

### Modifying the Server

The server code is mounted as a volume for development:

1. Edit `server.py` locally
2. Restart the container: `chatterbox-helper.bat restart`
3. Changes take effect immediately (no rebuild needed)

### Accessing Container Shell

```bash
chatterbox-helper.bat shell
```

Inside container:

```bash
# Check Python packages
pip list

# Test Chatterbox directly
python
>>> from chatterbox import ChatterboxTTS
>>> tts = ChatterboxTTS()
```

### Building from Source

To modify dependencies:

1. Edit `requirements.txt`
2. Rebuild: `chatterbox-helper.bat build`
3. Restart: `chatterbox-helper.bat start`

## Integration with Java Client

The container is designed to work with `ChatterboxTTS.java`:

```java
// Java client automatically connects to http://localhost:4123
byte[] audio = ChatterboxTTS.generateSpeech(
                "Hello from Java!",
                0.7f,  // temperature
                1.0f,  // exaggeration
                3.0f   // cfg_weight
        );

String[] languages = ChatterboxTTS.getLanguages();
```

No additional configuration needed if using default port 4123.

## Model Information

Chatterbox TTS uses pre-trained models that are automatically downloaded on first use:

- Models are cached in Docker volume `chatterbox-cache`
- First run may take longer (model download)
- Subsequent runs use cached models
- Volume persists across container restarts

**Model size:** ~1-2 GB (approximate)

## Performance Benchmarks

Approximate generation times (RTX 2080 Ti):

- Short text (10 words): ~1-2 seconds
- Medium text (50 words): ~3-5 seconds
- Long text (200 words): ~10-15 seconds

CPU mode is approximately 10-20x slower.

## License

This container setup is part of the kassandra application.
Chatterbox TTS is subject to its own license terms.

## Support

For issues specific to:

- **Container setup**: Check this README and troubleshooting section
- **Chatterbox TTS**: Visit [Chatterbox repository](https://github.com/chatterbox-tts)
- **Java integration**: Check `ChatterboxTTS.java` documentation

## Version History

- **1.0.0** (2025-10-27)
  - Initial release
  - FastAPI-based REST server
  - GPU support (RTX 2080 Ti)
  - OpenAI-compatible endpoints
  - Docker Compose setup
  - Windows helper script
