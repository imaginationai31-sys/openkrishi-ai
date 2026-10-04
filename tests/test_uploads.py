from io import BytesIO

import pytest
from fastapi import UploadFile

from services.core.uploads import read_limited_upload


def upload(filename: str, content_type: str, data: bytes = b"test-data") -> UploadFile:
    return UploadFile(
        filename=filename,
        file=BytesIO(data),
        headers={"content-type": content_type},
    )


@pytest.mark.anyio
async def test_image_upload_accepts_supported_extension():
    result = await read_limited_upload(
        upload("leaf.jpg", "image/jpeg", b"image-bytes"),
        kind="image",
        max_bytes=20,
    )
    assert result == b"image-bytes"


@pytest.mark.anyio
async def test_audio_upload_accepts_mime_parameter_without_extension():
    result = await read_limited_upload(
        upload("voice.mp3", "audio/mpeg", b"audio-bytes"),
        kind="audio",
        max_bytes=20,
    )
    assert result == b"audio-bytes"


@pytest.mark.anyio
async def test_upload_rejects_unsupported_mime_type():
    with pytest.raises(ValueError, match="Unsupported image content type"):
        await read_limited_upload(
            upload("leaf.gif", "image/gif"),
            kind="image",
            max_bytes=20,
        )


@pytest.mark.anyio
async def test_upload_rejects_mismatched_extension():
    with pytest.raises(ValueError, match="Unsupported image file extension"):
        await read_limited_upload(
            upload("leaf.png", "image/jpeg"),
            kind="image",
            max_bytes=20,
        )


@pytest.mark.anyio
async def test_upload_allows_missing_filename_extension():
    result = await read_limited_upload(
        upload("", "image/png", b"png-bytes"),
        kind="image",
        max_bytes=20,
    )
    assert result == b"png-bytes"


@pytest.mark.anyio
async def test_upload_rejects_content_over_limit():
    with pytest.raises(OverflowError, match="Image file exceeds"):
        await read_limited_upload(
            upload("large.jpg", "image/jpeg", b"12345"),
            kind="image",
            max_bytes=4,
        )
