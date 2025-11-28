"""
YTMusic Recommender - Uses ytmusicapi to get song-only recommendations
This module provides song search and recommendation functionality using YouTube Music API
"""

from ytmusicapi import YTMusic
import json
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class YTMusicRecommender:
    """Handles YouTube Music API interactions for song recommendations"""
    
    def __init__(self):
        """Initialize YTMusic client"""
        try:
            self.yt = YTMusic()
            logger.info("YTMusic client initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize YTMusic: {e}")
            self.yt = None
    
    def search_songs(self, query, limit=20):
        """
        Search for songs only (no videos, playlists, or albums)
        
        Args:
            query: Search query string
            limit: Maximum number of results (default 20)
            
        Returns:
            List of song dictionaries with keys: videoId, title, artist, album, duration, thumbnail
        """
        if not self.yt:
            logger.error("YTMusic client not initialized")
            return []
        
        try:
            results = self.yt.search(query, filter='songs', limit=limit)
            songs = []
            
            for item in results:
                song = {
                    'videoId': item.get('videoId', ''),
                    'title': item.get('title', 'Unknown Title'),
                    'artist': ', '.join([a['name'] for a in item.get('artists', [])]) or 'Unknown Artist',
                    'album': item.get('album', {}).get('name', 'Unknown Album') if item.get('album') else 'Unknown Album',
                    'duration': item.get('duration', '0:00'),
                    'thumbnail': item.get('thumbnails', [{}])[-1].get('url', '') if item.get('thumbnails') else '',
                    'videoType': item.get('videoType', 'MUSIC_VIDEO_TYPE_ATV')
                }
                
                # Only include audio tracks (songs), not music videos
                if song['videoType'] in ['MUSIC_VIDEO_TYPE_ATV', 'MUSIC_VIDEO_TYPE_OFFICIAL_SOURCE_MUSIC']:
                    songs.append(song)
            
            logger.info(f"Found {len(songs)} songs for query: {query}")
            return songs
            
        except Exception as e:
            logger.error(f"Search failed for query '{query}': {e}")
            return []
    
    def search_albums(self, query, limit=20):
        """
        Search for albums only
        
        Args:
            query: Search query string
            limit: Maximum number of results (default 20)
            
        Returns:
            List of album dictionaries with keys: browseId, title, artist, year, thumbnail
        """
        if not self.yt:
            logger.error("YTMusic client not initialized")
            return []
        
        try:
            results = self.yt.search(query, filter='albums', limit=limit)
            albums = []
            
            for item in results:
                album = {
                    'browseId': item.get('browseId', ''),
                    'title': item.get('title', 'Unknown Album'),
                    'artist': ', '.join([a['name'] for a in item.get('artists', [])]) or 'Unknown Artist',
                    'year': item.get('year', ''),
                    'thumbnail': item.get('thumbnails', [{}])[-1].get('url', '') if item.get('thumbnails') else '',
                    'type': item.get('resultType', 'album')
                }
                albums.append(album)
            
            logger.info(f"Found {len(albums)} albums for query: {query}")
            return albums
            
        except Exception as e:
            logger.error(f"Album search failed for query '{query}': {e}")
            return []
    
    def search_artists(self, query, limit=10):
        """
        Search for artists on YouTube Music
        
        Args:
            query: Search query string
            limit: Maximum number of results (default 10)
            
        Returns:
            List of artist dictionaries with browseId, name, thumbnail
        """
        if not self.yt:
            logger.error("YTMusic client not initialized")
            return []
        
        try:
            results = self.yt.search(query=query, filter='artists', limit=limit)
            
            if not results:
                logger.warning(f"No artists found for query: {query}")
                return []
            
            artists = []
            for item in results:
                artist = {
                    'browseId': item.get('browseId', ''),
                    'name': item.get('artist', 'Unknown Artist'),
                    'thumbnail': item.get('thumbnails', [{}])[-1].get('url', '') if item.get('thumbnails') else '',
                    'type': item.get('resultType', 'artist'),
                    'subscribers': item.get('subscribers', '')
                }
                artists.append(artist)
            
            logger.info(f"Found {len(artists)} artists for query: {query}")
            return artists
            
        except Exception as e:
            logger.error(f"Artist search failed for query '{query}': {e}")
            return []
    
    def get_album_details(self, browse_id):
        """
        Get album details including songs list
        
        Args:
            browse_id: Album browseId from search results
            
        Returns:
            Dictionary with album details and songs list
        """
        if not self.yt:
            logger.error("YTMusic client not initialized")
            return None
        
        try:
            album = self.yt.get_album(browseId=browse_id)
            
            if not album:
                logger.warning(f"No album details found for browseId: {browse_id}")
                return None
            
            songs = []
            for track in album.get('tracks', []):
                song = {
                    'videoId': track.get('videoId', ''),
                    'title': track.get('title', 'Unknown Title'),
                    'artist': ', '.join([a['name'] for a in track.get('artists', [])]) or 'Unknown Artist',
                    'album': album.get('title', 'Unknown Album'),
                    'duration': str(track.get('duration', '0:00')),
                    'thumbnail': track.get('thumbnails', [{}])[-1].get('url', '') if track.get('thumbnails') else ''
                }
                songs.append(song)
            
            result = {
                'browseId': browse_id,
                'title': album.get('title', 'Unknown Album'),
                'artist': ', '.join([a['name'] for a in album.get('artists', [])]) or 'Unknown Artist',
                'year': str(album.get('year', '')),
                'thumbnail': album.get('thumbnails', [{}])[-1].get('url', '') if album.get('thumbnails') else '',
                'trackCount': album.get('trackCount', len(songs)),
                'duration': album.get('duration', ''),
                'songs': songs
            }
            
            logger.info(f"Retrieved album details: {result['title']} with {len(songs)} songs")
            return result
            
        except Exception as e:
            logger.error(f"Failed to get album details for browseId '{browse_id}': {e}")
            return None
    
    def get_artist_details(self, browse_id):
        """
        Get artist details including top songs
        
        Args:
            browse_id: Artist browseId from search results
            
        Returns:
            Dictionary with artist details and songs list
        """
        if not self.yt:
            logger.error("YTMusic client not initialized")
            return None
        
        try:
            artist = self.yt.get_artist(channelId=browse_id)
            
            if not artist:
                logger.warning(f"No artist details found for browseId: {browse_id}")
                return None
            
            songs = []
            # Get songs from the artist (top tracks, singles, etc.)
            if 'songs' in artist and 'results' in artist['songs']:
                for track in artist['songs']['results'][:25]:  # Top 25 songs
                    song = {
                        'videoId': track.get('videoId', ''),
                        'title': track.get('title', 'Unknown Title'),
                        'artist': ', '.join([a['name'] for a in track.get('artists', [])]) or artist.get('name', 'Unknown Artist'),
                        'album': track.get('album', {}).get('name', '') if track.get('album') else '',
                        'duration': str(track.get('duration', '0:00')),
                        'thumbnail': track.get('thumbnails', [{}])[-1].get('url', '') if track.get('thumbnails') else ''
                    }
                    songs.append(song)
            
            result = {
                'browseId': browse_id,
                'name': artist.get('name', 'Unknown Artist'),
                'description': artist.get('description', ''),
                'thumbnail': artist.get('thumbnails', [{}])[-1].get('url', '') if artist.get('thumbnails') else '',
                'subscribers': artist.get('subscribers', ''),
                'songs': songs
            }
            
            logger.info(f"Retrieved artist details: {result['name']} with {len(songs)} songs")
            return result
            
        except Exception as e:
            logger.error(f"Failed to get artist details for browseId '{browse_id}': {e}")
            return None
    
    def get_song_recommendations(self, video_id, limit=25):
        """
        Get song recommendations based on a video ID
        Uses get_watch_playlist to get radio recommendations
        
        Args:
            video_id: YouTube video ID of a song
            limit: Maximum number of recommendations (default 25)
            
        Returns:
            List of recommended song dictionaries
        """
        if not self.yt:
            logger.error("YTMusic client not initialized")
            return []
        
        try:
            # Get watch playlist with radio parameter for recommendations
            watch_playlist = self.yt.get_watch_playlist(videoId=video_id, limit=limit, radio=True)
            
            if not watch_playlist or 'tracks' not in watch_playlist:
                logger.warning(f"No recommendations found for video_id: {video_id}")
                return []
            
            tracks = watch_playlist['tracks']
            recommendations = []
            
            for track in tracks:
                song = {
                    'videoId': track.get('videoId', ''),
                    'title': track.get('title', 'Unknown Title'),
                    'artist': ', '.join([a['name'] for a in track.get('artists', [])]) or 'Unknown Artist',
                    'album': track.get('album', {}).get('name', 'Unknown Album') if track.get('album') else 'Unknown Album',
                    'duration': str(track.get('length', '0:00')),
                    'thumbnail': track.get('thumbnail', [{}])[-1].get('url', '') if track.get('thumbnail') else '',
                    'videoType': track.get('videoType', 'MUSIC_VIDEO_TYPE_ATV')
                }
                
                # Filter: Only include audio tracks (songs), exclude music videos
                if song['videoType'] in ['MUSIC_VIDEO_TYPE_ATV', 'MUSIC_VIDEO_TYPE_OFFICIAL_SOURCE_MUSIC']:
                    recommendations.append(song)
            
            logger.info(f"Found {len(recommendations)}/{len(tracks)} song recommendations for video_id: {video_id}")
            return recommendations
            
        except Exception as e:
            logger.error(f"Failed to get recommendations for video_id '{video_id}': {e}")
            return []
    
    def get_recommendations_for_query(self, query, limit=25):
        """
        Get song recommendations based on a search query
        First searches for songs, then gets recommendations based on the first result
        
        Args:
            query: Search query string
            limit: Maximum number of recommendations (default 25)
            
        Returns:
            List of recommended song dictionaries
        """
        if not self.yt:
            logger.error("YTMusic client not initialized")
            return []
        
        try:
            # First, search for songs matching the query
            search_results = self.search_songs(query, limit=5)
            
            if not search_results:
                logger.warning(f"No songs found for query: {query}")
                return []
            
            # Use the first song to get recommendations
            first_song_id = search_results[0]['videoId']
            logger.info(f"Using song '{search_results[0]['title']}' (ID: {first_song_id}) for recommendations")
            
            recommendations = self.get_song_recommendations(first_song_id, limit)
            return recommendations
            
        except Exception as e:
            logger.error(f"Failed to get recommendations for query '{query}': {e}")
            return []


