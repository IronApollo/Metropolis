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

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.object.visitor.FastIterator;
import com.boydti.fawe.util.EditSessionBuilder;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;

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
public class ClipboardWE implements Clipboard{
    private com.sk89q.worldedit.extent.clipboard.Clipboard cuboid;
    private Schematic schem;
//    private Schematic schem;
    private SchematicConfig config;
    private GlobalSchematicConfig globalConfig;
    private List<Cartesian3D> chests = new ArrayList<>();
    private List<Cartesian3D> spawners = new ArrayList<>();
    private int blockCount;
    private String groupId;

    public ClipboardWE(Schematic s, SchematicConfig config, GlobalSchematicConfig globalConfig,String groupId) {
	    this.schem = s;
    	this.cuboid = s.getClipboard();
        this.config = config;
        this.globalConfig = globalConfig;
        this.blockCount = s.getClipboard().getRegion().getArea();
        this.groupId = groupId;
        if(config.getGroundLevelY()==0){
            config.setGroundLevelY(estimateStreetLevel());
        }
    }

    @Override
    public void paste(MetropolisGenerator generator, Cartesian2D base, int streetLevel) {
        int blockY = getBottom(streetLevel);
        Vector at = new Vector(base.X << 4, blockY , base.Y << 4);
        Minions.d("Schematic " + this.schem.toString() + " ; " + this.getConfig().getBuildName() + " pasted at " + at.toString());
        try {
            EditSession editSession = new EditSessionBuilder(FaweAPI.getWorld(generator.getWorld().getName())).changeSetNull().autoQueue(false).fastmode(true).checkMemory(false).build();
            //place Schematic
            GridRandom rand = generator.getGridProvider().getGrid(base.X,base.Y).getRandom();
            placeNew(generator, rand, editSession, at);
            editSession.flushQueue();
            editSession.setBlock(at, new BaseBlock(Material.RED_GLAZED_TERRACOTTA.getId()));
            editSession.setBlock(at.add(15, 0, 0), new BaseBlock(Material.RED_GLAZED_TERRACOTTA.getId()));
            editSession.setBlock(at.add(15, 0, 15), new BaseBlock(Material.RED_GLAZED_TERRACOTTA.getId()));
            editSession.setBlock(at.add(0, 0, 15), new BaseBlock(Material.RED_GLAZED_TERRACOTTA.getId()));
            editSession.flushQueue();

            //            schem.paste(editSession.getWorld(), at, false, true, null);
//            editSession.flushQueue();
            //fill chests
            World world = generator.getWorld();
            Cartesian3D base3 = new Cartesian3D(base.X<< 4, blockY,  base.Y<< 4);

//            if (PluginConfig.isChestRenaming()) { //do we really want to name them all?
//                for (Cartesian3D c : chests) {
//                    Cartesian3D temp = base3.add(c);
//                    Block block = world.getBlockAt(temp.X, temp.Y, temp.Z);
//                    if (block.getType() == Material.CHEST) {
//                        if (!rand.getChance(config.getChestOdds())) { //we were unlucky, chest doesn't get placed{
//                            block.setType(Material.AIR);
//                        } else { //rename chest
//
//
//                            try {
//                                Chest chest = (Chest) block.getState(); //block has to be a chest
//                                String name = DirtyHacks.getChestName(chest);
//                                name = validateChestName(rand, name);
//                                nameChest(chest, name);
//                                chest.update();
//                            }catch (NullPointerException e){
//                                Minions.d("NPE while naming chest.");
//                            }
//                        }
//                    } else {
//                        Minions.d("Chest coordinates were wrong! (" + block + ")");
//                    }
//                }
//            }
//
//            //set spawners
//
//            if (PluginConfig.isSpawnerPlacing()) { // do we even place any?
//                for (Cartesian3D c : spawners) {
//                    Cartesian3D temp = base3.add(c);
//                    Block block = world.getBlockAt(temp.X, temp.Y, temp.Z);
//
//                    if (block.getType() == Material.SPONGE) {
//                        if (!rand.getChance(config.getSpawnerOdds())) { //we were unlucky, chest doesn't get placed{
//                            block.setType(Material.AIR);
//                        } else { //set spawn type
//                            block.setType(Material.MOB_SPAWNER);
//                            if (block.getState() instanceof CreatureSpawner) {
//                                CreatureSpawner spawner = (CreatureSpawner) block.getState(); //block has to be a chest
//                                spawner.setSpawnedType(getSpawnedEntity(rand));
//                                spawner.update();
//                                //generator.reportDebug("Placed a spawner!");
//                            } else {
//                                //generator.reportDebug("Unable to place Spawner.");
//                            }
//                        }
//                    } else {
//                        Minions.w("Spawner coordinates were wrong!");
//                    }
//                }
//            }
        } catch (Exception e) { //FIXME don't catch generic Exception!!!!
            Minions.w("Placing schematic failed. WorldEdit fucked up.");
//            Minions.e(e);
            e.printStackTrace();
        }
    }

