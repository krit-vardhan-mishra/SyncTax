"""
Offline music recommendation using ensemble learning
Similar to behavioral_ml.py in OD-MAS

Enhanced with algorithms from RECOMMENDATION_LOGIC_NEW.md:
- Cosine similarity for content-based filtering
- Time decay for relevance scoring
- Skip penalty calculation
- Confidence scoring with implicit feedback
"""
import json
import math
import os
from typing import List, Dict, Any


def cosine_similarity(a: List[float], b: List[float]) -> float:
    """
    Calculate cosine similarity between two vectors.
    Formula: cos(θ) = (A·B) / (|A||B|)
    """
    if len(a) != len(b) or len(a) == 0:
        return 0.0
    
    dot_product = sum(x * y for x, y in zip(a, b))
    magnitude_a = math.sqrt(sum(x * x for x in a))
    magnitude_b = math.sqrt(sum(x * x for x in b))
    
    if magnitude_a == 0 or magnitude_b == 0:
        return 0.0
    
    return max(0.0, min(1.0, dot_product / (magnitude_a * magnitude_b)))


def time_decay(days_since: float, half_life: float = 23.0) -> float:
    """
    Calculate time decay factor using exponential decay.
    Formula: relevance(t) = e^(-λ * days)
    where λ = ln(2) / half_life (~0.03 for 23-day half-life)
    """
    lambda_decay = math.log(2) / half_life
    return math.exp(-lambda_decay * days_since)


def skip_penalty(listen_time: float, total_duration: float) -> float:
    """
    Calculate skip penalty using exponential decay.
    Formula: penalty = e^(-listen_time / song_duration)
    Shorter listens = bigger penalty
    """
    if total_duration <= 0:
        return 1.0
    completion_rate = listen_time / total_duration
    return math.exp(-completion_rate)


def confidence_score(completion_rate: float, skip_rate: float, play_count: int,
                     alpha: float = 0.5, beta: float = 0.3, gamma: float = 0.2) -> float:
    """
    Calculate confidence score using implicit feedback.
    Formula: confidence = α * completion_rate + β * (1 - skip_rate) + γ * normalized_play_count
    """
    # Normalize play count (saturates around 100 plays)
    normalized_plays = min(play_count / 100.0, 1.0)
    
    return (alpha * completion_rate + 
            beta * (1.0 - skip_rate) + 
            gamma * normalized_plays)


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

    def to_dict(self):
        return {
            "n_clusters": self.n_clusters,
            "centroids": self.centroids
        }

    def from_dict(self, data):
        self.n_clusters = data.get("n_clusters", 5)
        self.centroids = data.get("centroids", [])


class RecommendationScorer:
    """
    Score songs based on user listening patterns.
    
    Enhanced with algorithms from RECOMMENDATION_LOGIC_NEW.md:
    - Cosine similarity for vector comparison
    - Diversity penalty to avoid repetition
    - Skip rate penalty
    - Time decay for recency
    """

    def __init__(self):
        self.user_mean = None
        self.user_std = None
        self.user_profile_vector = None  # Average of liked songs

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
        
        # Create user profile vector (average of all history)
        self.user_profile_vector = self.user_mean.copy()

    def score(self, song_features: List[float]) -> float:
        """
        Calculate recommendation score for a song.
        
        Score formula:
        score = weighted_similarity + cosine_sim_bonus - skip_penalty
        """
        if not self.user_mean or not self.user_std:
            return 0.5

        # 1. Calculate Z-score based similarity
        similarity_scores = []
        for i, (feature, mean, std) in enumerate(zip(song_features, self.user_mean, self.user_std)):
            z_score = abs((feature - mean) / std) if std > 0 else 0
            similarity = 1.0 / (1.0 + z_score)
            similarity_scores.append(similarity)

        # 2. Weighted average with updated weights for 14 features
        weights = [
            0.10,  # playFrequency
            0.18,  # avgCompletionRate - most important for engagement
            0.12,  # skipRate - penalize skipped songs (inverted)
            0.10,  # recencyScore
            0.08,  # timeOfDayMatch
            0.08,  # genreAffinity
            0.08,  # artistAffinity
            0.04,  # consecutivePlays
            0.04,  # sessionContext
            0.06,  # durationScore
            0.05,  # albumAffinity
            0.03,  # releaseYearScore
            0.02,  # songPopularity
            0.02   # tempoEnergy
        ]
        
        # Pad weights if feature count doesn't match
        if len(similarity_scores) < len(weights):
            weights = weights[:len(similarity_scores)]
        elif len(similarity_scores) > len(weights):
            remaining_weight = 1.0 - sum(weights)
            extra_features = len(similarity_scores) - len(weights)
            weights.extend([remaining_weight / extra_features] * extra_features)
        
        weighted_score = sum(s * w for s, w in zip(similarity_scores, weights))
        
        # 3. Add cosine similarity bonus (content-based)
        if self.user_profile_vector and len(song_features) == len(self.user_profile_vector):
            cos_sim = cosine_similarity(song_features, self.user_profile_vector)
            weighted_score = weighted_score * 0.7 + cos_sim * 0.3
        
        # 4. Apply skip penalty if skipRate (index 2) is high
        if len(song_features) > 2:
            skip_rate = song_features[2]
            skip_penalty_factor = 1.0 - (skip_rate * 0.5)  # Max 50% penalty
            weighted_score *= skip_penalty_factor

        return max(0.0, min(1.0, weighted_score))

    def score_with_context(self, song_features: List[float], 
                          recent_songs: List[List[float]] = None) -> float:
        """
        Calculate score with diversity consideration.
        Penalizes songs too similar to recently played.
        """
        base_score = self.score(song_features)
        
        if not recent_songs:
            return base_score
        
        # Calculate diversity penalty
        diversity_penalty = 0.0
        for recent in recent_songs[-5:]:  # Last 5 songs
            sim = cosine_similarity(song_features, recent)
            diversity_penalty += sim
        
        # Normalize penalty
        if recent_songs:
            diversity_penalty /= min(len(recent_songs), 5)
        
        # Formula: final_score = base_score * (1 - diversity_factor * penalty)
        diversity_factor = 0.3  # Max 30% diversity penalty
        return base_score * (1.0 - diversity_factor * diversity_penalty)

    def to_dict(self):
        return {
            "user_mean": self.user_mean,
            "user_std": self.user_std,
            "user_profile_vector": self.user_profile_vector
        }

    def from_dict(self, data):
        self.user_mean = data.get("user_mean")
        self.user_std = data.get("user_std")
        self.user_profile_vector = data.get("user_profile_vector")


