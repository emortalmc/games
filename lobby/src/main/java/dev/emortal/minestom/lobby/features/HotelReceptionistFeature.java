package dev.emortal.minestom.lobby.features;

import dev.emortal.bbstom.BBAnimation;
import dev.emortal.bbstom.BBModel;
import dev.emortal.bbstom.renderer.BlockbenchPlayerRenderer;
import dev.emortal.minestom.lobby.emote.Emote;
import dev.emortal.minestom.lobby.util.entity.BetterEntity;
import dev.emortal.minestom.lobby.util.entity.BetterLivingEntity;
import dev.emortal.minestom.lobby.util.entity.InteractionEntity;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.player.ResolvableProfile;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.concurrent.ThreadLocalRandom;

public final class HotelReceptionistFeature implements LobbyFeature {

    private static final PlayerSkin BOM_SKIN = new PlayerSkin(
            "ewogICJ0aW1lc3RhbXAiIDogMTY5Mzg0NjI0NDQyOCwKICAicHJvZmlsZUlkIiA6ICI5Yjk2NzE2MDY4YmE0OTkyODYzZTYwYjE1ZDAwY2UzOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCb21CYXJkeUdhbWVyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzRkNDk5ODE2ODE1MGM2NTQxNGE0NjcyN2IxZGMyYmY3MzdlOGFjMDY1OGFmNzRjYzhjNDA0MTQwNjZhNmFlZTkiCiAgICB9LAogICAgIkNBUEUiIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzIzNDBjMGUwM2RkMjRhMTFiMTVhOGIzM2MyYTdlOWUzMmFiYjIwNTFiMjQ4MWQwYmE3ZGVmZDYzNWNhN2E5MzMiCiAgICB9CiAgfQp9",
            "FSO8gI+vFJMhPJu4OsILhNlBG8A64Qi0HwdbTsIWdLHew7ucSDMDGKSoSxGbo2eOAXYxoKDXzydjO08LgmCrr2aFo3l/lB+BQ0fbaPlp41b2soUdnfnQsvri45a+QF+xfP2P5KAGE3QWKuf6hMZpZ/pKx2Cw4F21P3X5zKN+sKFTozkZILokfKw68guOqI46lNl1eICxqjAHlycQ4L7r1uETtpZ5J3a8P5rNpkpgUdXk7kO3UUvLA1Rh1ptDO9rFBIUeYABsHWGwyGE5B9N+9ML8iCHVF3rmeJg0gei3+bA0sedLmVglLboxgCbHKIL3tu6J1WPMFPgU8OrOs9c41YI9nyCY1I05Bgfm8wt6Q9C2avesH1fx/Q7mglRilZK/5eyI87dzq5mKjOhhgDljU9RTobY0hVxcffCBeYnUk9fsztEYXNcTzzoUKBJTXI9DDQU1yyZ5qpvte7e9jFpfVjmFgpGcut5w1YAyFVakS90Pfi8WfHZD13Jg9ZyqD4JpgXYiRJklTU3vqkjfcK3AK4vAEuYIuqZYThk1+yxSRvSVvDtk7V7iZqEomvJcWouOBG/8EpoJAVJ6MUourY7DFrXaOaIZMJUXQX1GmPjqgk1qtvRJGzPKWS56cek6HMhzs3Ror+ebHvXfHvxTwLXEuIqIwOEjdH2vrMqryJcs1Ks="
    );
    private static final PlayerSkin ZAK_SKIN = new PlayerSkin(
            "ewogICJ0aW1lc3RhbXAiIDogMTY5Mzg0NjM1NjcwNywKICAicHJvZmlsZUlkIiA6ICI4ZDM2NzM3ZTFjMGE0YTcxODdkZTk5MDZmNTc3ODQ1ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJFeHBlY3RhdGlvbmFsIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzE0MzQxNmU2ODA1NTU2OGU1YmZmMDcyMWYyMTNiOTQ5OTQ0MmEyNDcwNDc3NjZhNWVkNWY2NjM5MTM1NDRiOWYiCiAgICB9LAogICAgIkNBUEUiIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzIzNDBjMGUwM2RkMjRhMTFiMTVhOGIzM2MyYTdlOWUzMmFiYjIwNTFiMjQ4MWQwYmE3ZGVmZDYzNWNhN2E5MzMiCiAgICB9CiAgfQp9",
            "txUiz7rJBj1UblS7ZIOT2mXymQ15PcI5oTm9MRCHwLHDCHmyTbm7D6rjzhStYRLEvYbvqCJ2yikrSzBTV06SnNtpkxfw8tN7oVLQbiRzTUClTJbA9z52QQuiFoOQUL4Rr6XetIcUh7oyy9Bqc6JvvoIqj8mmAGa155AeQ1/aMGiq7NOuJcYwR7kBbh/jD6y5qZq0c6TRlmchtamQRgXcX06bH+8IEVhu+QjxKJz7QqWL46HCR76p+dcxe0w4yiY4HS1pGINJyc9r+PLC7EAT5JvH4H9KiQR2RqJtq+v0NVCJCmX4NMkEhInGDLS7q0qwgGt3N4uHPyi48VPo+RLaXGHHaA8auCV0eqZTg40jZxAHmImxOx0y/Iqt5XYonBZOj0h9O3VdheCpioNRsFbVUBJe0yCq+NCVCboi+tvBmwZtI5FnLgHr1KwG+w37o1qTQORZy7qWbqwqu4LyflKe5Mr8BueccP1xEhc9KOEx9VWUi0Cy/thtx4O3AkSw3yjwmTRVErx6nSojA7KoHPqIszUPBnGqRJqraSOt6vyXeDtX7d/gvAC6CbNcNRP86xXAuI8QxG9AeTSVEd9p8/KDZsDxwhOJJavDQTaBnZSJ/Bg3j30OEFznz+K9f42js5M7YarqeprYmXQ767gCFCNlPUtg19aN68L+C1KETjJnOyI="
    );

