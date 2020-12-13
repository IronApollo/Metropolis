package ch.k42.metropolis.grid.urbanGrid.clipboard;


import ch.k42.metropolis.generator.MetropolisGenerator;
import ch.k42.metropolis.grid.urbanGrid.config.GlobalSchematicConfig;
import ch.k42.metropolis.grid.urbanGrid.config.SchematicConfig;
import ch.k42.metropolis.grid.urbanGrid.provider.EnvironmentProvider;
import ch.k42.metropolis.minions.Cartesian2D;
import ch.k42.metropolis.minions.Cartesian3D;
import ch.k42.metropolis.minions.DirtyHacks;
import ch.k42.metropolis.minions.GridRandom;
import ch.k42.metropolis.minions.Minions;
import ch.k42.metropolis.plugin.PluginConfig;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Thomas on 07.03.14.
 */
public class FileClipboard implements Clipboard{
    private SchematicConfig config;
    private GlobalSchematicConfig globalConfig;
    private List<Cartesian3D> chests = new ArrayList<>();
    private List<Cartesian3D> spawners = new ArrayList<>();
    private int blockCount;
    private String groupId;
    private com.sk89q.worldedit.extent.clipboard.Clipboard clipboard;
//    private Schematic schem;

    public FileClipboard(com.sk89q.worldedit.extent.clipboard.Clipboard clipboard, SchematicConfig config, GlobalSchematicConfig globalConfig, String hash) {
        this.config = config;
        this.globalConfig = globalConfig;
        this.groupId = hash;
        this.clipboard = clipboard;
//        this.schem = s;

        this.blockCount = (int)clipboard.getRegion().getVolume();

        if(config.getGroundLevelY()==0){
            config.setGroundLevelY(estimateStreetLevel());
        }
    }

//    public com.sk89q.worldedit.extent.clipboard.Clipboard loadCuboid(){
//        SchematicFormat format = SchematicFormat.getFormat(file);
//        try {
//            return format.load(file);
//        	return (BlockArrayClipboard) ClipboardFormat.SCHEMATIC.load(file).getClipboard();
//        	return schem.getClipboard();
//        } catch (IOException e) {
//            Minions.e(e);
//            throw new RuntimeException("Failed to reload clipboard, possible corrupted cache.",e);
//        }
//    }

