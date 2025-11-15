# Test YouTube InnerTube API
# PowerShell script to test search and stream URL fetching

$INNERTUBE_API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
$BASE_URL = "https://music.youtube.com/youtubei/v1"

# Search request body
$searchBody = @"
{
    "context": {
        "client": {
            "clientName": "WEB_REMIX",
            "clientVersion": "1.20240403.01.00",
            "gl": "US",
            "hl": "en"
        }
    },
    "query": "Sunflower Post Malone"
}
"@

Write-Host "🔍 Testing YouTube InnerTube API for: Sunflower Post Malone" -ForegroundColor Cyan
Write-Host ""

# Headers for the request
$headers = @{
    "Content-Type" = "application/json"
    "User-Agent" = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    "Accept" = "application/json"
    "X-Goog-Api-Format-Version" = "1"
    "X-YouTube-Client-Name" = "67"
    "X-YouTube-Client-Version" = "1.20240403.01.00"
    "Origin" = "https://music.youtube.com"
    "Referer" = "https://music.youtube.com/"
}

try {
    # Make search request
    Write-Host "Sending search request..." -ForegroundColor Yellow
    $searchUrl = "$BASE_URL/search?key=$INNERTUBE_API_KEY&prettyPrint=false"
    $response = Invoke-RestMethod -Uri $searchUrl -Method Post -Headers $headers -Body $searchBody -ErrorAction Stop
    
    # Parse results
    $contents = $response.contents.tabbedSearchResultsRenderer.tabs[0].tabRenderer.content.sectionListRenderer.contents
    
    $results = @()
    foreach ($section in $contents) {
        if ($section.musicShelfRenderer) {
            $items = $section.musicShelfRenderer.contents
            foreach ($item in $items) {
                $listItem = $item.musicResponsiveListItemRenderer
                if ($listItem) {
                    $videoId = $listItem.playlistItemData.videoId
                    $title = $listItem.flexColumns[0].musicResponsiveListItemFlexColumnRenderer.text.runs[0].text
                    $artist = $listItem.flexColumns[1].musicResponsiveListItemFlexColumnRenderer.text.runs[0].text
                    
                    $results += @{
                        id = $videoId
                        title = $title
                        artist = $artist
                    }
                    
                    if ($results.Count -ge 5) { break }
                }
            }
            break
        }
    }
    
    Write-Host "✅ Found $($results.Count) results:" -ForegroundColor Green
    Write-Host ""
    
    $index = 1
    foreach ($result in $results) {
        Write-Host "  $index. $($result.title) - $($result.artist)" -ForegroundColor White
        Write-Host "     ID: $($result.id)" -ForegroundColor Gray
        $index++
    }
    
    if ($results.Count -gt 0) {
        Write-Host ""
        Write-Host "🎵 Testing stream URL for first result..." -ForegroundColor Cyan
        
        # Get stream URL for first result
        $videoId = $results[0].id
        $playerBody = @"
{
    "context": {
        "client": {
            "clientName": "WEB_REMIX",
            "clientVersion": "1.20240403.01.00",
            "gl": "US",
            "hl": "en"
        }
    },
    "videoId": "$videoId"
}
"@
        
        $playerUrl = "$BASE_URL/player?key=$INNERTUBE_API_KEY&prettyPrint=false"
        $playerResponse = Invoke-RestMethod -Uri $playerUrl -Method Post -Headers $headers -Body $playerBody -ErrorAction Stop
        
        $status = $playerResponse.playabilityStatus.status
        
        if ($status -eq "OK") {
            $formats = $playerResponse.streamingData.adaptiveFormats
            $bestAudio = $null
            $bestBitrate = 0
            
            foreach ($format in $formats) {
                if ($format.mimeType -like "audio/*") {
                    if ($format.bitrate -gt $bestBitrate) {
                        $bestAudio = $format.url
                        $bestBitrate = $format.bitrate
                    }
                }
            }
            
            if ($bestAudio) {
                Write-Host "✅ Stream URL found! (bitrate: $bestBitrate)" -ForegroundColor Green
                Write-Host "   URL: $($bestAudio.Substring(0, [Math]::Min(100, $bestAudio.Length)))..." -ForegroundColor Gray
                Write-Host ""
                Write-Host "✅ SUCCESS! InnerTube API is working correctly!" -ForegroundColor Green
                Write-Host "   The Android app should be able to search and play online songs." -ForegroundColor Green
            } else {
                Write-Host "⚠️  No audio stream found in response" -ForegroundColor Yellow
            }
        } else {
            Write-Host "❌ Video not playable: $status" -ForegroundColor Red
            if ($playerResponse.playabilityStatus.reason) {
                Write-Host "   Reason: $($playerResponse.playabilityStatus.reason)" -ForegroundColor Red
            }
        }
    }
    
} catch {
    Write-Host "❌ Error occurred:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    Write-Host "Response:" -ForegroundColor Yellow
    Write-Host $_.ErrorDetails.Message
}