class MusicRecommendationML:
    """
    Main ML recommendation engine.
    
    Enhanced with algorithms from RECOMMENDATION_LOGIC_NEW.md:
    - Multi-feature scoring with skip penalties
    - Cosine similarity for content-based filtering
    - Diversity-aware recommendations
    - Confidence scoring with implicit feedback
    """

    def __init__(self, model_dir=None):
        self.scorer = RecommendationScorer()
        self.clustering = SimpleClustering(n_clusters=5)
        self.is_trained = False
        self.model_dir = model_dir or "."
        self.recent_recommendations = []  # For diversity tracking
        self._load_model()

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
            self._save_model()

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
        """
        Generate recommendation score for a song.
        
        Uses enhanced scoring with:
        - Base score from weighted features
        - Cosine similarity bonus
        - Skip penalty
        - Diversity consideration
        """
        if not self.is_trained:
            return {
                "score": 50.0,
                "confidence": 0.3,
                "cluster": -1,
                "diversity_score": 1.0,
                "message": "Model not trained yet"
            }

        try:
            song_features = json.loads(song_features_json)

            # Get base score with diversity consideration
            base_score = self.scorer.score_with_context(
                song_features, 
                self.recent_recommendations[-10:]  # Consider last 10 recommendations
            ) * 100

            # Get cluster information
            cluster_id = self.clustering.predict_cluster(song_features)

            # Calculate confidence using implicit feedback formula
            # Extract relevant features (playFrequency=0, avgCompletionRate=1, skipRate=2)
            play_freq = song_features[0] if len(song_features) > 0 else 0.5
            completion = song_features[1] if len(song_features) > 1 else 0.5
            skip_rate = song_features[2] if len(song_features) > 2 else 0.0
            
            confidence = confidence_score(
                completion_rate=completion,
                skip_rate=skip_rate,
                play_count=int(play_freq * 100)  # Denormalize
            )
            
            # Clamp confidence
            confidence = min(0.95, max(0.3, confidence))
            
            # Track this recommendation for diversity
            self.recent_recommendations.append(song_features)
            if len(self.recent_recommendations) > 50:
                self.recent_recommendations.pop(0)

            return {
                "score": base_score,
                "confidence": confidence,
                "cluster": cluster_id,
                "diversity_score": 1.0 - (skip_rate * 0.5),
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

    def _save_model(self):
        """Save model state to file"""
        try:
            model_data = {
                "is_trained": self.is_trained,
                "scorer": self.scorer.to_dict(),
                "clustering": self.clustering.to_dict()
            }
            with open(os.path.join(self.model_dir, "ml_model.json"), "w") as f:
                json.dump(model_data, f)
        except Exception as e:
            print(f"Failed to save model: {e}")

    def _load_model(self):
        """Load model state from file"""
        try:
            model_file = os.path.join(self.model_dir, "ml_model.json")
            if os.path.exists(model_file):
                with open(model_file, "r") as f:
                    model_data = json.load(f)
                self.is_trained = model_data.get("is_trained", False)
                self.scorer.from_dict(model_data.get("scorer", {}))
                self.clustering.from_dict(model_data.get("clustering", {}))
        except Exception as e:
            print(f"Failed to load model: {e}")


# Global instance
_ml_engine = None

def _get_engine(model_dir=None):
    global _ml_engine
    if _ml_engine is None:
        _ml_engine = MusicRecommendationML(model_dir)
    return _ml_engine


def train_model(user_history_json: str, model_dir=None) -> str:
    """Train the recommendation model"""
    engine = _get_engine(model_dir)
    result = engine.train(user_history_json)
    return json.dumps(result)


def get_recommendation(song_features_json: str, model_dir=None) -> str:
    """Get recommendation score for a song"""
    engine = _get_engine(model_dir)
    result = engine.recommend(song_features_json)
    return json.dumps(result)


def get_model_status(model_dir=None) -> str:
    """Get current model status"""
    engine = _get_engine(model_dir)
    result = engine.get_status()
    return json.dumps(result)


def reset_model(model_dir=None) -> str:
    """Reset the ML engine state"""
    global _ml_engine
    if _ml_engine:
        try:
            model_file = os.path.join(_ml_engine.model_dir, "ml_model.json")
            if os.path.exists(model_file):
                os.remove(model_file)
        except:
            pass
    _ml_engine = MusicRecommendationML(model_dir)
    return json.dumps({"success": True, "message": "Model reset"})