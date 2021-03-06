package me.taylorkelly.bigbrother.datablock;

import java.util.ArrayList;

import me.taylorkelly.bigbrother.BBPlayerInfo;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;

public class Flow extends BBDataBlock {

    private ArrayList<BBDataBlock> bystanders;

    public Flow(String player, Block block, String world) {
        super(player, Action.FLOW, world, block.getX(), block.getY(), block.getZ(), block.getTypeId(), Byte.toString(block.getData()));
        bystanders = new ArrayList<BBDataBlock>();
        // TODO can't lava flow break blocks?
    }

    public Flow(String player, String world, int x, int y, int z, int type, byte data) {
        super(player, Action.FLOW, world, x, y, z, type, Byte.toString(data));
        bystanders = new ArrayList<BBDataBlock>();
    }

    public void send() {
        for (BBDataBlock block : bystanders) {
            block.send();
        }
        super.send();
    }

    private Flow(BBPlayerInfo player, String world, int x, int y, int z, int type, String data) {
        super(player, Action.FLOW, world, x, y, z, type, data);
    }

    public void rollback(World wld) {
        World currWorld = wld;//server.getWorld(world);
        if (!currWorld.isChunkLoaded(x >> 4, z >> 4)) {
            currWorld.loadChunk(x >> 4, z >> 4);
        }

        currWorld.getBlockAt(x, y, z).setTypeId(0);
    }

    public void redo(Server server) {
    }

    public static BBDataBlock getBBDataBlock(BBPlayerInfo pi, String world, int x, int y, int z, int type, String data) {
        return new Flow(pi, world, x, y, z, type, data);
    }
}
