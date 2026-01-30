"""
YouTube Stream URL Extractor using yt-dlp
Provides fallback streaming when NewPipeExtractor fails
"""
import yt_dlp
import json
import sys
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class YTDLPStreamExtractor:
    """Extracts stream URLs using yt-dlp as fallback when NewPipe fails"""

    def __init__(self):
        """Initialize yt-dlp extractor"""
        self.ytdl_opts = {
            'quiet': True,
            'no_warnings': True,
            'extract_flat': False,
            'format': 'bestaudio/best',
            'noplaylist': True,
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

            with yt_dlp.YoutubeDL(self.ytdl_opts) as ydl:
                # Extract info without downloading
                info = ydl.extract_info(url, download=False)

                if info and 'formats' in info:
                    # Get best audio format
                    formats = info['formats']
                    audio_formats = [f for f in formats if f.get('acodec') != 'none']

                    if audio_formats:
                        # Sort by bitrate (highest first)
                        best_format = max(audio_formats,
                                        key=lambda f: f.get('abr', 0) or f.get('tbr', 0) or 0)

                        stream_url = best_format.get('url')
                        if stream_url:
                            logger.info(f"yt-dlp extracted stream URL for {video_id}")
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
    Main function to extract stream URL - called from Kotlin
    """
    extractor = YTDLPStreamExtractor()
    result = extractor.get_stream_url(video_id)

    # Return JSON for Kotlin parsing
    print(json.dumps(result))
    return result


if __name__ == "__main__":
    if len(sys.argv) > 1:
        video_id = sys.argv[1]
        extract_stream_url(video_id)
    else:
        print(json.dumps({
            'success': False,
            'error': 'No video ID provided'
        }))