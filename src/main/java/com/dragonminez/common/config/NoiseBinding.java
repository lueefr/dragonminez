package com.dragonminez.common.config;

/** Borrow selected noise channels while retaining the target dimension's blocks and surface rules. */
public final class NoiseBinding {
    private String source="";
    private boolean terrain=true,climate=true;
    public NoiseBinding(){}
    public NoiseBinding(String source,boolean terrain,boolean climate){this.source=source;this.terrain=terrain;this.climate=climate;}
    public String source(){return source;}
    public boolean terrain(){return terrain;}
    public boolean climate(){return climate;}
    public void validate(){com.dragonminez.api.content.ContentProgress.requireId(source);if(!terrain&&!climate)throw new IllegalArgumentException("Noise binding needs terrain or climate");}
}
