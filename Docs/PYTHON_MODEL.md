# Python (Chaquopy) Model

This document describes the Python-based ML model used by the app. The Python module `music_ml.py` implements a basic k-means clustering and a z-score-based scorer that measures how 'close' a candidate is to a user's averaged feature vector.

Summary:
- `train_model` — Fits a k-means clustering model to feature vectors. Currently, it uses Euclidean distance and computes cluster centroids.
- `get_recommendation` — Given a target vector, returns the top scored song indices using a z-score similarity method and feature weights.
- `reset_model` — Reinitializes the internal model state and clears any in-memory centroids.

Considerations:
- This model is intentionally lightweight and performs in-memory computations for offline scenarios using Chaquopy.
- For production-grade recommendations, consider training larger models on server-side systems and using a model server or exporting model weights with persistence.

Limitations and future work:
- No persistent model storage currently exists — everything is in memory and is reset when the app process is restarted.
- Consider using advanced clustering / supervised models (e.g., KNN, LightGBM) and adding training persistence and versioning.
