package ch.k42.metropolis.plugin;

import ch.k42.metropolis.commands.CommandMetropolisFreder;
import ch.k42.metropolis.commands.CommandMetropolisGrot;
import ch.k42.metropolis.commands.CommandMetropolisMaria;
import ch.k42.metropolis.generator.MetropolisGenerator;
import ch.k42.metropolis.generator.populators.PopulatorConfig;
import ch.k42.metropolis.grid.common.Factory;
import ch.k42.metropolis.grid.urbanGrid.clipboard.ClipboardBean;
import ch.k42.metropolis.grid.urbanGrid.clipboard.ClipboardProvider;
import ch.k42.metropolis.grid.urbanGrid.clipboard.ClipboardProviderDB;
import ch.k42.metropolis.grid.urbanGrid.context.ContextConfig;
import ch.k42.metropolis.minions.Nimmersatt;

import com.avaje.ebean.EbeanServer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;


/**
 * Main Class for the Metropolis plugin.
 *
 * @author Thomas Richner
 */
public class MetropolisPlugin extends JavaPlugin {

    private PopulatorConfig populatorConfig = new PopulatorConfig();
    private ContextConfig contextConfig = new ContextConfig();
    private ClipboardProvider clipboardProvider;
    private EbeanServer db;
    private MetropolisGenerator generator;

//    @Override
    public void installDDL() {
        try {
        	db = EBeanServerUtil.build(this);
        }catch(final Exception e) {
        	getLogger().throwing(this.getClass().getName(), "Failed to install EbeanServer: " + e.getMessage(), e);
        }
        
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return (generator = new MetropolisGenerator(this, worldName, clipboardProvider));
    }

    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> classes = new LinkedList<>();
        classes.add(ClipboardBean.class);
        return classes;
    }

    @Override
    public void onDisable() {
        super.onDisable();    //To change body of overridden methods use File | Settings | File Templates.
    }

    private void loadJsonConfigs(){
        File contextsConfig = new File(getDataFolder().getPath() + "/contexts.json");
        File populatorsConfig = new File(getDataFolder().getPath() + "/populators.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create(); //new GsonBuilder().setPrettyPrinting().create() ;
        try {

            if (!populatorsConfig.exists()) {
                String file = gson.toJson(populatorConfig);
                Files.write(populatorsConfig.toPath(), file.getBytes());
            } else {
                String json = new String(Files.readAllBytes(populatorsConfig.toPath()));
                json = Nimmersatt.friss(json);
                populatorConfig = gson.fromJson(json, PopulatorConfig.class);
            }

            if (!contextsConfig.exists()) {
                String file = gson.toJson(contextConfig);
                Files.write(contextsConfig.toPath(), file.getBytes());
            } else {
                String json = new String(Files.readAllBytes(contextsConfig.toPath()));
                json = Nimmersatt.friss(json);
                contextConfig = gson.fromJson(json, ContextConfig.class);
            }

        } catch (Exception e) { // catch all exceptions, inclusive any JSON fails
            getLogger().severe(e.getMessage());
        }
    }

    private void loadPluginConfig(){
        this.saveDefaultConfig(); // this saves the config provided in the jar if no config was found
        FileConfiguration configFile = getConfig();
        PluginConfig.loadFromFile(configFile);
    }

    private void loadClipboards(){
        //---- clips, load after config is ready
        try {
            clipboardProvider = Factory.getDefaultClipboardProvider();
            if(clipboardProvider == null) {
            	System.out.println();
            }
            clipboardProvider.loadClips(this);
        } catch (ClipboardProviderDB.PluginNotFoundException e) {
            getLogger().throwing(this.getClass().getName(), "Failed to load clipboard provider: " + e.getMessage(), e);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            getLogger().throwing(this.getClass().getName(), "Failed to load clipboards: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        loadJsonConfigs();
        loadPluginConfig();
        installDDL();
        loadClipboards();
        registerCommands();
    }

    private void registerCommands(){
        //---- add our command
        PluginCommand cmd = getCommand("metropolis");
        cmd.setExecutor(new CommandMetropolisMaria(this));
        cmd = getCommand("freder");
        cmd.setExecutor(new CommandMetropolisFreder(this));
        cmd = getCommand("grot");
        cmd.setExecutor(new CommandMetropolisGrot(this));
    }

    public PopulatorConfig getPopulatorConfig() { return populatorConfig; }

    public ContextConfig getContextConfig() { return contextConfig; }
    
    public EbeanServer getDatabase() { return db; }
    
    public MetropolisGenerator getGenerator(){ return generator; }

}
