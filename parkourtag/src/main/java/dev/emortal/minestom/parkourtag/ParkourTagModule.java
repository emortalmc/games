package dev.emortal.minestom.parkourtag;

import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.Jolt;
import com.github.stephengold.joltjni.JoltPhysicsObject;
import com.github.stephengold.joltjni.enumerate.EActivation;
import dev.emortal.minestom.core.Module;
import dev.emortal.minestom.core.game.GameCreator;
import dev.emortal.minestom.core.map.MapManager;
import dev.emortal.minestom.core.map.MapManagerImpl;
import dev.emortal.minestom.parkourtag.blockhandler.SignHandler;
import dev.emortal.minestom.parkourtag.physics.MinecraftPhysics;
import dev.emortal.minestom.parkourtag.physics.worldmesh.ChunkMesher;
import electrostatic4j.snaploader.LibraryInfo;
import electrostatic4j.snaploader.LoadingCriterion;
import electrostatic4j.snaploader.NativeBinaryLoader;
import electrostatic4j.snaploader.filesystem.DirectoryPath;
import electrostatic4j.snaploader.platform.NativeDynamicLibrary;
import electrostatic4j.snaploader.platform.util.PlatformPredicate;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockManager;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ParkourTagModule implements Module {

    public ParkourTagModule() {
        loadJoltJni();
    }

    private void loadJoltJni() {
        LibraryInfo libInfo = new LibraryInfo(null, "joltjni", DirectoryPath.USER_DIR);
        NativeBinaryLoader loader = new NativeBinaryLoader(libInfo);
        NativeDynamicLibrary[] libraries = {
                new NativeDynamicLibrary("linux/aarch64/com/github/stephengold", PlatformPredicate.LINUX_ARM_64),
//                new NativeDynamicLibrary("linux/armhf/com/github/stephengold", PlatformPredicate.LINUX_ARM_32),
                new NativeDynamicLibrary("linux/x86-64/com/github/stephengold", PlatformPredicate.LINUX_X86_64),
//                new NativeDynamicLibrary("osx/aarch64/com/github/stephengold", PlatformPredicate.MACOS_ARM_64),
//                new NativeDynamicLibrary("osx/x86-64/com/github/stephengold", PlatformPredicate.MACOS_X86_64),
//                new NativeDynamicLibrary("windows/x86-64/com/github/stephengold", PlatformPredicate.WIN_X86_64)
        };
        loader.registerNativeLibraries(libraries).initPlatformLibrary();
        try {
            loader.loadLibrary(LoadingCriterion.CLEAN_EXTRACTION);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load a Jolt-JNI native library!");
        }

        //Jolt.setTraceAllocations(true); // to log Jolt-JNI heap allocations
        JoltPhysicsObject.startCleaner(); // to reclaim native memory
        Jolt.registerDefaultAllocator(); // tell Jolt Physics to use malloc/free
        Jolt.installDefaultAssertCallback();
        Jolt.installDefaultTraceCallback();
        boolean success = Jolt.newFactory();
        assert success;
        Jolt.registerTypes();
    }

    @Override
    public String getId() {
        return "parkourtag";
    }

    @Override
    public int getMinPlayers() {
        return 2;
    }

    @Override
    public int getMaxPlayers() {
        return 8;
    }

    @Override
    public GameCreator getGameCreator() {
        return (info, map) -> {
            MinecraftPhysics physics = new MinecraftPhysics(map.instance());

            for (int x = -MapManagerImpl.CHUNK_LOADING_RADIUS; x < MapManagerImpl.CHUNK_LOADING_RADIUS; x++) {
                for (int z = -MapManagerImpl.CHUNK_LOADING_RADIUS; z < MapManagerImpl.CHUNK_LOADING_RADIUS; z++) {
                    CompletableFuture<BodyCreationSettings> future = map.instance().loadChunk(x, z)
                            .thenApply(ChunkMesher::createChunk);

                    future.thenAccept(a -> {
                        physics.getBodyInterface().createAndAddBody(a, EActivation.DontActivate);
                    });
                }
            }

            return new ParkourTagGame(info, map, physics);
        };
    }

    @Override
    public void postRegister() {
        registerSignHandlers();
    }

    @Override
    public MapManager getMapManager() {
        return new MapManagerImpl(getId(), Set.of("city", "ruins"));
    }

    private static void registerSignHandlers() {
        BlockManager blockManager = MinecraftServer.getBlockManager();
        blockManager.registerHandler("minecraft:sign", SignHandler::new);

        for (Block value : Block.values()) {
            if (value.name().endsWith("sign")) blockManager.registerHandler(value.key(), SignHandler::new);
        }
    }

}
