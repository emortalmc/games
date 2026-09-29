package dev.emortal.minestom.parkourtag.physics;

import com.github.stephengold.joltjni.Body;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import dev.emortal.minestom.parkourtag.physics.objects.MinecraftPhysicsObject;
import dev.emortal.minestom.parkourtag.physics.objects.RagdollPhysics;
import dev.emortal.minestom.parkourtag.utils.PTQuaternion;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.property.ServerProperties;
import net.minestom.server.timer.TaskSchedule;

import java.util.concurrent.ThreadLocalRandom;

import static dev.emortal.minestom.parkourtag.utils.CoordinateUtils.toRVec3;
import static dev.emortal.minestom.parkourtag.utils.CoordinateUtils.toVec3;

public class PlayerRagdoll {

    public static MinecraftPhysicsObject spawnRagdollWithImpulse(MinecraftPhysics physics, Player player, Pos startPos, Point impulse) {
        Instance instance = player.getInstance();

        double playerSize = 1;

        // all halves \/
        Vec torsoSize = new Vec(4.0f/16.0f, 6.0f/16.0f, 2.0f/16.0f).mul(playerSize);
//            Vector3 headSize = new Vector3(4.0f/16.0f, 4.0f/16.0f, 4.0f/16.0f);
        Vec headSize = new Vec(3f/16.0f, 3.0f/16.0f, 3f/16.0f).mul(playerSize);
//            Vector3 limbSize = new Vector3(2.0f/16.0f, 6.0f/16.0f, 2.0f/16.0f);
        Vec limbSize = new Vec(1f/16.0f, 6.0f/16.0f, 1f/16.0f).mul(playerSize);

        startPos = startPos.add(0, 1.5, 0);

        float yaw = -startPos.yaw() + 180;
        double yawRad = Math.toRadians(yaw);
        PTQuaternion yawQuat = new PTQuaternion(new Vec(0, 1, 0), yawRad);
        Quat yawQuat2 = new Quat((float) yawQuat.getX(), (float) yawQuat.getY(), (float) yawQuat.getZ(), (float) yawQuat.getW());

        MinecraftPhysicsObject torso = new RagdollPhysics(physics, player,null, PlayerDisplayPart.TORSO, toRVec3(startPos), yawQuat2, torsoSize);
        Body torsoBody = torso.getBody();
        MinecraftPhysicsObject head = new RagdollPhysics(physics, player, torsoBody, PlayerDisplayPart.HEAD, toRVec3(startPos.add(new Vec(0,  torsoSize.y() * 2, 0).rotateAroundY(yawRad).mul(playerSize))), yawQuat2, headSize);
        MinecraftPhysicsObject rightArm = new RagdollPhysics(physics, player, torsoBody, PlayerDisplayPart.RIGHT_ARM, toRVec3(startPos.add(new Vec((torsoSize.x() / 1.35) * 2, 0, 0).rotateAroundY(yawRad).mul(playerSize))), yawQuat2, limbSize);
        MinecraftPhysicsObject leftArm = new RagdollPhysics(physics, player, torsoBody, PlayerDisplayPart.LEFT_ARM, toRVec3(startPos.add(new Vec((torsoSize.x() / 1.35) * -2, 0, 0).rotateAroundY(yawRad).mul(playerSize))), yawQuat2, limbSize);
        MinecraftPhysicsObject rightLeg = new RagdollPhysics(physics, player, torsoBody, PlayerDisplayPart.RIGHT_LEG, toRVec3(startPos.add(new Vec(0.13, -0.72, 0).rotateAroundY(yawRad).mul(playerSize))), yawQuat2, limbSize);
        MinecraftPhysicsObject leftLeg = new RagdollPhysics(physics, player, torsoBody, PlayerDisplayPart.LEFT_LEG, toRVec3(startPos.add(new Vec(-0.13, -0.72, 0).rotateAroundY(yawRad).mul(playerSize))), yawQuat2, limbSize);

        torso.setInstance();
        head.setInstance();
        rightArm.setInstance();
        leftArm.setInstance();
        rightLeg.setInstance();
        leftLeg.setInstance();

        RVec3 torsoPos = torso.getBody().getPosition();
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        RVec3 impulsePos = new RVec3(rand.nextFloat(-1f, 1f), rand.nextFloat(-1.5f, 1.5f), rand.nextFloat(-1f, 1f));
        impulsePos.addInPlace(torsoPos.xx(), torsoPos.yy(), torsoPos.zz());
        torso.getBody().addImpulse(toVec3(impulse), impulsePos);

        instance.scheduler().buildTask(() -> {
            torso.destroy();
            head.destroy();
            rightArm.destroy();
            leftLeg.destroy();
            rightLeg.destroy();
            leftArm.destroy();
        }).delay(TaskSchedule.tick(20 * ServerProperties.SERVER_TICKS_PER_SECOND.get())).schedule();

        return torso;
    }

}
