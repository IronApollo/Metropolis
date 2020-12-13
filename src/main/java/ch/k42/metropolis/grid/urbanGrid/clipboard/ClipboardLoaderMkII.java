package ch.k42.metropolis.grid.urbanGrid.clipboard;


import ch.k42.metropolis.grid.urbanGrid.config.GlobalSchematicConfig;
import ch.k42.metropolis.grid.urbanGrid.config.SchematicConfig;
import ch.k42.metropolis.grid.urbanGrid.enums.Direction;
import ch.k42.metropolis.minions.Cartesian2D;
import ch.k42.metropolis.minions.Minions;
import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.world.DataException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Thomas on 07.03.14.
 */
public class ClipboardLoaderMkII implements ClipboardLoader{

    private GlobalSchematicConfig globalConfig;
    private Map<String,Clipboard> clipstore ;
    //@Inject
    private ClipboardDAO dao;
    private boolean zero;

    public ClipboardLoaderMkII(ClipboardDAO dao) {
        this.dao = dao;
    }

    private SchematicConfig getConfig(File folder){
        SchematicConfig config = SchematicConfig.fromFile(new File(folder,ClipboardConstants.CONFIG_FILE));
        if(config==null){
            Minions.w("Found no config in folder: " + folder.getName());
        }
        return config;
    }

    public Map<String,Clipboard> loadSchematics(boolean wasImportZero){
    	this.zero = wasImportZero;
        clipstore = new HashMap<>();
        Minions.i("loading schematic config files...");
        globalConfig = GlobalSchematicConfig.fromFile(ClipboardConstants.IMPORT_FOLDER + File.separator +
                ClipboardConstants.GLOBAL_SETTINGS);

        File cacheFolder = new File(ClipboardConstants.CACHE_FOLDER);

        File[] schematicFolders = cacheFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        });

        int length = schematicFolders.length;
        Minions.d("ClipboardLoaderMkII schematic folder count: " + length);
        for(int i=0;i<length;i++){
            File folder = schematicFolders[i];

            Minions.i("Loading schematic %4d of %d (%.2f%%) : %s" ,i,length,(i/(double) length)*100,folder.getName());
            try {
                loadFolder(folder);
            } catch (IOException | DataException e) {
                // move on to next schem
                Minions.e(e);
            }
        }

        return clipstore;
    }

    private List<Clipboard> loadFolder(File folder) throws IOException, DataException {
        List<Clipboard> clips = new ArrayList<>();
        clips.addAll(loadStreet(folder));
        clips.addAll(loadBuilds(folder));
        return clips;
    }

    private List<Clipboard> loadStreet(File folder) throws IOException, DataException {
        File streetFile =    new File(folder, ClipboardConstants.STREET_FILE);
        if(!streetFile.exists()){
            return Collections.emptyList();
        }
//        SchematicFormat format = SchematicFormat.getFormat(streetFile);
////        Schematic schem = ClipboardFormat.findByFile(streetFile).load(streetFile);
//        format.save(cuboid, streetFile);
        
        ClipboardFormat format = ClipboardFormats.findByFile(streetFile);
        com.sk89q.worldedit.extent.clipboard.Clipboard clipboard;
        try (ClipboardReader reader = format.getReader(new FileInputStream(streetFile))){
            clipboard = reader.read();
        }
        if(clipboard == null){
            Minions.w("Clipboard is null for street " + streetFile.getName() + "!");
            return ImmutableList.of();
        }
        
//        Schematic s = ClipboardFormat.SCHEMATIC.load(streetFile);
        
//        s.save(streetFile, ClipboardFormat.SCHEMATIC);
//        BlockArrayClipboard cuboid = (BlockArrayClipboard) s.getClipboard();
        
        SchematicConfig config = getConfig(folder);
        String hash = folder.getName();
        Clipboard clip = new FileClipboard(clipboard, config, globalConfig, hash);
        hash += ".STREET";
        clipstore.put( hash,clip);
        if(!this.zero) {
	        if(dao.containsHash(hash)){// delete old entries
	            dao.deleteClipboardHashes(hash);
	        }
        }
        dao.storeClipboard(hash, streetFile.getName(), Direction.NONE, config, new Cartesian2D(1, 1));
        return Collections.singletonList(clip);
    }

    private List<Clipboard> loadBuilds(File folder) throws DataException{
        File northFile =    new File(folder, ClipboardConstants.NORTH_FILE);
        if(!northFile.exists()){
            return Collections.emptyList();
        }
        File eastFile =     new File(folder, ClipboardConstants.EAST_FILE);
        File southFile =    new File(folder, ClipboardConstants.SOUTH_FILE);
        File westFile =     new File(folder, ClipboardConstants.WEST_FILE);

        String hash = folder.getName();
        SchematicConfig config = getConfig(folder);

        List<Clipboard> clips = new ArrayList<>();

        clips.add(loadFromCache(eastFile,hash,Direction.EAST,config));
        clips.add(loadFromCache(westFile,hash,Direction.WEST,config));
        clips.add(loadFromCache(southFile,hash,Direction.SOUTH,config));
        clips.add(loadFromCache(northFile,hash,Direction.NORTH,config));
        return clips;
    }

    private Clipboard loadFromCache(File file,String hash,Direction direction,SchematicConfig config) throws DataException{
        try {
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            com.sk89q.worldedit.extent.clipboard.Clipboard clipboard;
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))){
                clipboard = reader.read();
            }
            if(clipboard == null){
                Minions.w("Clipboard from cache " + file.getName() + "is null!");
                return null;
            }
//        	Minions.i("Start for " + hash.substring(0, 5) + " DIRECTION " + direction.name());
////            Schematic schem = ClipboardFormat.findByFile(file).load(file);
//        	Minions.i("Schematic loaded.");
//            Schematic s = ClipboardFormat.SCHEMATIC.load(file);
//            s.save(file, ClipboardFormat.SCHEMATIC);
//            BlockArrayClipboard cuboid = (BlockArrayClipboard) s.getClipboard();

            Clipboard clip = new FileClipboard(clipboard,config,globalConfig, hash);
//            Minions.i("Clipboard created.");

            String thash = hash + "." + direction.name();
            clipstore.put(thash,clip);
            if(!this.zero) {
	            if(dao.containsHash(thash)){ // check if already in, if yes, delete the old one
	               dao.deleteClipboardHashes(thash);
	            }
	            //DAO, store in db
	            Cartesian2D size = new Cartesian2D(clipboard.getRegion().getWidth()>>4,clipboard.getRegion().getLength()>>4);
	            dao.storeClipboard(thash,file.getName(), direction,config,size);
	
	            if(!config.getRoadFacing()){ // if it doesn't need roads, store it for 'non-road' usage too
	                dao.storeClipboard(thash,file.getName(), Direction.NONE,config,size);
	            }
            }
            return clip;
        } catch (IOException e) {
            Minions.e(e);
        }
        return null;
    }
}
