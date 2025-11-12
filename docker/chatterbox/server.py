"""
Chatterbox TTS REST API Server
Provides OpenAI-compatible TTS API endpoints for the Chatterbox TTS engine.
"""

import io
import logging
import os
import torch
import torchaudio as ta
import uvicorn
from fastapi import FastAPI, HTTPException, UploadFile
from fastapi.responses import StreamingResponse, JSONResponse
from pydantic import BaseModel, Field
from typing import Optional

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="Chatterbox TTS API",
    description="OpenAI-compatible Text-to-Speech API using Chatterbox",
    version="1.0.1"
)

# Global variables for lazy loading
tts_model = None
multilingual_model = None
device = None


def initialize_tts():
    """Initialize the Chatterbox TTS engines (lazy loading)"""
    global tts_model, multilingual_model, device
    if tts_model is not None:
        return

    logger.info("Initializing Chatterbox TTS engines...")

    try:
        from chatterbox.tts import ChatterboxTTS
        from chatterbox.mtl_tts import ChatterboxMultilingualTTS

        # Determine device - check environment variable first, then auto-detect
        env_device = os.getenv("DEVICE", "").lower()
        if env_device in ["cuda", "cpu", "mps"]:
            device = env_device
        else:
            # Auto-detect best available device
            if torch.cuda.is_available():
                device = "cuda"
            elif torch.backends.mps.is_available():
                device = "mps"
            else:
                device = "cpu"

        logger.info(f"Using device: {device}")

        # Initialize TTS models
        logger.info("Loading Chatterbox TTS model...")
        tts_model = ChatterboxTTS.from_pretrained(device=device)
        logger.info(f"Chatterbox TTS model loaded successfully (sample rate: {tts_model.sr})")

        logger.info("Loading Chatterbox Multilingual TTS model...")
        multilingual_model = ChatterboxMultilingualTTS.from_pretrained(device=device)
        logger.info(f"Chatterbox Multilingual TTS model loaded successfully (sample rate: {multilingual_model.sr})")

        logger.info("All Chatterbox TTS engines initialized successfully")

    except Exception as e:
        logger.error(f"Failed to initialize Chatterbox TTS: {e}", exc_info=True)
        raise


class SpeechRequest(BaseModel):
    """Request model for speech generation"""
    input: str = Field(..., description="Text to convert to speech")
    language: Optional[str] = Field(None, description="Language code (e.g., 'en', 'fr', 'es')")
    audio_prompt_path: Optional[str] = Field(None, description="Path to audio prompt for voice cloning")
    model: Optional[str] = Field("chatterbox", description="Model identifier: 'chatterbox' or 'multilingual'")
    response_format: Optional[str] = Field("wav", description="Audio format (currently only wav supported)")

    # Legacy parameters for compatibility (not used by Chatterbox but kept for API compatibility)
    temperature: Optional[float] = Field(0.7, ge=0.0, le=1.0, description="(Unused - kept for compatibility)")
    exaggeration: Optional[float] = Field(1.0, ge=0.0, le=2.0, description="(Unused - kept for compatibility)")
    cfg_weight: Optional[float] = Field(3.0, ge=0.0, le=10.0, description="(Unused - kept for compatibility)")
    voice: Optional[str] = Field(None, description="(Unused - use audio_prompt_path instead)")


class LanguageInfo(BaseModel):
    """Language information"""
    code: str
    name: str


class LanguagesResponse(BaseModel):
    """Response model for languages endpoint"""
    languages: list[LanguageInfo]


@app.on_event("startup")
async def startup_event():
    """Initialize TTS on startup"""
    logger.info("Starting Chatterbox TTS API server...")
    initialize_tts()


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    try:
        if tts_model is None:
            return {"status": "initializing", "message": "TTS engines are loading"}
        return {
            "status": "healthy",
            "device": device,
            "models_loaded": {
                "chatterbox": tts_model is not None,
                "multilingual": multilingual_model is not None
            },
            "message": "Chatterbox TTS is ready"
        }
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        raise HTTPException(status_code=503, detail=str(e))


