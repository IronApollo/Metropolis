package ch.k42.metropolis.generator;

import ch.k42.metropolis.generator.populators.BedrockFloorPopulator;
import ch.k42.metropolis.generator.populators.CavePopulator;
import ch.k42.metropolis.generator.populators.OrePopulator;
import ch.k42.metropolis.grid.common.Factory;
import ch.k42.metropolis.grid.common.GridProvider;
import ch.k42.metropolis.grid.urbanGrid.clipboard.ClipboardProvider;
import ch.k42.metropolis.grid.urbanGrid.context.ContextProvider;
import ch.k42.metropolis.grid.urbanGrid.provider.DecayProvider;
import ch.k42.metropolis.grid.urbanGrid.provider.DecayProviderNether;
import ch.k42.metropolis.grid.urbanGrid.provider.DecayProviderNormal;
import ch.k42.metropolis.grid.urbanGrid.provider.EnvironmentProvider;
import ch.k42.metropolis.grid.urbanGrid.provider.NetherEnvironmentProvider;
import ch.k42.metropolis.grid.urbanGrid.provider.NormalEnvironmentProvider;
import ch.k42.metropolis.minions.Minions;
import ch.k42.metropolis.plugin.MetropolisPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Thomas
 * Date: 16.09.13
 * Time: 00:29
 * To change this template use File | Settings | File Templates.
 */
public class MetropolisGenerator extends ChunkGenerator {

    private class MetropolisBlockPopulator extends BlockPopulator {

        @Override
        public void populate(World aWorld, Random random, final Chunk chunk) { // we should make sure that the whol
            gridProvider.getGrid(chunk.getX(),chunk.getZ()).populate(chunk);
        }
    }

    private MetropolisPlugin plugin;
    private Long worldSeed;
    private World world;

    public String worldName;


    //TODO those don't belong here -> MetropolisBlockPopulator
    private ClipboardProvider clipboardProvider;
    private GridProvider gridProvider;
    private ContextProvider contextProvider;

    private DecayProvider decayProvider; //NOT PUBLIC!
    private EnvironmentProvider natureDecay; //NOT PUBLIC!
    // ENDTODO

    public MetropolisGenerator(MetropolisPlugin plugin, String worldName, ClipboardProvider clipboardProvider) {
        this.clipboardProvider = clipboardProvider;
        this.plugin = plugin;
        this.worldName = worldName;

        Minions.i("Running MetropolisGenerator for world: %s",worldName);
    }

    public ClipboardProvider getClipboardProvider() {
        return clipboardProvider;
    }

    public GridProvider getGridProvider() {
        return gridProvider;
    }

    public ContextProvider getContextProvider() {
        return contextProvider;
    }

    public DecayProvider getDecayProvider() {
        return decayProvider;
    }

    public EnvironmentProvider getNatureDecayProvider() {
        return natureDecay;
    }

    public World getWorld() {
        return world;
    }

    public MetropolisPlugin getPlugin() {
        return plugin;
    }

    public Long getWorldSeed() {
        return worldSeed;
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        List<BlockPopulator> populators = new ArrayList<>();
        Minions.d("getDefaultPopulators: " + world.toString());

        this.world = world;
        this.worldSeed = world.getSeed();
        this.gridProvider = new GridProvider(this);
        this.contextProvider = Factory.getDefaultContextProvider(this, plugin.getContextConfig());


        /*
         * We should decouple them from here and decouple
         * the populator config from the plugin config
         */
        populators.add(new MetropolisBlockPopulator());
        populators.add(new CavePopulator());
        populators.add(new OrePopulator(world, plugin.getPopulatorConfig().getOres())); // last place some ore
        populators.add(new BedrockFloorPopulator());

        return populators;
    }

    @Override
    public boolean canSpawn(World world, int x, int z) {
        return true;
    }

    @Override
    public ChunkData generateChunkData(World aWorld, Random random, int chunkX, int chunkZ, BiomeGrid biomes) {
        if (natureDecay == null || decayProvider == null) { //FIXME TODO FUCKUP THIS IS NOT NECESSARY, use generator id (see MetropolisPlugin) memoization of providers, singletons
            if (aWorld.getEnvironment() == World.Environment.NETHER) {
                decayProvider = new DecayProviderNether(this,new Random(aWorld.getSeed()));//new DecayProviderNether(this, new Random(aWorld.getSeed() + 6)); // why add 6 ?
                natureDecay = new NetherEnvironmentProvider(aWorld.getSeed());//new NetherEnvironmentProvider(aWorld.getSeed());
            } else {
                decayProvider = new DecayProviderNormal(this,new Random(aWorld.getSeed()+10));//DecayProviderNormal(this, new Random(aWorld.getSeed() + 6));
                natureDecay = new NormalEnvironmentProvider(aWorld.getSeed());
            }
        }

        try {
            int maxTerrainHeight = 65;
            ChunkData chunkData = super.createChunkData(world);
            chunkData.setRegion(0, 0, 0, 15, aWorld.getMaxHeight() - 1, 15, Material.AIR);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < aWorld.getMaxHeight(); y++) {
                        biomes.setBiome(x, y, z, Biome.PLAINS);
                        if (y <= maxTerrainHeight) {
                            Material decay = natureDecay.checkBlock(aWorld, (chunkX >> 4) + x, y, (chunkZ >> 4) + x);
                            if (decay != null) {
                                chunkData.setBlock(x, y, z, decay);
                            } else {
                                chunkData.setBlock(x, y, z, Material.STONE);
                            }
                        }
                    }
                }
            }
            return chunkData;
        } catch (NullPointerException e) {
            Minions.e(e);
            return super.generateChunkData(world,random,chunkX,chunkZ,biomes);
        }
    }

    private final static int SPAWN_RADIUS = 100;

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        int spawnX = random.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
        int spawnZ = random.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;

        // find the first non empty spot;
        int spawnY = world.getHighestBlockYAt(spawnX,spawnZ);
        // return the location
        return new Location(world, spawnX, spawnY, spawnZ);
    }

}