    private static final Pos BOM_POS = new Pos(15, 64, 25, 90f, 0f);
    private static final Pos ZAK_POS = new Pos(15, 64, 28);

    private static final ItemStack BOM_FIREBALL = ItemStack.of(Material.FIRE_CHARGE);

    private static final String[] ZAK_TEXTS = new String[] {
            "This building needs a refactor",
            "Bom should stop adding bugs",
            "This is all Bom's fault",
            "emortal needs to stop adding things to the lobby",
            "Try going in the lift!",
            "You talk more than Bom does!"

    };

    private static final String[] BOM_TEXTS = new String[] {
            "Yeah I spent £20 on a basic logo. And what?",
            "What does that lever do?",
            "Legend has it, emortal worked on this server once",
            "We still don't have enough microservices!",
            "*Incoherent yapping*",
            "*Incoherent yapping*"
    };


    @Override
    public void register(@NotNull Instance instance) {
//        NpcPlayer zakNpc = new NpcPlayer("Zak", ZAK_SKIN, false);
//        zakNpc.setInstance(instance, ZAK_POS);
//        zakNpc.setItemInHand(Player.Hand.MAIN, ZAK_TRUMPET);
//        zakNpc.setHandler((player, clickType) -> {
//            player.sendActionBar(
//                    Component.text()
//                            .append(Component.text("Zak: ", NamedTextColor.GRAY))
//                            .append(Component.text(pickRandomZakMessage(), NamedTextColor.WHITE))
//                            .build()
//            );
//            player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_AMBIENT, Sound.Source.MASTER, 0.7f, 1.5f), ZAK_POS);
//        });


        InteractionEntity zakInteraction = new InteractionEntity();
        zakInteraction.setInstance(instance, ZAK_POS);
        zakInteraction.setHitConsumer(player -> {
            player.sendActionBar(
                    Component.text()
                            .append(Component.text("Zak: ", NamedTextColor.GRAY))
                            .append(Component.text(pickRandomZakMessage(), NamedTextColor.WHITE))
                            .build()
            );
            player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_AMBIENT, Sound.Source.MASTER, 0.7f, 1.5f), ZAK_POS);
        });

        BetterLivingEntity bomNpc = new BetterLivingEntity(EntityType.MANNEQUIN);
        bomNpc.editEntityMeta(MannequinMeta.class, meta -> {
            meta.setProfile(new ResolvableProfile(BOM_SKIN));
            meta.setCustomNameVisible(true);
        });
        bomNpc.set(DataComponents.CUSTOM_NAME, Component.text("Bom"));
        bomNpc.setInstance(instance, BOM_POS);
        bomNpc.setItemInHand(PlayerHand.MAIN, BOM_FIREBALL);
        instance.eventNode().addListener(PlayerEntityInteractEvent.class, event -> {
            if (event.getHand() != PlayerHand.MAIN) return;

            Player player = event.getPlayer();

            if (event.getTarget() == bomNpc) {
                player.sendActionBar(
                        Component.text()
                                .append(Component.text("Bom: ", NamedTextColor.GRAY))
                                .append(Component.text(pickRandomBomMessage(), NamedTextColor.WHITE))
                                .build()
                );
                player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_AMBIENT, Sound.Source.MASTER, 0.7f, 1.5f), BOM_POS);
            }
        });

        spawnSpinnyZak(instance, ZAK_POS);
    }

    private void spawnSpinnyZak(Instance instance, Pos pos) {
        BetterEntity seat = new BetterEntity(EntityType.ITEM_DISPLAY);
        seat.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setItemStack(ItemStack.of(Material.OAK_STAIRS));
        });
        seat.setTicking(false);
        seat.setInstance(instance, pos.add(0, 0.5, 0).withYaw(180f));

        BBAnimation spin = Emote.MODEL.getAnimationByName("zakspin");
        String torsoUUID = Emote.MODEL.getElementUUID("Body");
        BlockbenchPlayerRenderer renderer = new BlockbenchPlayerRenderer(Emote.MODEL, spin, instance, pos.withYaw(90f), ZAK_SKIN);
        renderer.render();

        Quaternionf rot = new Quaternionf(0, 0, 0, 1).rotateX((float) Math.toRadians(-20));

        instance.scheduler().buildTask(() -> {
            float time = renderer.getCurrentTime();
            Quaternionf rotation = Emote.MODEL.getRotation(spin, time - (1/20f), torsoUUID);

            seat.editEntityMeta(ItemDisplayMeta.class, meta -> {
                meta.setLeftRotation(BBModel.quatToFloats(rotation.mul(rot)));
                meta.setTransformationInterpolationDuration(1);
                meta.setTransformationInterpolationStartDelta(0);
            });
        }).repeat(TaskSchedule.tick(1)).schedule();

    }

    private String pickRandomZakMessage() {
        return ZAK_TEXTS[ThreadLocalRandom.current().nextInt(ZAK_TEXTS.length)];
    }
    private String pickRandomBomMessage() {
        return BOM_TEXTS[ThreadLocalRandom.current().nextInt(BOM_TEXTS.length)];
    }

}
