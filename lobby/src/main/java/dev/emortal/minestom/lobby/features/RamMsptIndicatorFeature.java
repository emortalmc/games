package dev.emortal.minestom.lobby.features;

import dev.emortal.minestom.lobby.util.entity.BetterEntity;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.nio.file.Files;
import java.nio.file.Path;

public final class RamMsptIndicatorFeature implements LobbyFeature {
    // Used to rotate the bars by 270 degrees so that they scale upwards
    private static final Quaternionf BAR_QUATERNION = new Quaternionf(new AxisAngle4f((float) Math.toRadians(270), 1, 0, 0));
    private static final float[] BAR_LEFT_ROTATION = new float[] {
            BAR_QUATERNION.w(), BAR_QUATERNION.x(), BAR_QUATERNION.y(), BAR_QUATERNION.z()
    };

    @Override
    public void register(@NotNull Instance instance) {
        this.createMsptPillar(instance);
        this.createRamPillar(instance);
    }

    private void createMsptPillar(@NotNull Instance instance) {
        DisplayPillar pillar = new DisplayPillar(Block.RED_CONCRETE);

        MinecraftServer.getGlobalEventHandler().addListener(ServerTickMonitorEvent.class, event -> {
            double tickTime = event.getTickMonitor().getTickTime();
            int ms = (int) tickTime;

            pillar.setText(Component.text(ms + "ms"));
            pillar.setBarPercent(Math.min(2, (tickTime / 50.0) * 2));
        });

        pillar.place(instance, PillarPlacement.MSPT);
    }

    private void createRamPillar(@NotNull Instance instance) {
        DisplayPillar pillar = new DisplayPillar(Block.LIME_CONCRETE);

        instance.scheduler().buildTask(() -> {
            Runtime runtime = Runtime.getRuntime();
            long heapUsed = runtime.totalMemory() - runtime.freeMemory();
            long residentSet = residentSetSizeBytes();
            long used = residentSet > 0 ? residentSet : heapUsed;

            pillar.setText(Component.text(used / 1024 / 1024 + "MB"));
            pillar.setBarPercent(Math.min(2, (double) used / runtime.maxMemory() * 2));
        }).repeat(TaskSchedule.seconds(1)).schedule();

        pillar.place(instance, PillarPlacement.RAM);
    }

    private static long residentSetSizeBytes() {
        try {
            for (String line : Files.readAllLines(Path.of("/proc/self/status"))) {
                if (line.startsWith("VmRSS:")) {
                    return Long.parseLong(line.split("\\s+")[1]) * 1024;
                }
            }
        } catch (Exception exception) {
            return -1;
        }
        return -1;
    }

    private static final class DisplayPillar {

        private final @NotNull BetterEntity bar;
        private final @NotNull BetterEntity text;

        DisplayPillar(@NotNull Block barBlock) {
            this.bar = this.createBar(barBlock);
            this.text = this.createText();
        }

        void setText(@NotNull Component text) {
            TextDisplayMeta meta = (TextDisplayMeta) this.text.getEntityMeta();
            meta.setText(text);
        }

        void setBarPercent(double percent) {
            BlockDisplayMeta meta = (BlockDisplayMeta) this.bar.getEntityMeta();
            meta.setScale(new Vec(0.501, percent, 0.501));
        }

        void place(@NotNull Instance instance, @NotNull PillarPlacement placement) {
            this.bar.setInstance(instance, placement.bar());
            this.text.setInstance(instance, placement.text());
        }

        private @NotNull BetterEntity createBar(@NotNull Block block) {
            BetterEntity entity = new BetterEntity(EntityType.BLOCK_DISPLAY);
            entity.setTicking(false);

            BlockDisplayMeta meta = (BlockDisplayMeta) entity.getEntityMeta();
            meta.setScale(Vec.ZERO);
            meta.setWidth(2);
            meta.setHeight(4);
            meta.setBlockState(block);

            return entity;
        }

        private @NotNull BetterEntity createText() {
            BetterEntity entity = new BetterEntity(EntityType.TEXT_DISPLAY);
            entity.setTicking(false);

            TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
            meta.setBackgroundColor(0);
            meta.setAlignRight(true);
            meta.setLeftRotation(BAR_LEFT_ROTATION);
            meta.setWidth(2);
            meta.setHeight(4);
            meta.setTextOpacity((byte) 120);
            meta.setText(Component.empty());

            return entity;
        }
    }

    private record PillarPlacement(@NotNull Pos bar, @NotNull Pos text) {
        static @NotNull PillarPlacement MSPT = new PillarPlacement(new Pos(1.2495, 69, -38.7505), new Pos(1.65, 70, -38.7505));
        static @NotNull PillarPlacement RAM = new PillarPlacement(new Pos(-0.7505, 69, -38.7505), new Pos(-0.35, 70, -38.7505));
    }
}