    @Override
    public void paste(MetropolisGenerator generator, Cartesian2D base, int streetLevel) {
        int blockY = getBottom(streetLevel);
        BlockVector3 at = BlockVector3.at(base.X << 4, blockY , base.Y << 4);
        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(BukkitAdapter.adapt(generator.getWorld())).build()){
            editSession.setSideEffectApplier(SideEffectSet.none());
//            schem.paste(editSession.getWorld(), at, false, true, null);
            //place Schematic
            place(generator, editSession, at);

            //fill chests
            World world = generator.getWorld();
            GridRandom rand = generator.getGridProvider().getGrid(base.X,base.Y).getRandom();
            Cartesian3D base3 = new Cartesian3D(base.X<< 4, blockY,  base.Y<< 4);

            if (PluginConfig.isChestRenaming()) { //do we really want to name them all?
                for (Cartesian3D c : chests) {
                    Cartesian3D temp = base3.add(c);
                    Block block = world.getBlockAt(temp.X, temp.Y, temp.Z);

                    if (block.getType() == Material.CHEST) {
                        if (!rand.getChance(config.getChestOdds())) { //we were unlucky, chest doesn't get placed{
                            block.setType(Material.AIR);
                        } else { //rename chest


                            try {
                                Chest chest = (Chest) block.getState(); //block has to be a chest
                                String name = DirtyHacks.getChestName(chest);
                                name = validateChestName(rand, name);
                                nameChest(chest, name);
                                chest.update();
                            }catch (NullPointerException e){
                                Minions.d("NPE while naming chest.");
                            }
                        }
                    } else {
                        Minions.d("Chest coordinates were wrong! (" + block + ")");
                    }

                }
            }

            //set spawners

            if (PluginConfig.isSpawnerPlacing()) { // do we even place any?
                for (Cartesian3D c : spawners) {
                    Cartesian3D temp = base3.add(c);
                    Block block = world.getBlockAt(temp.X, temp.Y, temp.Z);

                    if (block.getType() == Material.SPONGE) {
                        if (!rand.getChance(config.getSpawnerOdds())) { //we were unlucky, chest doesn't get placed{
                            block.setType(Material.AIR);
                        } else { //set spawn type
                            block.setType(Material.SPAWNER);
                            if (block.getState() instanceof CreatureSpawner) {
                                CreatureSpawner spawner = (CreatureSpawner) block.getState(); //block has to be a chest
                                spawner.setSpawnedType(getSpawnedEntity(rand));
                                spawner.update();
                                //generator.reportDebug("Placed a spawner!");
                            } else {
                                //generator.reportDebug("Unable to place Spawner.");
                            }
                        }
                    } else {
                        Minions.w("Spawner coordinates were wrong!");
                    }
                }
            }
        } catch (Exception e) { //FIXME don't catch generic Exception!!!!
            Minions.w("Placing schematic failed. WorldEdit fucked up.");
//            Minions.e(e);
            e.printStackTrace();
        }
    }

    @Override
    public Cartesian3D getSize() {
        return new Cartesian3D(clipboard.getRegion().getWidth(), clipboard.getRegion().getHeight(), clipboard.getRegion().getLength());
    }


    private void place(MetropolisGenerator generator, EditSession editSession, BlockVector3 pos) throws Exception {
//    	com.sk89q.worldedit.extent.clipboard.Clipboard cuboid = loadCuboid();
        EnvironmentProvider natureDecay = generator.getNatureDecayProvider();
        chests.clear();
        spawners.clear();
//        int cx = (int)(cuboid.getMaximumPoint().getX() - cuboid.getMinimumPoint().getX()) + 1;
//        int cy = (int)(cuboid.getMaximumPoint().getY() - cuboid.getMinimumPoint().getY()) + 1;
//        int cz = (int)(cuboid.getMaximumPoint().getZ() - cuboid.getMinimumPoint().getZ()) + 1;
        
//        System.out.println("CUBOID AREA SIZE: " + cuboid.getRegion().getArea() + ";; CUBOID SIZE: " + cx*cy*cz);


        for (int x = 0; x < clipboard.getRegion().getWidth(); x++) {
            for (int y = 0; y < clipboard.getRegion().getHeight(); y++) {
                for (int z = 0; z < clipboard.getRegion().getLength(); z++) {

                    BlockState block = clipboard.getBlock(BlockVector3.at(x, y, z));
                    BlockVector3 vec = BlockVector3.at(x, y, z).add(pos);
                    Material decay = natureDecay.checkBlock(generator.getWorld(), vec.getX(), vec.getY(), vec.getZ());
                    
                    if (decay != null) {
                        editSession.setBlock(vec, BukkitAdapter.adapt(decay.createBlockData()));
//                        cuboid.setBlock(new Vector(x, y, z), block);
                        continue;
                    }

                    if (block.getBlockType() == BlockTypes.CHEST) {
                        chests.add(new Cartesian3D(x, y, z));
                    } else if (block.getBlockType() == BlockTypes.SPONGE) {
                        spawners.add(new Cartesian3D(x, y, z));
                    }
                    editSession.setBlock(vec, block);
//                    try {
//                    cuboid.setBlock(x, y, z, block);
//                    }catch(Exception e) {
//                	System.out.println("BLOCK LOC: x: " + x + "; y: " + y + "; z: " + z + ";; CUBOID AREA SIZE: " + cuboid.getRegion().getArea() + ";; CUBOID SIZE: " + cuboid.getMaximumPoint().subtract(cuboid.getMinimumPoint()) + " (" + (cuboid.getMaximumPoint().getX() - cuboid.getMinimumPoint().getX()) + " * " + (cuboid.getMaximumPoint().getY() - cuboid.getMinimumPoint().getY()) + " * " + (cuboid.getMaximumPoint().getZ() - cuboid.getMinimumPoint().getZ()) + ")");
//                    }
                }
            }
        }
//        cuboid.paste(editSession, pos, false);
//        Schematic s = new Schematic(cuboid);
//        s.paste(editSession.getWorld(), pos);
//        editSession.flushQueue();
    }

    public int getBottom(int streetLevel) {
        return streetLevel - config.getGroundLevelY();
    }

    @Override
    public SchematicConfig getConfig() {
        return config;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    private EntityType getSpawnedEntity(GridRandom random) {
        if (config.getSpawners() == null) {
            if (globalConfig.getSpawners() == null) {
                return EntityType.ZOMBIE; // All settings failed
            } else {
                config.setSpawners(globalConfig.getSpawners());
            }
        }
        return config.getRandomSpawnerEntity(random);
    }

    private void nameChest(Chest chest, String name) { //there might be no better way...
        DirtyHacks.setChestName(chest, name);
    }

    private static final char COLOR = ChatColor.GREEN.getChar();

    private String validateChestName(GridRandom rand, String name) {

        //chest has level? -> Assumption: Chest fully named

        char lastchar = name.charAt(name.length() - 1);
        boolean fail;
        try {

            int level = Integer.parseInt(String.valueOf(lastchar));
            fail = level > 5 || level < 1;
        } catch (NumberFormatException e) {
            fail = true;
        }

        if (fail) {
            if (lastchar == '_') { //append only level
                name += Integer.toString(randomChestLevel(rand)); //add a random chest level
            } else { // set name and level
                name = getNameAndLevel(rand);
            }
        }
        return name;
    }

    private String getNameAndLevel(GridRandom rand) {
        StringBuffer buf = new StringBuffer();
        if (config.getLootCollections().length > 0) {
            buf.append('\u00a7')
                    .append(COLOR)
                    .append(config.getRandomLootCollection(rand).name)
                    .append('_')
                    .append(Integer.toString(randomChestLevel(rand)));
            return buf.toString();
        } else {
            return "";
        }
    }

    private int randomChestLevel(GridRandom random) {
        int min = config.getLootMinLevel();
        int max = config.getLootMaxLevel();
        return globalConfig.getRandomChestLevel(random, min, max);
    }

    /**
     * estimates the street level of a schematic, useful for bootstrapping settings
     *
     * @return
     */
    private int estimateStreetLevel() {
//    	com.sk89q.worldedit.extent.clipboard.Clipboard cuboid = loadCuboid();
        if (clipboard.getRegion().getHeight() - 2 < 0) return 1;

        for (int y = clipboard.getRegion().getHeight() - 2; y >= 0; y--) {
//            int b = cuboid.getPoint(new Vector(0, y, 0)).getType();
            Material b = clipboard.getBlock(BlockVector3.at(0, y, 0)) == null ? Material.AIR : BukkitAdapter.adapt(clipboard.getBlock(BlockVector3.at(0, y, 0)).getBlockType());
            if (b != Material.AIR && b != Material.TALL_GRASS && b != Material.DANDELION)
                return y + 1;
        }
        return 1;
    }

    @Override
    public String toString() {
        return "Clipboard: " + config.getPath();
    }
}
