# CollaborativeFilteringAgent

The CollaborativeFilteringAgent uses a vector database to store song feature vectors and applies a nearest-neighbor strategy to identify songs similar to a user's recent listening history.

## Key Behaviors

- Builds a vector index for songs using feature vectors from `SongFeatures`.
- Computes cosine similarity between a target vector built from user history and the vectors in the database.
- Returns top-K closest songs ordered by similarity score.

## VectorDatabase

- **Implementation**: In-memory LRU cache
- **Entry Limit**: 5,000 vectors
- **Similarity Metric**: Cosine similarity

## Optimization

- A small in-memory vector DB is used for quick inference; if dataset grows, consider using an on-disk or optimized ANN index (e.g., FAISS or HNSW).
- Computed vectors and similarities are cached to speed up repeated queries.
- Chunked processing (500 songs per batch) for large libraries.

## Edge Cases

- Cold-start is mitigated by falling back to the StatisticalAgent when history is insufficient.
- Sparse vectors handle missing feature components gracefully by treating them as zeros.

---

**Last Updated**: November 30, 2025  
**Status**: ✅ Production Ready
