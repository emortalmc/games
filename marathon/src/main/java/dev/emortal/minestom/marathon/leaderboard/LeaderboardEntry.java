package dev.emortal.minestom.marathon.leaderboard;

import java.util.UUID;

public record LeaderboardEntry(UUID uuid, String name, int score, long ticks, long submittedAt) {}
