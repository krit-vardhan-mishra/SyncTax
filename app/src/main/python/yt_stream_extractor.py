"""
YouTube Stream URL Extractor using yt-dlp
Provides fallback streaming when NewPipeExtractor fails

IMPORTANT: On Android (Chaquopy), subprocess.Popen causes a fatal native crash
in _posixsubprocess.cpython-310.so because the Android sandbox forbids forking.
We monkey-patch subprocess BEFORE importing yt-dlp so that any ffprobe/ffmpeg
calls raise a clean Python exception instead of crashing the entire app.
"""

import json
import sys
import logging
import os

# ──────────────────────────────────────────────────────────────────────
# Monkey-patch subprocess to prevent native crashes on Android
# Must be done BEFORE importing yt-dlp, which imports subprocess at
# module level and probes for ffprobe/ffmpeg availability.
# ──────────────────────────────────────────────────────────────────────
import subprocess

_original_popen_init = subprocess.Popen.__init__

def _safe_popen_init(self, *args, **kwargs):
    """
    Drop-in replacement for Popen.__init__ that raises FileNotFoundError
    instead of calling the native _posixsubprocess which would crash on Android.
    yt-dlp catches FileNotFoundError when probing for ffprobe/ffmpeg and
    gracefully continues without post-processing.
    """
    raise FileNotFoundError(
        "subprocess.Popen is not available on Android (Chaquopy). "
        "ffprobe/ffmpeg calls are intentionally blocked."
    )

subprocess.Popen.__init__ = _safe_popen_init

# Now it's safe to import yt-dlp
import yt_dlp

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class YTDLPStreamExtractor:
    """Extracts stream URLs using yt-dlp as fallback when NewPipe fails"""

    def __init__(self):
        """Initialize yt-dlp extractor with Android-safe options"""
        self.ytdl_opts = {
            'quiet': True,
            'no_warnings': True,
            'extract_flat': False,
            # Request audio-only formats that don't need merging (avoids ffmpeg)
            'format': 'bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio/best',
            'noplaylist': True,
            'no_check_certificates': True,
            # Prevent all post-processing (all of it requires subprocess)
            'postprocessors': [],
            'prefer_ffmpeg': False,
            # Don't try to merge formats (requires ffmpeg subprocess)
            'merge_output_format': None,
            # Explicitly skip ffmpeg checks
            'ffmpeg_location': '/dev/null',
        }

    def get_stream_url(self, video_id):
        """
        Extract stream URL for a YouTube video using yt-dlp

        Args:
            video_id: YouTube video ID

        Returns:
            dict: {'success': bool, 'url': str, 'error': str}
        """
        try:
            url = f"https://www.youtube.com/watch?v={video_id}"
            logger.info(f"yt-dlp extracting stream for {video_id}")

            with yt_dlp.YoutubeDL(self.ytdl_opts) as ydl:
                # Extract info without downloading
                info = ydl.extract_info(url, download=False)

                if info and 'url' in info:
                    # Direct URL available (single format selected by format spec)
                    stream_url = info['url']
                    logger.info(f"yt-dlp got direct URL for {video_id}")
                    return {
                        'success': True,
                        'url': stream_url,
                        'format': info.get('format_id', 'unknown'),
                        'bitrate': info.get('abr', 0)
                    }

                if info and 'formats' in info:
                    # Manually pick best audio format
                    formats = info['formats']
                    # Only consider formats that have a direct URL and audio
                    audio_formats = [
                        f for f in formats
                        if f.get('url')
                        and f.get('acodec', 'none') != 'none'
                        and f.get('vcodec', 'none') == 'none'  # audio-only
                    ]

                    if not audio_formats:
                        # Fallback: any format with audio (may include video)
                        audio_formats = [
                            f for f in formats
                            if f.get('url')
                            and f.get('acodec', 'none') != 'none'
                        ]

                    if audio_formats:
                        # Sort by bitrate (highest first)
                        best_format = max(
                            audio_formats,
                            key=lambda f: f.get('abr', 0) or f.get('tbr', 0) or 0
                        )

                        stream_url = best_format.get('url')
                        if stream_url:
                            logger.info(
                                f"yt-dlp extracted stream URL for {video_id}: "
                                f"format={best_format.get('format_id')}, "
                                f"abr={best_format.get('abr', '?')}kbps"
                            )
                            return {
                                'success': True,
                                'url': stream_url,
                                'format': best_format.get('format_id', 'unknown'),
                                'bitrate': best_format.get('abr', 0)
                            }

                return {
                    'success': False,
                    'url': None,
                    'error': 'No audio formats found'
                }

        except Exception as e:
            logger.error(f"yt-dlp extraction failed for {video_id}: {str(e)}")
            return {
                'success': False,
                'url': None,
                'error': str(e)
            }


def extract_stream_url(video_id):
    """
    Main function to extract stream URL - called from Kotlin via Chaquopy.
    Returns a JSON string (not a dict) so Kotlin can parse it directly.
    """
    extractor = YTDLPStreamExtractor()
    result = extractor.get_stream_url(video_id)
    return json.dumps(result)


if __name__ == "__main__":
    if len(sys.argv) > 1:
        video_id = sys.argv[1]
        print(extract_stream_url(video_id))
    else:
        print(json.dumps({
            'success': False,
            'error': 'No video ID provided'
        }))