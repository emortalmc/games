package dev.emortal.minestom.parkourtag.listeners;

import dev.emortal.minestom.parkourtag.GameStage;
import dev.emortal.minestom.parkourtag.ParkourTagGame;
import dev.emortal.minestom.parkourtag.physics.PlayerRagdoll;
import dev.emortal.minestom.parkourtag.utils.CoordinateUtils;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.network.packet.server.play.HitAnimationPacket;
import net.minestom.server.property.ServerProperties;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ParkourTagAttackListener {

    private static final List<String> DEATH_MESSAGES = List.of(
            "<victim> was found by <tagger>",
            "<victim> failed the jump",
            "<tagger> was too fast for <victim>",
            "<victim> isn't very good at hide and seek",
            "<victim> didn't realise the game started",
            "<victim> was cornered by <tagger>",
            "<victim> walked into a wall",
            "<tagger> got too close to <victim>",
            "<victim> should play more Marathon",
            "<victim> skipped leg day",
            "<victim> failed <tagger>'s expectations"
    );

    private static final Tag<Long> HIT_COOLDOWN = Tag.Long("hitCooldown");

    public static void registerListener(EventNode<Event> eventNode, ParkourTagGame game) {

        eventNode.addListener(EntityAttackEvent.class, e -> {
            GameStage gameStage = game.getGameStage();
            if (gameStage != GameStage.LIVE && gameStage != GameStage.VICTORY) return;

            if (!(e.getEntity() instanceof Player attacker)) return;
            if (!(e.getTarget() instanceof Player target)) return;

            if (attacker.getGameMode() != GameMode.ADVENTURE) return;
            if (target.getGameMode() != GameMode.ADVENTURE) return;

            // Logic for victory where goons can hit the seeker
            if (gameStage == GameStage.VICTORY && target.getTeam() == ParkourTagGame.TAGGER_TEAM) { // Attacking tagger after victory
                if (!target.hasTag(HIT_COOLDOWN)) target.setTag(HIT_COOLDOWN, System.currentTimeMillis());
                if (target.getTag(HIT_COOLDOWN) > System.currentTimeMillis()) return;
                target.setTag(HIT_COOLDOWN, System.currentTimeMillis() + 500);

                target.takeKnockback(0.4f, Math.sin(Math.toRadians(attacker.getPosition().yaw())), -Math.cos(Math.toRadians(attacker.getPosition().yaw())));
                HitAnimationPacket hitAnimationPacket = new HitAnimationPacket(target.getEntityId(), attacker.getPosition().yaw());
                game.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_HURT, Sound.Source.PLAYER, 1f, 1f), target.getPosition());

                game.sendGroupedPacket(hitAnimationPacket);

                return;
            }

            if (target.getTeam() == ParkourTagGame.TAGGER_TEAM || attacker.getTeam() != ParkourTagGame.TAGGER_TEAM ||
                    gameStage == GameStage.VICTORY)
                return;

            // Logic for the seeker hitting the attackers

            attacker.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());
            target.showTitle(
                    Title.title(
                            Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                            Component.empty(),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(500))
                    )
            );

            target.setGameMode(GameMode.SPECTATOR);
            target.setAutoViewable(false);
            target.setGlowing(false);

            Vec impulse = attacker.getPosition().direction().mul(3000);
            var torso = PlayerRagdoll.spawnRagdollWithImpulse(game.getPhysics(), target, target.getPosition(), impulse);

            Entity spectatingEntity = new Entity(EntityType.TEXT_DISPLAY);
            spectatingEntity.setNoGravity(true);
            spectatingEntity.editEntityMeta(TextDisplayMeta.class, meta -> {
                meta.setPosRotInterpolationDuration(5);
            });
            Pos pos = torso.getEntity().getPosition().sub(attacker.getPosition().direction().mul(3)).withLookAt(torso.getEntity().getPosition());
            spectatingEntity.setInstance(game.getInstance(), pos).thenRun(() -> {
                spectatingEntity.scheduler().buildTask(new Runnable() {
                    Vec lastDir = null;

                    @Override
                    public void run() {
                        double lengthSq = torso.getBody().getLinearVelocity().lengthSq();

                        if (lastDir == null || lengthSq > 5*5) lastDir = CoordinateUtils.toVec(torso.getBody().getLinearVelocity()).normalize();

                        spectatingEntity.teleport(torso.getEntity().getPosition().sub(lastDir.mul(3)).withLookAt(torso.getEntity().getPosition()));
                    }
                }).repeat(TaskSchedule.tick(1)).schedule();

                target.spectate(spectatingEntity);
            });

            game.getInstance().scheduler().buildTask(() -> {
                target.lookAt(torso.getEntity().getPosition());
                spectatingEntity.remove();
                target.stopSpectating();
            }).delay(TaskSchedule.tick(4 * ServerProperties.SERVER_TICKS_PER_SECOND.get())).schedule();

            game.getTaggers().remove(target);
            game.getGoons().remove(target);
            target.setTeam(ParkourTagGame.DEAD_TEAM);

            // Pick a random death message
            ThreadLocalRandom random = ThreadLocalRandom.current();
            game.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<gray>" +
                            DEATH_MESSAGES.get(random.nextInt(DEATH_MESSAGES.size())),
                    Placeholder.component("victim", Component.text(target.getUsername(), NamedTextColor.RED)),
                    Placeholder.component("tagger", Component.text(attacker.getUsername(), NamedTextColor.WHITE))
            ));

            target.setVelocity(attacker.getPosition().direction().mul(15.0));

            // Firework death effect
//            FireworkExplosion randomColorEffect = new FireworkExplosion(
//                    FireworkExplosion.Shape.LARGE_BALL,
//                    List.of(new Color(java.awt.Color.HSBtoRGB(random.nextFloat(), 1f, 1f))),
//                    List.of(),
//                    false,
//                    false
//            );
//            FireworkUtils.showFirework(game.getPlayers(), e.getInstance(), target.getPosition().add(0, 1.5, 0), List.of(randomColorEffect));

            // Check for win with new alive count
            game.checkPlayerCounts();
        });
    }

}
