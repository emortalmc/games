package dev.emortal.minestom.lobby.emote;

import dev.emortal.bbstom.BBAnimation;
import dev.emortal.bbstom.BBModel;
import dev.emortal.bbstom.renderer.AnimationRenderer;
import dev.emortal.minestom.core.playermodel.PlayerFigure;
import dev.emortal.minestom.core.playermodel.SkinLook;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.property.ServerProperties;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class EmoteRenderer extends AnimationRenderer {

    private String bodyUUID;
    private String leftArmUUID;
    private String rightArmUUID;
    private String leftLegUUID;
    private String rightLegUUID;
    private String headUUID;
    private String capeUUID;

    private final PlayerFigure figure;
    private @Nullable Task renderTask;

    public EmoteRenderer(BBModel model, BBAnimation animation, Instance instance, Pos position, PlayerSkin skin) {
        this.figure = new PlayerFigure(SkinLook.fromSkin(skin));
        super(model, animation, instance, position);
    }

    public EmoteRenderer(BBModel model, BBAnimation animation, Instance instance, Pos position, String username) {
        this.figure = new PlayerFigure(SkinLook.ofUsername(username));
        super(model, animation, instance, position);
    }

    @Override
    public void setModel(BBModel model) {
        super.setModel(model);
        this.bodyUUID = model.getElementUUID("Body");
        this.leftArmUUID = model.getElementUUID("Left Arm");
        this.rightArmUUID = model.getElementUUID("Right Arm");
        this.leftLegUUID = model.getElementUUID("Left Leg");
        this.rightLegUUID = model.getElementUUID("Right Leg");
        this.headUUID = model.getElementUUID("Head");
        this.capeUUID = model.getElementUUID("Cape");
    }

    @Override
    public void setInstance(Instance instance) {
        super.setInstance(instance);
        figure.setInstance(instance);
    }

    @Override
    public void render() {
        renderTask = MinecraftServer.getSchedulerManager().submitTask(new Supplier<>() {
            final float tickTime = 1f / ServerProperties.SERVER_TICKS_PER_SECOND.get();

            @Override
            public TaskSchedule get() {
                if (isPaused()) return TaskSchedule.park();

                Pos pos = getPosition();
                float time = getCurrentTime();
                BBAnimation animation = getAnimation();
                BBModel model = getModel();

                figure.teleport(pos);

                Quaternionf yawQuat = new Quaternionf().rotateY((float) Math.toRadians(-pos.yaw()));

                String prefix = "emortalmc:player/";
                Entity headDisplay = figure.getDisplay(prefix + "head");
                if (headDisplay == null) return TaskSchedule.tick(1);
                renderEntity(yawQuat, model, animation, headDisplay, headUUID, time);
                renderEntity(yawQuat, model, animation, figure.getDisplay(prefix + "body"), bodyUUID, time);
                Entity leftArm = figure.getDisplay(prefix + "left_arm");
                if (leftArm == null) leftArm = figure.getDisplay(prefix + "left_arm_slim");
                Entity rightArm = figure.getDisplay(prefix + "right_arm");
                if (rightArm == null) rightArm = figure.getDisplay(prefix + "right_arm_slim");
                renderEntity(yawQuat, model, animation, leftArm, leftArmUUID, time);
                renderEntity(yawQuat, model, animation, rightArm, rightArmUUID, time);
                renderEntity(yawQuat, model, animation, figure.getDisplay(prefix + "left_leg"), leftLegUUID, time);
                renderEntity(yawQuat, model, animation, figure.getDisplay(prefix + "right_leg"), rightLegUUID, time);

                Entity capeDisplay = figure.getDisplay(prefix + "cape");
                if (capeDisplay != null) renderEntity(yawQuat, model, animation, capeDisplay, capeUUID, time);

                setCurrentTime(time + tickTime * getAnimationSpeed());
                if (time > animation.length()) {
                    if (animation.loop().equals("loop")) {
                        setCurrentTime(time % animation.length());
                    }
                }

                return TaskSchedule.tick(1);
            }
        });
    }

    public PlayerFigure getFigure() {
        return figure;
    }

    @Override
    public void remove() {
        figure.remove();
        if (renderTask != null) renderTask.cancel();
    }

    private void renderEntity(Quaternionf yawQuat, BBModel model, BBAnimation animation, Entity entity, String uuid, float time) {
        Matrix4f m = model.getTransform(animation, time, uuid, BBModel.BakeMode.BIND_POSE, new Vector3f(), new Quaternionf().rotationY((float) Math.PI));
        Vector3f vec = m.getTranslation(new Vector3f()).div(16);
        Quaternionf quat = m.getUnnormalizedRotation(new Quaternionf()).normalize();

        yawQuat.transform(vec);
        quat.premul(yawQuat);

        entity.editEntityMeta(AbstractDisplayMeta.class, meta -> {
            meta.setTransformationInterpolationDuration(1);
            meta.setTransformationInterpolationStartDelta(0);
            meta.setLeftRotation(BBModel.quatToFloats(quat));
            meta.setTranslation(new Vec(vec.x, vec.y, vec.z));
        });
    }
}
