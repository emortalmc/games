package dev.emortal.minestom.lobby.features;

import dev.emortal.bbstom.BBModel;
import dev.emortal.minestom.lobby.util.entity.BetterEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.Git;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.*;
import java.util.function.Consumer;

public final class GraffitiFeature implements LobbyFeature {

    private static final Map<String, Double> DONATORS = new HashMap<>();

    private @NotNull Instance instance = null;

    @Override
    public void register(@NotNull Instance instance) {
        DONATORS.put("CoPokBl", 4.83);
        DONATORS.put("mudkip", 5.00 + 5.00);
        DONATORS.put("aesturr", 3.00);
        DONATORS.put("The real cat", 300.00 + 50.00);
        DONATORS.put("xSehrMotiviert", 140.00);
        DONATORS.put("MrGazdag", 8.00 + 3.00);
        DONATORS.put("Cybermats", 5.00);
        DONATORS.put("CultostOfTST", 3.00);
        DONATORS.put("allualbert", 10.00);
        DONATORS.put("iam", 3.00);
        DONATORS.put("mattw", 5.00);
        DONATORS.put("firestranded", 4.00);
        DONATORS.put("TheMode", 5.00);
        DONATORS.put("RayTheDumbass", 5.00);
        DONATORS.put("kibble_wibble", 3.00);
        DONATORS.put("Libreh", 12.00);


        this.instance = instance;

        // Welcome to
        // EmortalMC
        this.createGraffiti(
                Component.text("ᴡᴇʟᴄᴏᴍᴇ ᴛᴏ", NamedTextColor.GRAY),
                new Pos(5.5, 65 + Vec.EPSILON, -15.5, 15f, -90f),
                meta -> {
                    meta.setScale(new Vec(1.8, 1.8, 1));
                    meta.setShadow(true);
                }
        );
        this.createGraffiti(
                MiniMessage.miniMessage().deserialize("<gradient:light_purple:gold><bold>EmortalMC"),
                new Pos(5.35, 65 + Vec.EPSILON, -14.5, 15f, -90f),
                meta -> {
                    meta.setScale(new Vec(3, 3, 1));
                    meta.setShadow(true);
                }
        );

        // Library
        this.createGraffiti(
                Component.text("ʟɪʙʀᴀʀʏ", TextColor.color(0x333333)),
                new Pos(-11, 68, -11.5, -90f, 0f),
                meta -> meta.setScale(new Vec(3.4, 4, 1).mul(1.5))
        );
//        Entity graffiti5 = this.createGraffiti(
//                Component.text("Restocked!", NamedTextColor.RED),
//                meta -> meta.setScale(new Vec(1.3, 1.3, 1))
//        );

        // Server Version
        this.createGraffiti(
                Component.text()
                        .append(Component.text(MinecraftServer.getBrandName()))
                        .appendNewline()
                        .append(Component.text(Git.version()))
                        .build(),
                new Pos(-5.4, 69.2, -39.0 - Vec.EPSILON, 180f, 0f),
                meta -> meta.setAlignLeft(true)
        );

        // Cosmetics
        this.createGraffiti(
                Component.text("ᴄᴏѕᴍᴇᴛɪᴄѕ", TextColor.color(0x222222)),
                new Pos(-12.5, 69.7, 19 - 0.05, -180f, 0f),
                meta -> meta.setScale(new Vec(4))
        );
        this.createGraffiti(
                Component.text("ᴄᴏѕᴍᴇᴛɪᴄѕ", TextColor.color(0x158E3B)),
                new Pos(-12.55, 69.65, 19 - 0.01, -180f, 0f),
                meta -> meta.setScale(new Vec(4))
        );
        this.createGraffiti(
                Component.text("ᴄᴏѕᴍᴇᴛɪᴄѕ", TextColor.color(0x8E1568)),
                new Pos(-12.45, 69.75, 19 - 0.005, -180f, 0f),
                meta -> meta.setScale(new Vec(4))
        );

        // The Illager Inn
        this.createGraffiti(
                Component.text("The", TextColor.color(0xFFFFFF)),
                new Pos(19 - Vec.EPSILON, 71.8, 17, 90f, 0f),
                meta -> meta.setScale(new Vec(2))
        );
        this.createGraffiti(
                Component.text("Illager", TextColor.color(0xFFFFFF)),
                new Pos(19 - Vec.EPSILON, 70.8, 19, 90f, 0f),
                meta -> meta.setScale(new Vec(4))
        );
        this.createGraffiti(
                Component.text("Inn", TextColor.color(0xFFFFFF)),
                new Pos(19 - Vec.EPSILON, 70.15, 20.5, 90f, 0f),
                meta -> meta.setScale(new Vec(3))
        );

        // Post office
        this.createGraffiti(
                Component.text("ᴘᴏѕᴛ ᴏꜰꜰɪᴄᴇ", TextColor.color(0xDD0000)),
                new Pos(16.5, 69.3, -20 + Vec.EPSILON, 0f, 0f),
                meta -> meta.setScale(new Vec(5))
        );

        // Graffiti next to post office
        this.createGraffiti(
                Component.text("\uE018"),
                new Pos(11 + Vec.EPSILON, 65.5, -22, -90f, 0f),
                meta -> {
                    meta.setScale(new Vec(7));
                    meta.setLeftRotation(BBModel.quatToFloats(new Quaternionf(0, 0, 0, 1).rotateXYZ(0, 0, (float)Math.toRadians(20))));
                }
        );
        this.createGraffiti(
                Component.text("\uE019"),
                new Pos(12 - Vec.EPSILON, 65.5, -26.5, 90f, 0f),
                meta -> {
                    meta.setScale(new Vec(7));
                }
        );

        this.createGraffiti(
                Component.text("\uE01A"),
                new Pos(24.5, 64.5, 24 + Vec.EPSILON, 0, 0f),
                meta -> {
                    meta.setScale(new Vec(18));
                    meta.setLeftRotation(BBModel.quatToFloats(new Quaternionf(0, 0, 0, 1).rotateXYZ(0, 0, (float)Math.toRadians(10))));
                }
        );

        // Donator board
        this.createGraffiti(
                Component.text()
                        .append(Component.text("Donators: ", NamedTextColor.WHITE))
                        .append(Component.text("❤", NamedTextColor.RED))
                        .build(),
                new Pos(-24 + Vec.EPSILON, 68.3, -1.5, -90, 0),
                meta -> {
                    meta.setScale(new Vec(2.5));
                    meta.setShadow(true);
                }
        );

        List<String> donatorValues = new ArrayList<>(DONATORS.keySet());
        donatorValues.sort(Comparator.comparingDouble(DONATORS::get).reversed().thenComparing(a -> ((String)a).toLowerCase()));

        double donatorZ = 0.7;
        double donatorY = 67.6;
        for (String donator : donatorValues) {
            double donated = DONATORS.get(donator);
            double scale = Math.min(Math.min(0.05 * donated + 0.2, 0.004 * donated + 0.8), 1.5);

            this.createGraffiti(
                    Component.text(donator),
                    new Pos(-24 + Vec.EPSILON, donatorY, donatorZ, -90, 0),
                    meta -> {
                        meta.setScale(new Vec(scale));
                        meta.setShadow(true);
                    }
            );
            donatorY -= 0.25 * scale;

            if (donatorY < 65) {
                donatorY = 67.6;
                donatorZ -= 2.1;
            }
        }



    }

    private @NotNull BetterEntity createGraffiti(@NotNull Component text, @NotNull Pos position, @NotNull Consumer<TextDisplayMeta> metaConsumer) {
        BetterEntity graffiti = new BetterEntity(EntityType.TEXT_DISPLAY);
        graffiti.setNoGravity(true);
        graffiti.setTicking(false);
        graffiti.setPhysics(false);

        TextDisplayMeta graffitiMeta = (TextDisplayMeta) graffiti.getEntityMeta();
        graffitiMeta.setBackgroundColor(0);
        graffitiMeta.setText(text);
        metaConsumer.accept(graffitiMeta);

        instance.loadChunk(position).thenRun(() -> graffiti.setInstance(instance, position));

        return graffiti;
    }

}