@app.get("/languages", response_model=LanguagesResponse)
async def get_languages():
    """
    Get available languages
    
    Returns list of supported languages with their codes.
    The multilingual model supports 23 languages.
    """
    try:
        # Chatterbox Multilingual TTS supports 23 languages
        languages = [
            LanguageInfo(code="en", name="English"),
            LanguageInfo(code="es", name="Spanish"),
            LanguageInfo(code="fr", name="French"),
            LanguageInfo(code="de", name="German"),
            LanguageInfo(code="it", name="Italian"),
            LanguageInfo(code="pt", name="Portuguese"),
            LanguageInfo(code="pl", name="Polish"),
            LanguageInfo(code="tr", name="Turkish"),
            LanguageInfo(code="ru", name="Russian"),
            LanguageInfo(code="nl", name="Dutch"),
            LanguageInfo(code="cs", name="Czech"),
            LanguageInfo(code="ar", name="Arabic"),
            LanguageInfo(code="zh", name="Chinese"),
            LanguageInfo(code="ja", name="Japanese"),
            LanguageInfo(code="hu", name="Hungarian"),
            LanguageInfo(code="ko", name="Korean"),
            LanguageInfo(code="hi", name="Hindi"),
            LanguageInfo(code="uk", name="Ukrainian"),
            LanguageInfo(code="vi", name="Vietnamese"),
            LanguageInfo(code="th", name="Thai"),
            LanguageInfo(code="id", name="Indonesian"),
            LanguageInfo(code="ro", name="Romanian"),
            LanguageInfo(code="sv", name="Swedish"),
        ]

        return LanguagesResponse(languages=languages)

    except Exception as e:
        logger.error(f"Failed to get languages: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/v1/audio/speech")
async def generate_speech(request: SpeechRequest):
    """
    Generate speech from text
    
    OpenAI-compatible endpoint for text-to-speech generation.
    Returns audio in WAV format.
    
    Uses Chatterbox TTS for English text, or Multilingual TTS for other languages.
    """
    try:
        # Ensure TTS is initialized
        # if tts_model is None:
        initialize_tts()

        logger.info(f"Generating speech for text: '{request.input[:50]}...'")
        if request.language:
            logger.info(f"Language: {request.language}")
        if request.audio_prompt_path:
            logger.info(f"Using audio prompt: {request.audio_prompt_path}")

        # Determine which model to use
        use_multilingual = request.language and request.language.lower() != "en"

        if use_multilingual:
            # Use multilingual model for non-English languages
            logger.info(f"Using Multilingual TTS for language: {request.language}")
            model = multilingual_model

            # Generate speech with language specification
            wav = model.generate(request.input, language_id=request.language.lower())
            sample_rate = model.sr

        else:
            # Use standard Chatterbox TTS for English or unspecified language
            logger.info("Using standard Chatterbox TTS")
            model = tts_model

            # Generate speech with optional audio prompt
            if request.audio_prompt_path and os.path.exists(request.audio_prompt_path):
                logger.info(f"Using voice cloning with prompt: {request.audio_prompt_path}")
                wav = model.generate(request.input, audio_prompt_path=request.audio_prompt_path,
                                     exaggeration=request.exaggeration, cfg_weight=request.cfg_weight,
                                     temperature=request.temperature)
            else:
                logger.info(f"Using default voice")
                wav = model.generate(request.input, exaggeration=request.exaggeration, cfg_weight=request.cfg_weight,
                                     temperature=request.temperature)
            sample_rate = model.sr

        # Convert to WAV format in memory using torchaudio
        audio_buffer = io.BytesIO()

        # Ensure wav is on CPU and in the right format
        if isinstance(wav, torch.Tensor):
            wav_cpu = wav.cpu()
        else:
            wav_cpu = torch.tensor(wav)

        # Ensure it's 2D (channels, samples)
        if wav_cpu.dim() == 1:
            wav_cpu = wav_cpu.unsqueeze(0)

        # Save to buffer using torchaudio
        ta.save(
            audio_buffer,
            wav_cpu,
            sample_rate,
            format="wav"
        )
        audio_buffer.seek(0)

        logger.info(f"Speech generation successful (sample rate: {sample_rate} Hz)")

        # Return audio as streaming response
        return StreamingResponse(
            audio_buffer,
            media_type="audio/wav",
            headers={
                "Content-Disposition": "attachment; filename=speech.wav"
            }
        )

    except Exception as e:
        logger.error(f"Speech generation failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/v1/voice-references")
