"""Safe upload validation helpers."""
from __future__ import annotations

from pathlib import Path

from fastapi import UploadFile

from services.core.config import get_settings


IMAGE_MIME_TO_EXT = {
    "image/jpeg": {".jpg", ".jpeg"},
    "image/png": {".png"},
    "image/webp": {".webp"},
}
AUDIO_MIME_TO_EXT = {
    "audio/aac": {".aac"},
    "audio/flac": {".flac"},
    "audio/mpeg": {".mp3", ".mpga"},
    "audio/mp4": {".mp4", ".m4a"},
    "audio/ogg": {".ogg"},
    "application/ogg": {".ogg"},
    "audio/opus": {".opus"},
    "audio/wav": {".wav"},
    "audio/x-wav": {".wav"},
    "audio/webm": {".webm"},
}


async def read_limited_upload(
    upload: UploadFile,
    *,
    kind: str,
    max_bytes: int | None = None,
) -> bytes:
    settings = get_settings()
    limit = max_bytes or (settings.max_image_bytes if kind == "image" else settings.max_audio_bytes)
    mime = (upload.content_type or "").split(";", 1)[0].strip().lower()
    mapping = IMAGE_MIME_TO_EXT if kind == "image" else AUDIO_MIME_TO_EXT
    if mime not in mapping:
        raise ValueError(f"Unsupported {kind} content type.")
    extension = Path(upload.filename or "").suffix.lower()
    if extension and extension not in mapping[mime]:
        raise ValueError(f"Unsupported {kind} file extension.")

    chunks: list[bytes] = []
    total = 0
    while True:
        chunk = await upload.read(min(1024 * 1024, limit + 1))
        if not chunk:
            break
        total += len(chunk)
        if total > limit:
            raise OverflowError(f"{kind.title()} file exceeds the configured size limit.")
        chunks.append(chunk)
    return b"".join(chunks)
