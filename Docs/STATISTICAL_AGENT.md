# StatisticalAgent

The StatisticalAgent is a light-weight scoring agent that computes a weighted score for songs based on extracted features such as tempo, energy, danceability, valence, popularity, and other normalized numerical values. The agent uses a configurable weight vector and applies a sigmoid or logistic normalization to scale the scores to the 0-1 range.

Key behaviors:
- Weights per feature can be tuned to emphasize specific user preferences.
- Feature normalization is applied before scoring to avoid bias from different feature ranges.
- Outputs a list of scored song IDs and confidence values representing normalized scores.

Edge Cases:
- Missing features are skipped or imputed to default values.
- Extreme values are clipped to avoid numeric overflow in sigmoid.