async def list_voice_references():
    """
    List available voice reference files

    Returns a list of WAV files that can be used as audio prompts for voice cloning.
    """
    voice_dir = "/opt/chatterbox/voices"

    # Create directory if it doesn't exist
    os.makedirs(voice_dir, exist_ok=True)

    try:
        # List all WAV files
        wav_files = []
        for filename in os.listdir(voice_dir):
            if filename.lower().endswith('.wav'):
                file_path = os.path.join(voice_dir, filename)
                file_size = os.path.getsize(file_path)
                file_mtime = os.path.getmtime(file_path)

                wav_files.append({
                    "filename": filename,
                    "path": file_path,
                    "size_bytes": file_size,
                    "modified_timestamp": file_mtime
                })

        return {
            "voice_references": wav_files,
            "count": len(wav_files),
            "directory": voice_dir
        }
    except Exception as e:
        logger.error(f"Error listing voice references: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/v1/voice-references")
async def upload_voice_reference(file: UploadFile):
    """
    Upload a voice reference WAV file

    The uploaded file will be saved in the voice references directory and can be used
    for voice cloning in speech synthesis requests.

    Requirements:
    - File must be in WAV format
    - Recommended: 10-30 seconds of clear speech
    - Recommended sample rate: 16kHz, 22.05kHz, or 44.1kHz
    """
    voice_dir = "/opt/chatterbox/voices"
    os.makedirs(voice_dir, exist_ok=True)

    # Validate file extension
    if not file.filename.lower().endswith('.wav'):
        raise HTTPException(
            status_code=400,
            detail="Only WAV files are supported. Please upload a .wav file."
        )

    # Sanitize filename to prevent directory traversal
    import re
    safe_filename = re.sub(r'[^\w\-\.]', '_', file.filename)
    file_path = os.path.join(voice_dir, safe_filename)

    try:
        # Read and save the file
        contents = await file.read()

        with open(file_path, "wb") as f:
            f.write(contents)

        file_size = os.path.getsize(file_path)

        logger.info(f"Voice reference uploaded: {safe_filename} ({file_size} bytes)")

        return {
            "filename": safe_filename,
            "path": file_path,
            "size_bytes": file_size,
            "message": "Voice reference uploaded successfully"
        }
    except Exception as e:
        logger.error(f"Error uploading voice reference: {e}", exc_info=True)
        # Clean up partial file if it exists
        if os.path.exists(file_path):
            os.unlink(file_path)
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/v1/voice-references/{filename}")
async def delete_voice_reference(filename: str):
    """
    Delete a voice reference file

    Removes the specified voice reference file from the server.
    """
    voice_dir = "/opt/chatterbox/voices"

    # Sanitize filename to prevent directory traversal
    import re
    safe_filename = re.sub(r'[^\w\-\.]', '_', filename)
    file_path = os.path.join(voice_dir, safe_filename)

    # Check if file exists
    if not os.path.exists(file_path):
        raise HTTPException(
            status_code=404,
            detail=f"Voice reference '{safe_filename}' not found"
        )

    # Check if it's actually a file (not a directory)
    if not os.path.isfile(file_path):
        raise HTTPException(
            status_code=400,
            detail=f"'{safe_filename}' is not a file"
        )

    try:
        os.unlink(file_path)
        logger.info(f"Voice reference deleted: {safe_filename}")

        return {
            "filename": safe_filename,
            "message": "Voice reference deleted successfully"
        }
    except Exception as e:
        logger.error(f"Error deleting voice reference: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/")
async def root():
    """Root endpoint with API information"""
    return {
        "name": "Chatterbox TTS API",
        "version": "1.0.1",
        "status": "running",
        "models": {
            "chatterbox": "English TTS with voice cloning support",
            "multilingual": "23 languages support"
        },
        "endpoints": {
            "health": "/health",
            "languages": "/languages",
            "speech": "/v1/audio/speech"
        },
        "docs": "/docs"
    }


if __name__ == "__main__":
    # Get configuration from environment
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", "4123"))

    logger.info(f"Starting server on {host}:{port}")

    uvicorn.run(
        app,
        host=host,
        port=port,
        log_level="info",
        access_log=True
    )
