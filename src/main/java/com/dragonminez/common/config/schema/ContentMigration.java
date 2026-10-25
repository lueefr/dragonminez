package com.dragonminez.common.config.schema;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.function.Consumer;

/** Validate first, copy the exact original bytes, then atomically replace. Never overwrite a backup. */
public final class ContentMigration {
    private record Pending(String original,ContentSchema.Kind kind,JsonObject flat) {}
    private static final java.util.Map<Path,Pending> PENDING=new java.util.LinkedHashMap<>();
    public static synchronized void begin(){PENDING.clear();}
    private static Path key(Path file){return file.toAbsolutePath().normalize();}
    public static synchronized void discard(Path file){PENDING.remove(key(file));}
    public static synchronized void commitPending()throws IOException{
        for(var entry:PENDING.entrySet()){
            if(!Files.readString(entry.getKey(),StandardCharsets.UTF_8).equals(entry.getValue().original()))
                throw new IOException("Content changed during migration; refusing to overwrite: "+entry.getKey());
        }
        var entries=PENDING.entrySet().iterator();
        while(entries.hasNext()){var entry=entries.next();commit(entry.getKey(),entry.getValue());entries.remove();}
    }
    private static final Gson PRETTY=new GsonBuilder().setPrettyPrinting().create();
    public static synchronized JsonObject load(Path file,ContentSchema.Kind kind,Consumer<JsonObject> validator)throws IOException{
        String original=Files.readString(file,StandardCharsets.UTF_8);
        JsonObject source=JsonParser.parseString(original).getAsJsonObject();
        Pending queued=PENDING.get(key(file));
        if(queued!=null&&!original.equals(queued.original())){PENDING.remove(key(file));queued=null;}
        JsonObject flat=queued==null?ContentSchema.decode(kind,source):queued.flat().deepCopy();
        validator.accept(flat);
        if(source.has("schema")&&source.equals(ContentSchema.encode(kind,flat)))return flat;
        PENDING.put(key(file),new Pending(original,kind,flat.deepCopy()));
        return flat;
    }
    /** Default backfilling joins the migration transaction instead of overwriting its source. */
    public static synchronized void update(Path file,ContentSchema.Kind kind,JsonObject updated)throws IOException{
        Pending pending=PENDING.get(key(file));
        String current=Files.readString(file,StandardCharsets.UTF_8);
        if(pending!=null&&!pending.original().equals(current))pending=null;
        String original=pending==null?current:pending.original();
        JsonObject flat=pending==null?ContentSchema.decode(kind,JsonParser.parseString(original).getAsJsonObject()):pending.flat().deepCopy();
        merge(flat,updated);
        PENDING.put(key(file),new Pending(original,kind,flat));
    }
    private static void merge(JsonObject target,JsonObject updated){
        updated.entrySet().forEach(entry->{
            if(target.has(entry.getKey())&&target.get(entry.getKey()).isJsonObject()&&entry.getValue().isJsonObject())
                merge(target.getAsJsonObject(entry.getKey()),entry.getValue().getAsJsonObject());
            else target.add(entry.getKey(),entry.getValue().deepCopy());
        });
    }
    private static void commit(Path file,Pending pending)throws IOException{
        if(!Files.readString(file,StandardCharsets.UTF_8).equals(pending.original()))throw new IOException("Content changed before migration: "+file);
        JsonObject nested=ContentSchema.encode(pending.kind(),pending.flat());
        Path backupDir=file.toAbsolutePath().getParent().resolve("oldBackup");
        Files.createDirectories(backupDir);
        Path backup=backupDir.resolve(file.getFileName()+".legacy.bak");
        for(int i=1;Files.exists(backup);i++)backup=backupDir.resolve(file.getFileName()+".legacy."+i+".bak");
        Files.copy(file,backup);
        Path temporary=Files.createTempFile(file.toAbsolutePath().getParent(),"content-",".tmp");
        try{
            Files.writeString(temporary,PRETTY.toJson(nested)+"\n",StandardCharsets.UTF_8);
            if(!Files.readString(file,StandardCharsets.UTF_8).equals(pending.original()))throw new IOException("Content changed during migration: "+file);
            try{Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ex){throw new IOException("Atomic config replacement unavailable; original preserved, backup at "+backup,ex);}
        }finally{Files.deleteIfExists(temporary);}
        System.getLogger("DragonMineZ").log(System.Logger.Level.INFO,"Converted content {0}; original backup: {1}",file,backup);
    }
    private ContentMigration(){}
}
