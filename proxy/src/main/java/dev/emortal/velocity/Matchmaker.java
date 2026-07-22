package dev.emortal.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import dev.emortal.messaging.RedisMessenger;
import dev.emortal.messaging.message.CreateGameMessage;
import dev.emortal.messaging.message.GameReadyMessage;
import dev.emortal.messaging.message.MapVoteMessage;
import dev.emortal.messaging.message.MatchmakeMessage;
import dev.emortal.messaging.types.GameInfo;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Matchmaker {

    private static final Logger LOGGER = LoggerFactory.getLogger(Matchmaker.class);

    private static final int COUNTDOWN_SECS = 10;
    private final Map<String, List<Match>> matches = new HashMap<>();
    private final Map<String, GameQueue> queues = new HashMap<>();
    private final Map<UUID, Set<UUID>> waitingPlayersMap = new HashMap<>(); // players waiting for game to be ready and proxy to send them
    private final Map<UUID, String> mapVotes = new HashMap<>();
    private final CorePlugin plugin;
    private final RedisMessenger redis;

    public Matchmaker(CorePlugin plugin, RedisMessenger redis) {
        this.plugin = plugin;
        this.redis = redis;

        redis.addMessageHandler(GameReadyMessage.class, (_, msg) -> handleGameReady(msg));
        redis.addMessageHandler(MatchmakeMessage.class, (_, msg) -> handleMatchmake(msg));
        redis.addMessageHandler(MapVoteMessage.class, (_, msg) -> handleMapVote(msg));
    }

    private void checkForMatch() {
        outer: for (Map.Entry<String, GameQueue> entry : new ArrayList<>(queues.entrySet())) { // copy so we can modify it
            String gameId = entry.getKey();
            GameQueue queue = entry.getValue();
            Set<UUID> players = queue.players;

            GameInfo gameInfo = plugin.getGameInfo(gameId);
            if (gameInfo == null) {
                LOGGER.error("Tried checking for match but no info for game: {}", gameId);
                continue;
            }

            List<Match> backfillMatches = getMatch(gameId);
            if (backfillMatches != null) {
                for (Match match : backfillMatches) {
                    if ((match.players.size() + players.size()) > gameInfo.maxPlayers()) continue;
                    match.addPlayers(players);
                    continue outer;
                }
            }

            if (players.size() >= gameInfo.minPlayers()) { // can never go above maxPlayers because a match is created before that can happen
                createMatch(gameId, gameInfo, players);
                GameQueue removed = queues.remove(gameId);
                removed.clear();
            }
        }
    }

    private List<Match> getMatch(String id) {
        return matches.get(id);
    }

    private void createMatch(String id, GameInfo gameInfo, Set<UUID> players) {
        switch (gameInfo.matchMethod()) {
            case INSTANT -> {
                createGame(id, players);
            }
            case COUNTDOWN -> {
                Match match = new Match(players, gameInfo);

                List<Match> matchSets = matches.computeIfAbsent(id, _ -> new ArrayList<>());
                matchSets.add(match);
            }
        }
    }

    private void createGame(String id, Set<UUID> players) {
        String map = getMostMapVotes(players);
        UUID uuid = plugin.getServerUUID(id);
        if (uuid == null) {
            LOGGER.error("Tried creating game but no server for game: {}", id);
            return;
        }

        Set<UUID> waitingUUIDs = waitingPlayersMap.computeIfAbsent(uuid, _ -> new HashSet<>());
        waitingUUIDs.addAll(players);

        redis.sendServerMessage(uuid, new CreateGameMessage(id, map, players));
    }

    private void cancelMatch(Match match) {
        for (List<Match> value : matches.values()) {
            value.remove(match);
        }

        // requeue remaining players
        for (UUID player : match.players) {
            addPlayer(player, match.gameInfo.gameId());
        }
    }

    public void handleGameReady(GameReadyMessage msg) {
        RegisteredServer server = plugin.getProxy().getServer(msg.gameUUID().toString()).orElse(null);
        if (server == null) {
            LOGGER.error("Received game ready for {} but proxy doesn't know this server", msg.gameUUID());
            return;
        }

        Set<UUID> players = waitingPlayersMap.get(msg.gameUUID());
        if (players == null || players.isEmpty()) {
            LOGGER.error("Received game ready for {} but no players waiting", msg.gameUUID());
            return;
        }

        LOGGER.info("Sending players: {}", players);

        // black wipe transition
        int transitionTime = 400;

        for (UUID playerUUID : players) {
            Player player = plugin.getProxy().getPlayer(playerUUID).orElse(null);
            if (player == null) {
                LOGGER.warn("Could not find player {}", playerUUID);
                continue;
            }
            player.showTitle(Title.title(
                    Component.text("\uE000", TextColor.color(100, 36, 44)),
                    Component.empty(),
                    Title.Times.times(Duration.ofMillis(transitionTime), Duration.ofSeconds(4), Duration.ofMillis(400))
            ));
        }

        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            for (UUID playerUUID : players) {
                Player player = plugin.getProxy().getPlayer(playerUUID).orElse(null);
                if (player == null) {
                    LOGGER.warn("Could not find player {}", playerUUID);
                    continue;
                }

                removePlayer(player.getUniqueId());

                ConnectionRequestBuilder connectionRequest = player.createConnectionRequest(server);
                connectionRequest.connect()
                        .thenAccept(result -> {
                            LOGGER.info("Sent player {} with result {}", player.getUniqueId(), result);
                        })
                        .join();
            }
        }).delay(transitionTime + 50, TimeUnit.MILLISECONDS).schedule();
    }

    private void handleMatchmake(MatchmakeMessage msg) { // TODO: should ensure players in the same MatchmakeMessage go into the same game, currently it is first come first serve
        for (UUID player : msg.players()) {
            addPlayer(player, msg.gameId());
        }
    }

    public void handleMapVote(MapVoteMessage msg) {
        mapVotes.put(msg.player(), msg.map());
    }

    @Subscribe
    public void onQuit(DisconnectEvent event) {
        removePlayer(event.getPlayer().getUniqueId());
    }
    @Subscribe
    public void onChangeServer(ServerPreConnectEvent event) {
        removePlayer(event.getPlayer().getUniqueId());
    }

    private @Nullable String getMostMapVotes(Set<UUID> players) {
        Map<String, Integer> newMapVotes = new HashMap<>();
        for (UUID player : players) {
            String map = mapVotes.get(player);
            if (map == null) continue;
            newMapVotes.compute(map, (_, b) -> (b == null ? 0 : b) + 1); // add one to map votes
        }

        int highestVotes = 0;
        String highestVotedMap = null;
        for (Map.Entry<String, Integer> entry : newMapVotes.entrySet()) {
            if (entry.getValue() > highestVotes) {
                highestVotes = entry.getValue();
                highestVotedMap = entry.getKey();
            }
        }

        return highestVotedMap;
    }

    // TODO: maybe multi queue? although its kinda incompatible with map voting
    public void addPlayer(UUID player, String wantedGameId) {
        removePlayer(player);

        GameInfo gameInfo = plugin.getGameInfo(wantedGameId);
        if (gameInfo == null) {
            LOGGER.error("Tried adding player to queue but no info for game: {}", wantedGameId);
            return;
        }

        GameQueue queue = queues.computeIfAbsent(wantedGameId, _ -> new GameQueue(gameInfo));
        queue.addPlayer(player);

        checkForMatch();
    }

    public void removePlayer(UUID player) {
        mapVotes.remove(player);
        for (GameQueue value : queues.values()) {
            value.removePlayer(player);
        }
        for (List<Match> value : matches.values()) {
            for (Match match : new ArrayList<>(value)) {
                match.removePlayer(player);
            }
        }
    }

    private class GameQueue {
        private final Set<UUID> players = new HashSet<>();
        private final GameInfo gameInfo;
        private final BossBar bossBar;
        public GameQueue(GameInfo gameInfo) {
            this.gameInfo = gameInfo;
            this.bossBar = BossBar.bossBar(this::createBossbarComponent, 1f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        }

        private Component createBossbarComponent() {
            return Component.text()
                    .append(Component.text("Queuing for "))
                    .append(Component.text(gameInfo.gameId())) // TODO: replace with friendly name
                    .append(Component.text(" "))
                    .append(Component.text(players.size()))
                    .append(Component.text("/"))
                    .append(Component.text(gameInfo.minPlayers()))
                    .build();
        }

        public void addPlayer(UUID uuid) {
            players.add(uuid);
            this.bossBar.name(this::createBossbarComponent);

            Player player = plugin.getProxy().getPlayer(uuid).orElse(null);
            if (player == null) return;
            player.showBossBar(this.bossBar);
        }

        public void removePlayer(UUID uuid) {
            players.remove(uuid);

            this.bossBar.name(this::createBossbarComponent);
            Player player = plugin.getProxy().getPlayer(uuid).orElse(null);
            if (player == null) return;
            player.hideBossBar(this.bossBar);
        }

        public void clear() {
            for (Player player : getPlayers()) {
                player.hideBossBar(bossBar);
            }
        }

        private List<Player> getPlayers() {
            List<Player> players = new ArrayList<>();
            for (UUID uuid : this.players) {
                Player player = plugin.getProxy().getPlayer(uuid).orElse(null);
                if (player == null) continue;
                players.add(player);
            }
            return players;
        }
    }

    private class Match {
        private final Set<UUID> players;
        private final GameInfo gameInfo;
        private @Nullable BossBar bossBar = null;
        private final ScheduledTask task;
        public Match(Set<UUID> uuids, GameInfo gameInfo) {
            this.players = new HashSet<>(uuids);
            this.gameInfo = gameInfo;

            this.task = plugin.getProxy().getScheduler().buildTask(plugin, new Consumer<>() {
                int seconds = COUNTDOWN_SECS;

                @Override
                public void accept(ScheduledTask task) {
                    if (seconds == 0) {
                        if (bossBar != null) for (Player player : getPlayers()) {
                            player.hideBossBar(bossBar);
                        }

                        createGame(gameInfo.gameId(), new HashSet<>(players));
                        task.cancel();
                        return;
                    }

                    if (bossBar == null) {
                        bossBar = BossBar.bossBar(createBossbarSecondsComponent(COUNTDOWN_SECS), 1f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
                        for (Player player : getPlayers()) {
                            player.showBossBar(bossBar);
                            player.playSound(Sound.sound(Key.key("block.beacon.activate"), Sound.Source.MASTER, 1f, 2f), Sound.Emitter.self());
                        }
                    } else {
                        bossBar.name(createBossbarSecondsComponent(seconds));
                        for (Player player : getPlayers()) {
                            player.playSound(Sound.sound(Key.key("block.note_block.hat"), Sound.Source.MASTER, 0.7f, 2f), Sound.Emitter.self());
                        }
                    }


                    seconds--;
                }
            }).repeat(1, TimeUnit.SECONDS).delay(0, TimeUnit.SECONDS).schedule();
        }

        private Component createBossbarSecondsComponent(int seconds) {
            return Component.text()
                    .append(Component.text("Joining "))
                    .append(Component.text(gameInfo.gameId())) // TODO: replace with friendly name
                    .append(Component.text(" in "))
                    .append(Component.text(seconds))
                    .append(Component.text("s"))
                    .build();
        }

        private List<Player> getPlayers() {
            List<Player> players = new ArrayList<>();
            for (UUID uuid : this.players) {
                Player player = plugin.getProxy().getPlayer(uuid).orElse(null);
                if (player == null) continue;
                players.add(player);
            }
            return players;
        }

        public void addPlayers(Collection<UUID> uuids) {
            players.addAll(uuids);
            if (this.bossBar != null) for (Player player : getPlayers()) {
                player.showBossBar(this.bossBar);
            }
        }

        public void addPlayer(UUID uuid) {
            players.add(uuid);
            if (bossBar != null) {
                Player player = plugin.getProxy().getPlayer(uuid).orElse(null);
                if (player == null) return;
                player.showBossBar(bossBar);
            }
        }

        public void removePlayer(UUID uuid) {
            Player player = plugin.getProxy().getPlayer(uuid).orElse(null);
            if (player != null && this.bossBar != null) player.hideBossBar(this.bossBar);

            if (!players.remove(uuid)) return;
            if (players.size() < gameInfo.minPlayers()) {
                if (bossBar != null) for (Player p : getPlayers()) {
                    p.hideBossBar(this.bossBar);
                }
                if (task != null) task.cancel();

                cancelMatch(this);
            }
        }
    }

}
