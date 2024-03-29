package ch.k42.metropolis.grid.urbanGrid.config;

import ch.k42.metropolis.grid.urbanGrid.enums.ContextType;
import ch.k42.metropolis.grid.urbanGrid.enums.Direction;
import ch.k42.metropolis.grid.urbanGrid.enums.LootType;
import ch.k42.metropolis.grid.urbanGrid.enums.RoadType;
import ch.k42.metropolis.grid.urbanGrid.enums.SchematicType;
import ch.k42.metropolis.minions.DecayOption;
import ch.k42.metropolis.minions.GridRandom;
import ch.k42.metropolis.minions.Nimmersatt;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created with IntelliJ IDEA.
 * User: Thomas
 * Date: 27.09.13
 * Time: 21:31
 * To change this template use File | Settings | File Templates.
 */
public class SchematicConfig extends AbstractSchematicConfig {

    private int groundLevelY = 0;
    private int oddsOfAppearanceInPercent = 100;
    private LootType[] lootCollections = {LootType.RESIDENTIAL};
    private int lootMinLevel = 1;
    private int lootMaxLevel = 5;
    private boolean rotate = true;
    private boolean roadFacing = true;

    private int chestOddsInPercent = 50;
    private int spawnerOddsInPercent = 50;
    private int decayIntensityInPercent = 100;

    private RoadCutout[] cutouts = {};
    private ContextType[] context = null; //{ContextType.HIGHRISE, ContextType.MIDRISE, ContextType.LOWRISE, ContextType.RESIDENTIAL, ContextType.INDUSTRIAL, ContextType.FARM, ContextType.PARK};
    private RoadType roadType = RoadType.NONE;
    private Set<Material> decayExceptionMaterials = new HashSet();
    private String[] schematics = {};
    private String path;
    private SchematicType schematicType = null;
    private String buildName = "";

    public SchematicConfig() {}
    
    public String getBuildName() {
    	return buildName;
    }

    public SchematicType getSchematicType() {
        return schematicType;
    }

    public int getGroundLevelY() {
        return groundLevelY;
    }

    public int getLootMaxLevel() {
        return lootMaxLevel;
    }

    public int getLootMinLevel() {
        return lootMinLevel;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * The odds of a schematic actually appearing if it can
     *
     * @return odds in percent, [0,100]
     */
    public int getOddsOfAppearance() {
        return oddsOfAppearanceInPercent;
    }

    public LootType[] getLootCollections() {
        return lootCollections;
    }

    public LootType getRandomLootCollection(GridRandom random) {
        return lootCollections[random.getRandomInt(lootCollections.length)];
    }

    /**
     * The odds of a chest actually appearing if it can
     *
     * @return odds in percent, [0,100]
     */
    public int getChestOdds() {
        return chestOddsInPercent;
    }

    /**
     * The odds of a spawner actually appearing if it can
     *
     * @return odds in percent, [0,100]
     */
    public int getSpawnerOdds() {
        return spawnerOddsInPercent;
    }

    /**
     * The custom decay options for this schematic
     *
     * @return DecayOption according to this schematic
     */
    public DecayOption getDecayOption() {

        double intensity = decayIntensityInPercent / 100.0;

        //sanitize
        if (intensity > 2) intensity = 1;
        else if (intensity < 0) intensity = 0;

        return new DecayOption(intensity, decayExceptionMaterials);
    }

    public List<ContextType> getContext() {
        return Arrays.asList(context);
    }

    public Direction getDirection() {
        if (rotate) {
            return Direction.NORTH;
        } else {
            return Direction.NONE;
        }
    }

    public void setGroundLevelY(int groundLevelY) {
        this.groundLevelY = groundLevelY;
    }

    public RoadType getRoadType() {
        return roadType;
    }

    public List<String> getSchematics() {
        return Arrays.asList(schematics);
    }

    public boolean getRoadFacing() {
        return roadFacing;
    }

    public RoadCutout[] getCutouts() {
        return cutouts;
    }

    public static SchematicConfig fromFile(File file){
        Gson gson = new Gson();
        try {
            String json = new String(Files.readAllBytes(file.toPath()));
            json = Nimmersatt.friss(json);
            SchematicConfig config = gson.fromJson(json, SchematicConfig.class);
            config.setPath(file.getPath());
            return config;
        } catch (IOException e) {
            Bukkit.getLogger().throwing(SchematicConfig.class.getName(),"Can't load SchematicConfig: " + file.getName(),e);
        } catch (JsonSyntaxException e){
            Bukkit.getLogger().throwing(SchematicConfig.class.getName(),"Can't parse SchematicConfig: " + file.getName(),e);
        }
        return null;

    }

}
