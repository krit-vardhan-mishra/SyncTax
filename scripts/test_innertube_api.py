#!/usr/bin/env python3
"""
Test YouTube InnerTube API for searching and getting stream URLs.
This is the same API used by the Android app.
"""

import json
import requests

INNERTUBE_API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
BASE_URL = "https://music.youtube.com/youtubei/v1"
CLIENT_NAME = "WEB_REMIX"
CLIENT_VERSION = "1.20240403.01.00"

def build_context():
    return {
        "context": {
            "client": {
                "clientName": CLIENT_NAME,
                "clientVersion": CLIENT_VERSION,
                "gl": "US",
                "hl": "en"
            }
        }
    }

def search_music(query):
    """Search for music on YouTube Music"""
    url = f"{BASE_URL}/search?key={INNERTUBE_API_KEY}&prettyPrint=false"
    
    headers = {
        "Content-Type": "application/json",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept": "application/json",
        "X-Goog-Api-Format-Version": "1",
        "X-YouTube-Client-Name": "67",
        "X-YouTube-Client-Version": CLIENT_VERSION,
        "Origin": "https://music.youtube.com",
        "Referer": "https://music.youtube.com/"
    }
    
    body = build_context()
    body["query"] = query
    
    print(f"🔍 Searching for: {query}")
    response = requests.post(url, headers=headers, json=body, timeout=10)
    
    if response.status_code != 200:
        print(f"❌ Search failed with status {response.status_code}")
        print(response.text[:500])
        return []
    
    data = response.json()
    results = []
    
    # Navigate through the response structure
    try:
        contents = (data.get("contents", {})
                   .get("tabbedSearchResultsRenderer", {})
                   .get("tabs", [{}])[0]
                   .get("tabRenderer", {})
                   .get("content", {})
                   .get("sectionListRenderer", {})
                   .get("contents", []))
        
        for section in contents:
            music_shelf = section.get("musicShelfRenderer")
            if music_shelf:
                items = music_shelf.get("contents", [])
                for item in items[:5]:  # Limit to 5 results
                    list_item = item.get("musicResponsiveListItemRenderer")
                    if list_item:
                        # Parse result
                        video_id = (list_item.get("playlistItemData", {})
                                   .get("videoId"))
                        
                        flex_columns = list_item.get("flexColumns", [])
                        title = None
                        artist = None
                        
                        if flex_columns:
                            # Get title from first column
                            title_runs = (flex_columns[0]
                                        .get("musicResponsiveListItemFlexColumnRenderer", {})
                                        .get("text", {})
                                        .get("runs", []))
                            if title_runs:
                                title = title_runs[0].get("text")
                            
                            # Get artist from second column
                            if len(flex_columns) > 1:
                                artist_runs = (flex_columns[1]
                                             .get("musicResponsiveListItemFlexColumnRenderer", {})
                                             .get("text", {})
                                             .get("runs", []))
                                if artist_runs:
                                    artist = artist_runs[0].get("text")
                        
                        # Get thumbnail
                        thumbnail = (list_item.get("thumbnail", {})
                                   .get("musicThumbnailRenderer", {})
                                   .get("thumbnail", {})
                                   .get("thumbnails", [{}])[0]
                                   .get("url"))
                        
                        if video_id and title:
                            results.append({
                                "id": video_id,
                                "title": title,
                                "artist": artist,
                                "thumbnail": thumbnail
                            })
                break
        
        print(f"✅ Found {len(results)} results")
        for i, result in enumerate(results, 1):
            print(f"  {i}. {result['title']} - {result.get('artist', 'Unknown')}")
            print(f"     ID: {result['id']}")
        
        return results
    
    except Exception as e:
        print(f"❌ Error parsing search results: {e}")
        # Save response for debugging
        with open("search_response_debug.json", "w") as f:
            json.dump(data, f, indent=2)
        print("💾 Full response saved to search_response_debug.json")
        return []

def get_stream_url(video_id):
    """Get playable stream URL for a video"""
    url = f"{BASE_URL}/player?key={INNERTUBE_API_KEY}&prettyPrint=false"
    
    headers = {
        "Content-Type": "application/json",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept": "application/json",
        "X-Goog-Api-Format-Version": "1",
        "X-YouTube-Client-Name": "67",
        "X-YouTube-Client-Version": CLIENT_VERSION,
        "Origin": "https://music.youtube.com",
        "Referer": "https://music.youtube.com/"
    }
    
    body = build_context()
    body["videoId"] = video_id
    
    print(f"\n🎵 Getting stream URL for video: {video_id}")
    response = requests.post(url, headers=headers, json=body, timeout=10)
    
    if response.status_code != 200:
        print(f"❌ Player request failed with status {response.status_code}")
        return None
    
    data = response.json()
    
    # Check playability
    playability = data.get("playabilityStatus", {})
    status = playability.get("status")
    
    if status != "OK":
        print(f"❌ Video not playable: {status}")
        reason = playability.get("reason")
        if reason:
            print(f"   Reason: {reason}")
        return None
    
    # Get streaming data
    streaming_data = data.get("streamingData", {})
    adaptive_formats = streaming_data.get("adaptiveFormats", [])
    
    if not adaptive_formats:
        print("❌ No adaptive formats found")
        return None
    
    # Find best audio stream
    best_audio = None
    best_bitrate = 0
    
    for fmt in adaptive_formats:
        mime_type = fmt.get("mimeType", "")
        if mime_type.startswith("audio/"):
            url = fmt.get("url")
            bitrate = fmt.get("bitrate", 0)
            
            if url and bitrate > best_bitrate:
                best_audio = url
                best_bitrate = bitrate
    
    if best_audio:
        print(f"✅ Found audio stream (bitrate: {best_bitrate})")
        print(f"   URL: {best_audio[:100]}...")
        return best_audio
    else:
        print("❌ No audio stream found")
        # Save response for debugging
        with open("player_response_debug.json", "w") as f:
            json.dump(data, f, indent=2)
        print("💾 Full response saved to player_response_debug.json")
        return None

def main():
    # Test search for "Sunflower Post Malone"
    query = "Sunflower Post Malone Spider verse"
    results = search_music(query)
    
    if results:
        # Try to get stream URL for first result
        first_result = results[0]
        stream_url = get_stream_url(first_result["id"])
        
        if stream_url:
            print("\n✅ SUCCESS! InnerTube API is working correctly.")
            print(f"   Song: {first_result['title']}")
            print(f"   Artist: {first_result.get('artist', 'Unknown')}")
            print(f"   Stream available: Yes")
        else:
            print("\n⚠️  Search works but couldn't get stream URL")
    else:
        print("\n❌ Search returned no results")

if __name__ == "__main__":
    main()
