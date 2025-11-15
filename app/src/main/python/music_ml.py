"""
Offline music recommendation using ensemble learning
Similar to behavioral_ml.py in OD-MAS
"""
import json
import math
from typing import List, Dict, Any

class SimpleClustering:
    """Simple K-means clustering for song grouping"""

    def __init__(self, n_clusters=5):
        self.n_clusters = n_clusters
        self.centroids = []

    def fit(self, X: List[List[float]]):
        if len(X) < self.n_clusters:
            self.centroids = X.copy()
            return

        # Initialize centroids randomly
        import random
        self.centroids = random.sample(X, self.n_clusters)

        # Simple k-means iterations
        for _ in range(10):
            clusters = [[] for _ in range(self.n_clusters)]

            # Assign points to nearest centroid
            for point in X:
                distances = [self._distance(point, c) for c in self.centroids]
                nearest = distances.index(min(distances))
                clusters[nearest].append(point)

            # Update centroids
            for i, cluster in enumerate(clusters):
                if cluster:
                    self.centroids[i] = [sum(dim) / len(cluster) for dim in zip(*cluster)]

    def predict_cluster(self, point: List[float]) -> int:
        if not self.centroids:
            return 0
        distances = [self._distance(point, c) for c in self.centroids]
        return distances.index(min(distances))

    def _distance(self, a: List[float], b: List[float]) -> float:
        return math.sqrt(sum((x - y) ** 2 for x, y in zip(a, b)))


class RecommendationScorer:
    """Score songs based on user listening patterns"""

    def __init__(self):
        self.user_mean = None
        self.user_std = None

    def fit(self, user_history: List[List[float]]):
        """Train on user's listening history"""
        if not user_history:
            return

        n_features = len(user_history[0])

        # Calculate mean and std for each feature
        self.user_mean = []
        self.user_std = []

        for i in range(n_features):
            values = [h[i] for h in user_history]
            mean = sum(values) / len(values)
            variance = sum((v - mean) ** 2 for v in values) / len(values)
            std = math.sqrt(variance) if variance > 0 else 1.0

            self.user_mean.append(mean)
            self.user_std.append(std)

    def score(self, song_features: List[float]) -> float:
        """Calculate recommendation score for a song"""
        if not self.user_mean or not self.user_std:
            return 0.5

        # Calculate similarity using normalized features
        similarity_scores = []
        for i, (feature, mean, std) in enumerate(zip(song_features, self.user_mean, self.user_std)):
            # Z-score based similarity
            z_score = abs((feature - mean) / std) if std > 0 else 0
            similarity = 1.0 / (1.0 + z_score)
            similarity_scores.append(similarity)

        # Weighted average (different weights for different features)
        weights = [0.15, 0.20, 0.10, 0.15, 0.10, 0.10, 0.10, 0.05, 0.05]
        weighted_score = sum(s * w for s, w in zip(similarity_scores, weights))

        return weighted_score


class MusicRecommendationML:
    """Main ML recommendation engine"""

    def __init__(self):
        self.scorer = RecommendationScorer()
        self.clustering = SimpleClustering(n_clusters=5)
        self.is_trained = False

    def train(self, user_history_json: str) -> Dict[str, Any]:
        """Train models on user listening history"""
        try:
            user_history = json.loads(user_history_json)

            if not user_history or len(user_history) < 5:
                return {
                    "success": False,
                    "message": "Insufficient training data (need at least 5 samples)"
                }

            # Extract features from history
            features_list = [item["features"] for item in user_history]

            # Train recommendation scorer
            self.scorer.fit(features_list)

            # Train clustering model
            self.clustering.fit(features_list)

            self.is_trained = True

            return {
                "success": True,
                "samples_trained": len(user_history),
                "message": "Training completed successfully"
            }

        except Exception as e:
            return {
                "success": False,
                "message": f"Training failed: {str(e)}"
            }

    def recommend(self, song_features_json: str) -> Dict[str, Any]:
        """Generate recommendation score for a song"""
        if not self.is_trained:
            return {
                "score": 50.0,
                "confidence": 0.3,
                "cluster": -1,
                "message": "Model not trained yet"
            }

        try:
            song_features = json.loads(song_features_json)

            # Get base score from scorer
            base_score = self.scorer.score(song_features) * 100

            # Get cluster information
            cluster_id = self.clustering.predict_cluster(song_features)

            # Calculate confidence based on training data
            confidence = min(0.95, 0.3 + (len(self.scorer.user_mean or []) * 0.05))

            return {
                "score": base_score,
                "confidence": confidence,
                "cluster": cluster_id,
                "message": "Recommendation generated successfully"
            }

        except Exception as e:
            return {
                "score": 50.0,
                "confidence": 0.3,
                "cluster": -1,
                "message": f"Recommendation failed: {str(e)}"
            }

    def get_status(self) -> Dict[str, Any]:
        """Get current model status"""
        return {
            "is_trained": self.is_trained,
            "has_scorer": self.scorer.user_mean is not None,
            "n_clusters": self.clustering.n_clusters
        }


# Global instance
_ml_engine = MusicRecommendationML()


def train_model(user_history_json: str) -> str:
    """Train the recommendation model"""
    result = _ml_engine.train(user_history_json)
    return json.dumps(result)


def get_recommendation(song_features_json: str) -> str:
    """Get recommendation score for a song"""
    result = _ml_engine.recommend(song_features_json)
    return json.dumps(result)


def get_model_status() -> str:
    """Get current model status"""
    result = _ml_engine.get_status()
    return json.dumps(result)


def reset_model() -> str:
    """Reset the ML engine state"""
    global _ml_engine
    _ml_engine = MusicRecommendationML()
    return json.dumps({"success": True, "message": "Model reset"})