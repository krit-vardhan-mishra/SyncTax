# Python (Chaquopy) Model

This document describes the Python-based ML model used by the app. The Python module `music_ml.py` implements a basic k-means clustering and a z-score-based scorer that measures how 'close' a candidate is to a user's averaged feature vector.

## Technology Stack

- **Chaquopy**: Version 16.1.0 (Python 3.9+ runtime)
- **Python Packages**:
  - `yt-dlp`: 2025.11.12 (audio extraction)
  - `mutagen`: Metadata embedding
  - `ytmusicapi`: YouTube Music API
  - `requests`, `urllib3`: HTTP handling

## Core Functions

- `train_model` — Fits a k-means clustering model to feature vectors. Currently, it uses Euclidean distance and computes cluster centroids.
- `get_recommendation` — Given a target vector, returns the top scored song indices using a z-score similarity method and feature weights.
- `reset_model` — Reinitializes the internal model state and clears any in-memory centroids.

## Processing Optimization

- **Batch Size**: 500 songs per chunk for ML processing
- **VectorDatabase**: LRU cache with 5,000 entry limit
- **Cache TTL**: Quick Picks cached for 5 minutes

## Considerations

- This model is intentionally lightweight and performs in-memory computations for offline scenarios using Chaquopy.
- For production-grade recommendations, consider training larger models on server-side systems and using a model server or exporting model weights with persistence.

## Limitations and Future Work

- No persistent model storage currently exists — everything is in memory and is reset when the app process is restarted.
- Consider using advanced clustering / supervised models (e.g., KNN, LightGBM) and adding training persistence and versioning.

---

**Last Updated**: November 30, 2025  
**Chaquopy Version**: 16.1.0  
**yt-dlp Version**: 2025.11.12