    @Override
    public Cartesian3D getSize() {
        return new Cartesian3D(cuboid.getRegion().getWidth(),cuboid.getRegion().getHeight(),cuboid.getRegion().getLength());
    }

    private void placeNew(MetropolisGenerator generator, GridRandom rand, EditSession editSession, Vector at) {
    	for(Vector v : new FastIterator(this.cuboid.getRegion(), editSession)) {
    		BaseBlock block = this.cuboid.getBlock(v);
    		Vector vec = v.add(at).subtract(0, 64, 0);
    		try {
				editSession.setBlock(vec, block);
			} catch (MaxChangedBlocksException e) {
			}
    	}
//    	this.schem.getClipboard().setOrigin(new Vector(0,at.getBlockY(),0));
//    	return this.schem.paste(FaweAPI.getWorld(generator.getWorld().getName()), at);
    }
    
    private void place(MetropolisGenerator generator, EditSession editSession, Vector pos) throws Exception {
//        EnvironmentProvider natureDecay = generator.getNatureDecayProvider();
//        BlockArrayClipboard cb = new BlockArrayClipboard(cuboid.);
        chests.clear();
        spawners.clear();
//        int cx = (int)(cuboid.get.getMaximumPoint().getX() - schem.getClipboard().getMinimumPoint().getX()) + 1;
//        int cy = (int)(schem.getClipboard().getMaximumPoint().getY() - schem.getClipboard().getMinimumPoint().getY()) + 1;
//        int cz = (int)(schem.getClipboard().getMaximumPoint().getZ() - schem.getClipboard().getMinimumPoint().getZ()) + 1;
//        
//        System.out.println("CUBOID AREA SIZE: " + cuboid.getArea() + ";; CUBOID SIZE: " + cx*cy*cz);

        for (int x = 0; x < getSize().X; x++) {
            for (int y = 0; y < getSize().Y; y++) {
                for (int z = 0; z < getSize().Z; z++) {

                    BaseBlock block = cuboid.getBlock(new Vector(x, y, z));
                    Vector vec = new Vector(x, y, z).add(pos);
                    //always will be null
//                    Material decay = natureDecay.checkBlock(generator.getWorld(), (int) vec.getX(), (int) vec.getY(), (int) vec.getZ());
//
//                    if (decay != null) {
//                        block.setType(decay.getId());
//                        editSession.setBlock(vec, block);
////                        cuboid.setBlock(new Vector(x, y, z), block);
//                        continue;
//                    }

                    if (block.getId() == Material.CHEST.getId()) {
                        chests.add(new Cartesian3D(x, y, z));
                    } else if (block.getId() == Material.SPONGE.getId()) {
                        spawners.add(new Cartesian3D(x, y, z));
                    }
                    editSession.setBlock(vec, block);
//                    try {
//                    cuboid.setBlock(new Vector(x, y, z), block);
//                    }catch(Exception e) {
//                    	System.out.println("BLOCK LOC: x: " + x + "; y: " + y + "; z: " + z + ";; CUBOID AREA SIZE: " + cuboid.getRegion().getArea() + ";; CUBOID SIZE: " + cuboid.getMaximumPoint().subtract(cuboid.getMinimumPoint()) + " (" + (cuboid.getMaximumPoint().getX() - cuboid.getMinimumPoint().getX()) + " * " + (cuboid.getMaximumPoint().getY() - cuboid.getMinimumPoint().getY()) + " * " + (cuboid.getMaximumPoint().getZ() - cuboid.getMinimumPoint().getZ()) + ")");
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

//    private EditSession getEditSession(MetropolisGenerator generator) {
//        return new EditSession(new BukkitWorld(generator.getWorld()), blockCount);
//    }

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
            return "LENGTH!>0";
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
        if (getSize().Y - 2 < 0) return 1;

        for (int y = getSize().Y - 2; y >= 0; y--) {
//            int b = cuboid.getBlock(new Vector(0, y, 0)).getType();
            Material b = cuboid.getBlock(new Vector(0, y, 0)) == null ? Material.AIR : Material.getMaterial(cuboid.getBlock(new Vector(0, y, 0)).getType());
            if (b != Material.AIR && b != Material.LONG_GRASS && b != Material.YELLOW_FLOWER)
                return y + 1;
        }
        return 1;
    }

    @Override
    public String toString() {
        return "Clipboard: " + config.getBuildName() + "; Schematics: " + config.getSchematics();
    }
}