# Module-level functions for easy calling from Kotlin via Chaquopy

_recommender = None


def initialize():
    """Initialize the YTMusic recommender"""
    global _recommender
    try:
        _recommender = YTMusicRecommender()
        return "YTMusic initialized successfully"
    except Exception as e:
        logger.error(f"Initialization failed: {e}")
        return f"Initialization failed: {e}"


def search_songs(query, limit=20):
    """
    Search for songs
    Returns JSON string of song list
    """
    global _recommender
    if not _recommender:
        initialize()
    
    try:
        songs = _recommender.search_songs(query, limit)
        return json.dumps(songs)
    except Exception as e:
        logger.error(f"search_songs error: {e}")
        return json.dumps([])


def search_albums(query, limit=20):
    """
    Search for albums
    Returns JSON string of album list
    """
    global _recommender
    if not _recommender:
        initialize()
    
    try:
        albums = _recommender.search_albums(query, limit)
        return json.dumps(albums)
    except Exception as e:
        logger.error(f"search_albums error: {e}")
        return json.dumps([])


def search_artists(query, limit=10):
    """
    Search for artists
    Returns JSON string of artist list
    """
    global _recommender
    if not _recommender:
        initialize()
    
    try:
        artists = _recommender.search_artists(query, limit)
        return json.dumps(artists)
    except Exception as e:
        logger.error(f"search_artists error: {e}")
        return json.dumps([])


