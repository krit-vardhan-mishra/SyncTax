# FusionAgent

The FusionAgent merges recommendations from multiple agents to produce a single ranked list. It supports weighted fusion and diversity boosting to ensure recommendations are both relevant and varied.

Key behaviors:
- Receives scored lists from StatisticalAgent, CollaborativeFilteringAgent, and optionally Python-based scoring.
- Applies weights to each agent's scores and combines them using a weighted average.
- Adds a diversification step to penalize songs that are too similar to already selected items, improving the variety of the list.

Design considerations:
- Weights can be tuned to prioritize collaborative signals vs. statistical preferences.
- Diversity penalties are configurable by a decay factor.

Edge Cases:
- When only a single agent returns results, FusionAgent acts as a simple pass-through.
- Unavailable agents (e.g., Python model untrained) are skipped in fusion.
