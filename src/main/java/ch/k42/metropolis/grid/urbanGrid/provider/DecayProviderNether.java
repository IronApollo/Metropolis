package ch.k42.metropolis.grid.urbanGrid.provider;

import java.util.Random;
import java.util.Set;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockCategories;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import ch.k42.metropolis.generator.MetropolisGenerator;
import ch.k42.metropolis.minions.Constants;
import ch.k42.metropolis.minions.DecayOption;
import com.google.common.collect.ImmutableSet;


/**
 * Provides decay to area of blocks.
 * Originally written by spaceribs for CityWorld.
 *
 * @author spaceribs, Thomas Richner
 */
public class DecayProviderNether extends DecayProvider {

    public DecayProviderNether(MetropolisGenerator generator, Random random) {
        super(generator, random);
    }

    /**
     * Destroys an area with custom decay scale
     *
     * @param x1      start x coordinate
     * @param x2      end x coordinate
     * @param y1      start y coordinate
     * @param y2      end y coordinate
     * @param z1      start z coordinate
     * @param z2      end z coordinate
     * @param options decay options
     */
    public void destroyWithin(int x1, int x2, int y1, int y2, int z1, int z2, DecayOption options) {

        if (y1 < 0) y1 = 0;
        if (y2 < 0) y2 = 0;

        int MAX = Constants.WORLD_HEIGHT;
        int groundHeight = Constants.BUILD_HEIGHT;
        if (y1 > MAX) y1 = MAX;
        if (y2 > MAX) y2 = MAX;

        double holeScale = options.getHoleScale();
        double fulldecay = options.getFullDecay() - 0.4;
        double partialdecay = options.getPartialDecay() - 0.4;

        World world = generator.getWorld();

        long seed = generator.getWorldSeed();
        SimplexOctaveGenerator noiseGen = new SimplexOctaveGenerator(seed, 2);

        for (int z = z1; z < z2; z++) {
            for (int x = x1; x < x2; x++) {
                for (int y = y1; y < y2; y++) {

                    double holeNoise = noiseGen.noise(x * holeScale, y * holeScale, z * holeScale, 0.3D, 0.6D, true);

                    Block block = world.getBlockAt(x, y, z);

                    if (options.getExceptions().contains(block.getType())) { // do we ignore this type of block?
                        continue;
                    }
                    if (BlockCategories.DOORS.contains(BukkitAdapter.adapt(block.getBlockData())) && BlockCategories.DOORS.contains(BukkitAdapter.adapt(block.getRelative(0, 1, 0).getBlockData()))) {
                        if (random.nextInt(100) < 80) {
                            Door door = (Door) block.getBlockData();
                            door.setOpen(true);
                            block.setBlockData(door);
                        }
                    }

                    if (block.getType() == Material.WATER) {
                        block.setType(Material.LAVA);
                    }

                    if (block.getType() == Material.WATER) {
                        block.setType(Material.LAVA);
                    }

                    if (block.getType() == Material.GRASS) {
                        block.setType(Material.DIRT);
                    }

                    if (block.getType() == Material.DANDELION && block.getType() == Material.POPPY) {
                        block.setType(Material.TALL_GRASS);
                    }


                    if (isDeleted(block)) {
                        block.setType(Material.AIR);
                    }

                    //if(true) return; //FIXME omitting problematic part
                    Block[] neighbors = {
                            block.getRelative(0, 1, 0),
                            block.getRelative(0, 0, -1),
                            block.getRelative(0, 0, 1),
                            block.getRelative(1, 0, 0),
                            block.getRelative(-1, 0, 0),
                            block.getRelative(0, -1, 0)
                    };

//                    if (isOnFire(block, neighbors[0]) && random.nextBoolean()) {
//                        block.setType(Material.FIRE);
//                        neighbors[0].setType(Material.FIRE);
//                    }

                    if (y < groundHeight - 1 && holeNoise > fulldecay) {
                        block.setType(Material.LAVA);
                    } else {

                        if (!block.isEmpty() && (holeNoise > fulldecay)) {
                            block.setType(Material.AIR);
                        } else if (isValid(block) && holeNoise > partialdecay) {
                            switch (block.getType()) { //TODO too many hardcoded values
                                case STONE:
                                case SANDSTONE:
                                case COBBLESTONE:
                                case BRICK:
                                case STONE_BRICKS:
                                    if (random.nextInt(100) < 40) break; // 40% nothing
                                    block.setType(Material.NETHERRACK);
                                    break;
                                default:
                                    block.setType(Material.AIR);
                                    break;
                            }

                            if (block.isEmpty() && !neighbors[5].isEmpty()) {
                                int prob = 0;
                                for (int i = 0; i < neighbors.length; i++) {
                                    prob += neighbors[i].isEmpty() ? 1 : 0;
                                }
                                if (random.nextInt(500) < prob) {
                                    block.setType(Material.LAVA);
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    protected static boolean isOnFire(Block block, Block above) {
        return (
                block.getType().isBurnable()
                        && above.isEmpty()
        );
    }

    private static Set<Material> deletedBlocks = ImmutableSet.of(
            Material.OAK_LEAVES,
            Material.SPRUCE_LEAVES,
            Material.ROSE_BUSH,
            Material.DANDELION,
            Material.POPPY,
            Material.GRASS,
            Material.TALL_GRASS,
            Material.VINE,
            Material.OAK_LOG,
            Material.SPRUCE_LOG,
            Material.WATER,
            Material.WATER
    );

    protected static boolean isDeleted(Block block) {
        return deletedBlocks.contains(block.getType());
    }
}