def get_song_recommendations(video_id, limit=25):
    """
    Get recommendations for a specific video ID
    Returns JSON string of recommendation list
    """
    global _recommender
    if not _recommender:
        initialize()
    
    try:
        recommendations = _recommender.get_song_recommendations(video_id, limit)
        return json.dumps(recommendations)
    except Exception as e:
        logger.error(f"get_song_recommendations error: {e}")
        return json.dumps([])


def get_recommendations_for_query(query, limit=25):
    """
    Get recommendations based on a search query
    Returns JSON string of recommendation list
    """
    global _recommender
    if not _recommender:
        initialize()
    
    try:
        recommendations = _recommender.get_recommendations_for_query(query, limit)
        return json.dumps(recommendations)
    except Exception as e:
        logger.error(f"get_recommendations_for_query error: {e}")
        return json.dumps([])


def get_album_details(browse_id):
    """
    Get album details including songs list
    Returns JSON string of album details with songs
    """
    global _recommender
    if not _recommender:
        initialize()
    
    try:
        album = _recommender.get_album_details(browse_id)
        return json.dumps(album) if album else json.dumps(None)
    except Exception as e:
        logger.error(f"get_album_details error: {e}")
        return json.dumps(None)


def get_artist_details(browse_id):
    """
    Get artist details including top songs
    Returns JSON string of artist details with songs
    """
    global _recommender
    if not _recommender:
        initialize()
    
    try:
        artist = _recommender.get_artist_details(browse_id)
        return json.dumps(artist) if artist else json.dumps(None)
    except Exception as e:
        logger.error(f"get_artist_details error: {e}")
        return json.dumps(None)
