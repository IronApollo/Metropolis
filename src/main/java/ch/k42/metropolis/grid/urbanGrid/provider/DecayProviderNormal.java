package ch.k42.metropolis.grid.urbanGrid.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.material.Leaves;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import ch.k42.metropolis.generator.MetropolisGenerator;
import ch.k42.metropolis.minions.Constants;
import ch.k42.metropolis.minions.DecayOption;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.util.EditSessionBuilder;
import com.google.common.collect.ImmutableSet;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

/**
 * Provides decay to area of blocks.
 * Originally written by spaceribs for CityWorld.
 *
 * @author spaceribs, Thomas Richner
 */
public class DecayProviderNormal extends DecayProvider {

    private static Set<Material> unsupportingBlocks = ImmutableSet.of(
            Material.LEAVES,
            Material.LEAVES_2,
            Material.DOUBLE_PLANT,
            Material.GRASS,
            Material.LONG_GRASS,
            Material.VINE,
            Material.LOG,
            Material.LOG_2,
            Material.WATER,
            Material.STATIONARY_WATER,
            Material.LAVA,
            Material.STATIONARY_LAVA
    );

    private static Set<Material> invalidBlocks = ImmutableSet.<Material>builder().addAll(unsupportingBlocks).build();

    public DecayProviderNormal(MetropolisGenerator generator, Random random) {
        super(generator, random);
    }

//    public void destroyWithin(int x1, int x2, int y1, int y2, int z1, int z2, DecayOption options) {
//
//        if (y1 < 0) y1 = 0;
//        if (y2 < 0) y2 = 0;
//
//        int MAX = Constants.WORLD_HEIGHT;
//        if (y1 > MAX) y1 = MAX;
//        if (y2 > MAX) y2 = MAX;
//
//        double holeScale = options.getHoleScale();
//        double leavesScale = options.getLeavesScale();
//        double fulldecay = options.getFullDecay();
//        double partialdecay = options.getPartialDecay();
//        double leavesdecay = options.getLeavesdecay();
//
//        World world = generator.getWorld();
//
//        long seed = generator.getWorldSeed();
//        SimplexOctaveGenerator simplexOctaveGenerator = new SimplexOctaveGenerator(seed, 2);
//        EditSession session = new EditSessionBuilder(FaweAPI.getWorld(world.getName())).changeSetNull().autoQueue(false).fastmode(true).checkMemory(false).build();
//        
////        CuboidClipboard cb = new CuboidClipboard(new CuboidRegion(new Vector(x1,y1,z1), new Vector(x2,y2,z2)));
////        CuboidClipboard cb = new CuboidClipboard(new Vector(x2-x1, y2-y1, z2-z1), new Vector(x1, y1, z1));
//        try {
//        for (int z = z1; z < z2; z++) {
//            for (int x = x1; x < x2; x++) {
//                for (int y = y1; y < y2; y++) {
//                    Block block = world.getBlockAt(x, y, z);
//                    Vector v = new Vector(x, y, z);
//                    Vector v1 = new Vector(x-x1, y-y1,z-z1);
//                    // do we ignore this type of block? is it already empty?
//
//                    if (!isValid(block) || !block.isEmpty() || !options.getExceptions().contains(block.getType())) {
//
//                        double noise = simplexOctaveGenerator.noise(x * holeScale, y * holeScale, z * holeScale, 0.3D, 0.6D, true);
//                        session.setBlock(v1, session.getBlock(v));
//                        if (noise > fulldecay) {
////                            block.setType(Material.AIR);
//                        	session.setBlock(v1, ne BaseBlock());
//                            // we may add leaves if it's supporting
//
//                        }else if (noise > partialdecay) {
//
//                            // alter block
//                            switch (block.getType()) { //TODO too many hardcoded values
//                                case STONE:
//                                    if (random.nextInt(100) > 40) { // 40% happens nothing
//                                        if (random.nextBoolean()){
////                                            block.setType(Material.COBBLESTONE);
//                                        	cb.getBlock(v1).setType(Material.COBBLESTONE.getId());
//                                        }else{
////                                            block.setType(Material.MOSSY_COBBLESTONE);
//                                        	cb.getBlock(v1).setType(Material.MOSSY_COBBLESTONE.getId());
//                                        }
//                                    }
//                                    break;
//                                case SANDSTONE:
//                                    if (random.nextBoolean()){
////                                        block.setTypeIdAndData(Material.SANDSTONE_STAIRS.getId(), (byte) random.nextInt(4), true);
//                                    	cb.getBlock(v1).setIdAndData(Material.SANDSTONE_STAIRS.getId(), (byte)random.nextInt(4));
//                                    }
//                                    break;
//                                case BRICK:
//                                    if (random.nextBoolean()) {
////                                        block.setTypeIdAndData(Material.BRICK_STAIRS.getId(), (byte) random.nextInt(4), true);
//                                    	cb.getBlock(v1).setIdAndData(Material.BRICK_STAIRS.getId(), (byte)random.nextInt(4));
//                                    }
//                                    break;
//                                case COBBLESTONE:
////                                    block.setTypeIdAndData(Material.MOSSY_COBBLESTONE.getId(), (byte) random.nextInt(4), true);
//                                	cb.getBlock(v1).setIdAndData(Material.MOSSY_COBBLESTONE.getId(), (byte)random.nextInt(4));
//                                    break;
//                                case SMOOTH_BRICK:
////                                    block.setTypeIdAndData(Material.SMOOTH_BRICK.getId(), (byte) random.nextInt(3), true);
//                                	cb.getBlock(v1).setIdAndData(Material.SMOOTH_BRICK.getId(), (byte)random.nextInt(3));
//                                    break;
//                                case WOOD:
//                                    if (random.nextBoolean()) break; // not too much stairs
//                                    switch (block.getData()) {
//                                        case 0:
////                                            block.setTypeIdAndData(Material.WOOD_STAIRS.getId(), (byte) random.nextInt(4), true);
//                                        	cb.getBlock(v1).setIdAndData(Material.WOOD_STAIRS.getId(), (byte)random.nextInt(4));
//                                            break;
//                                        case 1:
////                                            block.setTypeIdAndData(Material.SPRUCE_WOOD_STAIRS.getId(), (byte) random.nextInt(4), true);
//                                        	cb.getBlock(v1).setIdAndData(Material.SPRUCE_WOOD_STAIRS.getId(), (byte)random.nextInt(4));
//                                            break;
//                                        case 2:
////                                            block.setTypeIdAndData(Material.BIRCH_WOOD_STAIRS.getId(), (byte) random.nextInt(4), true)
//                                        	cb.getBlock(v1).setIdAndData(Material.BIRCH_WOOD_STAIRS.getId(), (byte)random.nextInt(4));
//                                            break;
//                                        default:
////                                            block.setTypeIdAndData(Material.JUNGLE_WOOD_STAIRS.getId(), (byte) random.nextInt(4), true);
//                                        	cb.getBlock(v1).setIdAndData(Material.JUNGLE_WOOD_STAIRS.getId(), (byte)random.nextInt(4));
//                                            break;
//                                    }
//                                    break;
//                                case WOODEN_DOOR: // randomly open wooden doors
//                                    if (Material.WOODEN_DOOR.equals(block.getRelative(0, 1, 0).getType())) {
//                                        if (random.nextInt(100) < 80) {
//                                            byte data = block.getData();
//                                            data ^= 4;
////                                            block.setData(data);
//                                        	cb.getBlock(v1).setData(data);
//                                        }
//                                    }
//                                    break;
//                                default:
////                                    block.setType(Material.AIR);
//                                	cb.getBlock(v1).setId(Material.AIR.getId());
//
//                                    break;
//                            }
//
//                            // can we attach leaves?
//                            if(isSupporting(block)){
//                                for(Block n1 : getNeighbours(block)){
//                                    // add them leaves
//                                    addLeavesRec(n1,options,2);
//                                }
//                            }
//                        }
//
//                    }
//                }
//            }
//        }
////        Schematic s = new Schematic(cb);
//        cb.paste(session, new Vector(x1, y1, z1), true);
////        s.paste(session.getWorld(), new Vector(x1, y1, z1), false, true, null);
////        session.flushQueue();
//        }catch (Exception e) {
//        	e.printStackTrace();
//        }
//    }

