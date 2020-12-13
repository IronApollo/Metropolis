package ch.k42.metropolis.grid.urbanGrid.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import ch.k42.metropolis.generator.MetropolisGenerator;
import ch.k42.metropolis.minions.Constants;
import ch.k42.metropolis.minions.DecayOption;

import com.google.common.collect.ImmutableSet;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.regions.CuboidRegion;

/**
 * Provides decay to area of blocks.
 * Originally written by spaceribs for CityWorld.
 *
 * @author spaceribs, Thomas Richner
 */
public class DecayProviderNormal extends DecayProvider {

    private static Set<Material> unsupportingBlocks = ImmutableSet.of(
            Material.OAK_LEAVES,
            Material.JUNGLE_LEAVES,
            Material.TALL_GRASS,
            Material.GRASS,
            //Material.LONG_GRASS,
            Material.VINE,
            Material.OAK_LOG,
            Material.JUNGLE_LOG,
            Material.WATER,
            //Material.STATIONARY_WATER,
            Material.LAVA
            //Material.STATIONARY_LAVA
    );

    private static Set<Material> invalidBlocks = ImmutableSet.<Material>builder().addAll(unsupportingBlocks).build();

    public DecayProviderNormal(MetropolisGenerator generator, Random random) {
        super(generator, random);
    }

    public void destroyWithin(int x1, int x2, int y1, int y2, int z1, int z2, DecayOption options) {

        if (y1 < 0) y1 = 0;
        if (y2 < 0) y2 = 0;

        int MAX = Constants.WORLD_HEIGHT;
        if (y1 > MAX) y1 = MAX;
        if (y2 > MAX) y2 = MAX;

        double holeScale = options.getHoleScale();
        double leavesScale = options.getLeavesScale();
        double fulldecay = options.getFullDecay();
        double partialdecay = options.getPartialDecay();
        double leavesdecay = options.getLeavesdecay();

        World world = generator.getWorld();

        long seed = generator.getWorldSeed();
        SimplexOctaveGenerator simplexOctaveGenerator = new SimplexOctaveGenerator(seed, 2);

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(generator.getWorld());
        BlockArrayClipboard clipboard = new BlockArrayClipboard(new CuboidRegion(weWorld, BlockVector3.at(x1, y1, z1), BlockVector3.at(x2, y2, z2)));
        try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).build()) {
        for (int z = z1; z < z2; z++) {
            for (int x = x1; x < x2; x++) {
                for (int y = y1; y < y2; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    BlockVector3 v = BlockVector3.at(x, y, z);
                    BlockVector3 v1 = BlockVector3.at(x-x1, y-y1,z-z1);
                    // do we ignore this type of block? is it already empty?

                    if (!isValid(block) || !block.isEmpty() || !options.getExceptions().contains(block.getType())) {

                        double noise = simplexOctaveGenerator.noise(x * holeScale, y * holeScale, z * holeScale, 0.3D, 0.6D, true);
                        session.setBlock(v1, session.getBlock(v));
                        if (noise > fulldecay) {
                            clipboard.setBlock(v1, BlockTypes.AIR.getDefaultState());
                            // we may add leaves if it's supporting

                        }else if (noise > partialdecay) {

                            // alter block
                            switch (block.getType()) { //TODO too many hardcoded values
                                case STONE:
                                    if (random.nextInt(100) > 40) { // 40% happens nothing
                                        if (random.nextBoolean()){
                                            clipboard.setBlock(v1, BlockTypes.COBBLESTONE.getDefaultState());
                                        }else{
                                            clipboard.setBlock(v1, BlockTypes.MOSSY_COBBLESTONE.getDefaultState());
                                        }
                                    }
                                    break;
                                case COBBLESTONE:
                                	clipboard.setBlock(v1, BlockTypes.MOSSY_COBBLESTONE.getDefaultState());
                                    break;
                                case SANDSTONE:
                                case STONE_BRICKS:
                                case BRICK:
                                case OAK_PLANKS:
                                case SPRUCE_PLANKS:
                                case BIRCH_PLANKS:
                                case JUNGLE_PLANKS:
                                case DARK_OAK_PLANKS:
                                case ACACIA_PLANKS:
                                    if (random.nextBoolean()) break; // not too much stairs
                                    String baseName = block.getType().toString().replaceAll("_PLANKS", "");
                                    if (baseName.endsWith("S")){
                                        baseName.substring(0, baseName.length()-2);
                                    }
                                    if (random.nextBoolean()) { //stairs
                                        baseName += "_STAIRS";
                                        block.setType(Material.valueOf(baseName));
                                        Stairs stairs = (Stairs) block.getBlockData().clone();
                                        stairs.setShape(Stairs.Shape.values()[random.nextInt(Stairs.Shape.values().length-1)]);
                                        stairs.setHalf(Bisected.Half.values()[random.nextInt(Bisected.Half.values().length-1)]);
                                        stairs.setFacing(BlockFace.values()[random.nextInt(BlockFace.values().length-1)]);
                                        clipboard.setBlock(v1, BukkitAdapter.adapt(stairs));
                                    } else { //slabs
                                        baseName += "_SLAB";
                                        block.setType(Material.valueOf(baseName));
                                        Slab slab = (Slab) block.getBlockData();
                                        slab.setType(Slab.Type.values()[random.nextInt(Slab.Type.values().length-1)]);
                                        clipboard.setBlock(v1, BukkitAdapter.adapt(slab));
                                    }
                                    break;
                                case OAK_DOOR:
                                case SPRUCE_DOOR:
                                case BIRCH_DOOR:
                                case JUNGLE_DOOR:
                                case DARK_OAK_DOOR:
                                case ACACIA_DOOR:
                                case IRON_DOOR:
                                    Door door = (Door) block.getBlockData();
                                    door.setOpen(true);
                                    clipboard.setBlock(v1, BukkitAdapter.adapt(door));
                                default:
                                    clipboard.setBlock(v1, BlockTypes.AIR.getDefaultState());
                                    break;
                            }

                            // can we attach leaves?
                            if(isSupporting(block)){
                                for(Block n1 : getNeighbours(block)){
                                    // add them leaves
                                    addLeavesRec(clipboard, n1 ,options , 2);
                                }
                            }
                        }

                    }
                }
            }
        }
        Operation operation = new ClipboardHolder(clipboard).createPaste(session).to(BlockVector3.at(x1, y1, z1)).build();
        Operations.complete(operation);
        }catch (Exception e) {
        	e.printStackTrace();
        }
    }

    private void addLeavesRec(Clipboard clipboard, Block block, DecayOption option, int depth) throws WorldEditException {
        // recursion done?
        if(depth>0){
            // block free?
            if(block.isEmpty()){
                // should we set a leaf?
                if(random.nextDouble() < option.getLeavesScale()) { // should we add leaves?
                    //set leaf here
                    Material leavesMaterial = getRandomLeaves();
                    block.setType(leavesMaterial);
                    clipboard.setBlock(BukkitAdapter.asBlockVector(block.getLocation()), BukkitAdapter.adapt(block.getBlockData()));                   //since this is now supporting, set leafes around
                    for (Block n2 : getNeighbours(block)) {
                        addLeavesRec(clipboard, n2, option, depth - 1);
                    }
                }
            }
        }
    }

    private Material[] leafSpecies =  new Material[]{Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES, Material.JUNGLE_LEAVES, Material.DARK_OAK_LEAVES, Material.ACACIA_LEAVES};

    private Material getRandomLeaves(){
        return leafSpecies[random.nextInt(leafSpecies.length)];
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
