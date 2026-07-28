package dev.emortal.minestom.holeymoley.game;

import dev.emortal.minestom.core.game.Game;
import dev.emortal.minestom.core.game.config.GameCreationInfo;
import dev.emortal.minestom.core.game.util.GameWinLoseMessages;
import dev.emortal.minestom.core.map.LoadedMap;
import dev.emortal.minestom.holeymoley.HoleyMoleyModule;
import dev.emortal.minestom.holeymoley.chest.ChestBlockHandler;
import dev.emortal.minestom.holeymoley.chest.ChestUpdateHandler;
import dev.emortal.minestom.holeymoley.event.BlindnessEvent;
import dev.emortal.minestom.holeymoley.event.CloseCallEvent;
import dev.emortal.minestom.holeymoley.event.EventManager;
import dev.emortal.minestom.holeymoley.event.HalfLifeEvent;
import dev.emortal.minestom.holeymoley.event.InventoryChaosEvent;
import dev.emortal.minestom.holeymoley.event.MinesweeperEvent;
import dev.emortal.minestom.holeymoley.event.RainbowDirtEvent;
import dev.emortal.minestom.holeymoley.event.TrapdoorEvent;
import dev.emortal.minestom.holeymoley.listeners.PvpListener;
import io.github.togar2.pvp.damage.combat.CombatManager;
import io.github.togar2.pvp.feature.CombatFeatures;
import io.github.togar2.pvp.feature.FeatureType;
import io.github.togar2.pvp.feature.cooldown.VanillaItemCooldownFeature;
import io.github.togar2.pvp.feature.explosion.ExplosionFeature;
import io.github.togar2.pvp.feature.food.VanillaExhaustionFeature;
import io.github.togar2.pvp.feature.food.VanillaRegenerationFeature;
import io.github.togar2.pvp.feature.tracking.VanillaDeathMessageFeature;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EquipmentSlotGroup;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.AttributeList;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class HoleyMoleyGame extends Game {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final ItemStack EFFICIENCY_SHOVEL = ItemStack.builder(Material.IRON_SHOVEL)
            .set(DataComponents.ENCHANTMENTS, new EnchantmentList(Map.of(Enchantment.EFFICIENCY, 5)))
            .set(DataComponents.ATTRIBUTE_MODIFIERS, new AttributeList(new AttributeList.Modifier(Attribute.BLOCK_BREAK_SPEED, new AttributeModifier(Key.key("holeymoley"), 3, AttributeOperation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.HAND)))
            .build();
    private static final ItemStack DIRT_STACK_ITEM = ItemStack.of(Material.DIRT, 64);
    private static final ItemStack DIRT_ITEM = ItemStack.of(Material.DIRT);

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean ended = new AtomicBoolean();

    private final EventManager eventManager = new EventManager();

    private @Nullable Task regularGlowTask = null;
    private @Nullable Task regularEventTask = null;

    private final Set<Point> playerPlacedBlocks = new HashSet<>();

    public HoleyMoleyGame(@NotNull GameCreationInfo creationInfo, @NotNull LoadedMap map) {
        super(creationInfo, map);

        this.eventManager.registerEvent("blindness", BlindnessEvent.class);
        this.eventManager.registerEvent("closecall", CloseCallEvent.class);
        this.eventManager.registerEvent("halflife", HalfLifeEvent.class);
        this.eventManager.registerEvent("inventorychaos", InventoryChaosEvent.class);
        this.eventManager.registerEvent("minesweeper", MinesweeperEvent.class);
        this.eventManager.registerEvent("rainbowdirt", RainbowDirtEvent.class);
        this.eventManager.registerEvent("trapdoor", TrapdoorEvent.class);
    }

    @Override
    public void onPreJoin(@NotNull Player player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int radius = HoleyMoleyModule.MAP_RADIUS;

        player.setRespawnPoint(new Pos(
                random.nextInt(-radius + 1, radius) + 0.5,
                random.nextInt(-radius + 1, radius - 1), // player is 2 blocks tall
                random.nextInt(-radius + 1, radius) + 0.5
        ));
        getInstance().setBlock(player.getRespawnPoint(), Block.AIR);
        getInstance().setBlock(player.getRespawnPoint().add(0, 1, 0), Block.AIR);
    }

    @Override
    public void onJoin(@NotNull Player player) {
        player.setAutoViewable(true);
        player.setTeam(PlayerTeams.ALIVE);
        player.setGlowing(false);
        player.setGameMode(GameMode.ADVENTURE);

        player.getInventory().setItemStack(0, EFFICIENCY_SHOVEL);
    }

    @Override
    public void onLeave(@NotNull Player player) {
        player.setTeam(null);
        player.clearEffects();

        this.checkPlayerCounts();
    }

    @Override
    public void start() {
        this.started.set(true);

        getInstance().scheduler().submitTask(new Supplier<>() {
            int i = 5;

            @Override
            public @NotNull TaskSchedule get() {
                if (this.i == 3) {
                    playSound(Sound.sound(SoundEvent.BLOCK_PORTAL_TRIGGER, Sound.Source.MASTER, 0.45f, 1.27f));
                }

                if (this.i == 0) {
                    clearTitle();
                    startGame();
                    return TaskSchedule.stop();
                }

                showCountdown(this.i);
                this.i--;
                return TaskSchedule.seconds(1);
            }
        });
    }

    private void showCountdown(final int countdown) {
        this.playSound(Sound.sound(Key.key("battle.countdown.begin"), Sound.Source.MASTER, 1F, 1F), Sound.Emitter.self());
        this.showTitle(Title.title(
                Component.text(countdown, NamedTextColor.GREEN, TextDecoration.BOLD),
                Component.empty(),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500))
        ));
    }

    public void startGame() {
        this.regularGlowTask = getInstance().scheduler().submitTask(new RegularGlowTask(this));
        this.regularEventTask = getInstance().scheduler().submitTask(new RegularEventTask(this));
        new PvpListener(this);
        ChestUpdateHandler chestUpdateHandler = new ChestUpdateHandler(this);

        for (Player player : getPlayers()) {
            // silly minestompvp
            player.setTag(VanillaItemCooldownFeature.COOLDOWN_END, new HashMap<>());
            player.setTag(VanillaDeathMessageFeature.COMBAT_MANAGER, new CombatManager(player));
            player.setTag(VanillaExhaustionFeature.EXHAUSTION, 0.0f);
            player.setTag(VanillaRegenerationFeature.STARVATION_TICKS, 0);
            player.setGameMode(GameMode.SURVIVAL);
        }

        getEventNode().addChild(CombatFeatures.modernVanilla().createNode()); // add minestompvp events

        getEventNode().addListener(ItemDropEvent.class, event -> {
            ItemEntity itemEntity = new ItemEntity(event.getItemStack());
            itemEntity.setPickupDelay(40, TimeUnit.SERVER_TICK);
            Vec velocity = event.getEntity().getPosition().direction().mul(6);
            itemEntity.setVelocity(velocity);
            itemEntity.setInstance(event.getEntity().getInstance(), event.getEntity().getPosition().add(0, event.getEntity().getEyeHeight(), 0));
        });

        getEventNode().addListener(PickupItemEvent.class, event -> {
            if (!(event.getLivingEntity() instanceof Player player)) return;

            boolean success = player.getInventory().addItemStack(event.getItemStack());
            event.setCancelled(!success);
        });

        getEventNode().addListener(PlayerBlockPlaceEvent.class, event -> {
            if (event.getBlock().compare(Block.TNT)) { // tnt block should immediately prime
                event.setCancelled(true);
                ItemStack newUsedItem = event.getPlayer().getItemInHand(event.getHand()).consume(1);
                event.getPlayer().setItemInHand(event.getHand(), newUsedItem);

                CombatFeatures.modernVanilla().get(FeatureType.EXPLOSION)
                        .primeExplosive(getInstance(), event.getBlockPosition(), new ExplosionFeature.IgnitionCause.ByPlayer(event.getPlayer()), 40);
                return;
            }

            playerPlacedBlocks.add(event.getBlockPosition());
        });

        getEventNode().addListener(PlayerBlockBreakEvent.class, event -> {
            Player player = event.getPlayer();
            Block block = event.getBlock();

            boolean hasStackOfDirt = player.getInventory().takeItemStack(DIRT_STACK_ITEM, TransactionOption.DRY_RUN);
            if (block.compare(Block.DIRT) && !hasStackOfDirt) {
                player.playSound(
                        Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 0.25f, 1f),
                        Sound.Emitter.self()
                );
                player.getInventory().addItemStack(DIRT_ITEM);
            }

            if (block.compare(Block.CHEST)) {
                ChestBlockHandler chestHandler = chestUpdateHandler.getOrCreateChestHandler(event.getBlockPosition());
                PvpListener.dropItems(chestHandler.getInventory(), getInstance(), event.getBlockPosition());
            }

            if (ThreadLocalRandom.current().nextDouble() < 0.005) { // 0.5% chance per block broken to spawn chest
                if (playerPlacedBlocks.contains(event.getBlockPosition())) return; // Don't allow chests to spawn in player-placed blocks

                player.sendMessage(
                        Component.text()
                                .append(Component.text("★", NamedTextColor.GOLD))
                                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                                .append(Component.text("You uncovered a ", NamedTextColor.GRAY))
                                .append(Component.text("CHEST", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                                .append(Component.text("!", NamedTextColor.GRAY))
                );

                player.playSound(
                        Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.PLAYER, 1f, 1.5f),
                        event.getBlockPosition()
                );

                event.setResultBlock(Block.CHEST);
                chestUpdateHandler.getOrCreateChestHandler(event.getBlockPosition());
            }
        });
    }

    public void checkPlayerCounts() {
        Set<Player> alivePlayers = this.getAlivePlayers();

        if (alivePlayers.isEmpty()) {
            this.finish();
            return;
        }

        if (alivePlayers.size() == 1) {
            if (this.started.get()) {
                this.victory(alivePlayers.iterator().next());
            } else {
                this.finish();
            }

            return;
        }

        // TODO: show remaining players somewhere
    }

    public void victory(@Nullable Player winner) {
        this.ended.set(true);

        // cancel ongoing tasks
        if (this.regularGlowTask != null) this.regularGlowTask.cancel();
        if (this.regularEventTask != null) this.regularEventTask.cancel();

        Title victoryTitle = Title.title(
                MINI_MESSAGE.deserialize("<gradient:#ffc570:gold><bold>VICTORY!"),
                Component.text(GameWinLoseMessages.randomVictory(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
        );
        Title defeatTitle = Title.title(
                MINI_MESSAGE.deserialize("<gradient:#ff474e:#ff0d0d><bold>DEFEAT!"),
                Component.text(GameWinLoseMessages.randomDefeat(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
        );

        Sound defeatSound = Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.MASTER, 1f, 1f);
        Sound victorySound = Sound.sound(SoundEvent.BLOCK_BEACON_POWER_SELECT, Sound.Source.MASTER, 1f, 0.8f);

        // If there is no winner, choose the player with the highest kills
        if (winner == null) {
            winner = this.findPlayerWithHighestKills();
        }

        for (Player player : this.getPlayers()) {
            player.setInvulnerable(true);

            if (winner == player) {
                player.showTitle(victoryTitle);
                player.playSound(victorySound, Sound.Emitter.self());
            } else {
                player.showTitle(defeatTitle);
                player.playSound(defeatSound, Sound.Emitter.self());
            }
        }

        getInstance().scheduler().buildTask(this::finish)
                .delay(TaskSchedule.seconds(6))
                .schedule();
    }

    private @Nullable Player findPlayerWithHighestKills() {
        int killsRecord = 0;
        Player highestKiller = null;

        for (Player player : this.getPlayers()) {
            Integer playerKills = player.getTag(PvpListener.KILLS_TAG);
            if (playerKills == null) playerKills = 0;
            if (playerKills > killsRecord) {
                killsRecord = playerKills;
                highestKiller = player;
            }
        }

        return highestKiller;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    @Override
    public void cleanUp() {
        getInstance().scheduleNextTick(MinecraftServer.getInstanceManager()::unregisterInstance);
    }

    @Override
    public @NotNull Instance getSpawningInstance(@NotNull Player player) {
        return getInstance();
    }

    public @NotNull Instance getInstance() {
        return getMap().instance();
    }

    public @NotNull Set<Player> getAlivePlayers() {
        Set<Player> alivePlayers = ConcurrentHashMap.newKeySet();
        for (Player player : getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            alivePlayers.add(player);
        }
        return Collections.unmodifiableSet(alivePlayers);
    }

    public boolean hasEnded() {
        return this.ended.get();
    }
}