    private void addLeavesRec(Block block, DecayOption option, int depth){
        // recursion done?
        if(depth>0){
            // block free?
            if(block.isEmpty()){
                // should we set a leaf?
                if(random.nextDouble()<option.getLeavesScale()) { // should we add leaves?
                    //set leaf here
                    Leaves leaf = getRandomLeaves();
                    block.setType(leaf.getItemType());
                    block.setData(leaf.getData());
                    //since this is now supporting, set leafes around
                    for (Block n2 : getNeighbours(block)) {
                        addLeavesRec(n2, option, depth - 1);
                    }
                }
            }
        }
    }

    private TreeSpecies[] treeSpecies =  TreeSpecies.values();

    private Leaves getRandomLeaves(){
        return new Leaves(treeSpecies[random.nextInt(treeSpecies.length)]);
    }

    /**
     * Calculates all directly connected blocks, without
     * blocks that would cross chunk borders (prevent endless recursion)
     * @param block
     * @return at maximum 6 blocks, all facing 'block'
     */
    private List<Block> getNeighbours(Block block){
        List<Block> neighbours = new ArrayList<>(6);

        // block above
        if(block.getY()<(block.getWorld().getMaxHeight()-1)){
            neighbours.add(block.getRelative(0,1,0));
        }

        // block below
        if(block.getY()>0){
            neighbours.add(block.getRelative(0,-1,0));
        }

        //block in x direction

        // on chunk border?
        if(block.getX()%16!=0){
            neighbours.add(block.getRelative(-1,0,0));
        }

        if(block.getX()%16!=15){
            neighbours.add(block.getRelative(1,0,0));
        }

        // Z direction
        if(block.getZ()%16!=0){
            neighbours.add(block.getRelative(0,0,-1));
        }

        if(block.getZ()%16!=15){
            neighbours.add(block.getRelative(0,0,1));
        }

        return neighbours;
    }


    protected boolean isValid(Block block) {
        return !invalidBlocks.contains(block.getType());
    }

    protected boolean isSupporting(Block block) {
        return !unsupportingBlocks.contains(block.getType()) && !block.isEmpty();
    }
}
