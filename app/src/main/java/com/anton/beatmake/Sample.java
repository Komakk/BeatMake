package com.anton.beatmake;

import com.google.gson.Gson;

import java.io.Serializable;

public class Sample implements Serializable{

    private String title;
    private float[][] sequence;
    private int tempo;
    private float[] channelVolumes;

    public Sample(String title, float[][] sequence, int tempo, float[] channelVolumes){
        this.title = title;
        this.sequence = sequence;
        this.tempo = tempo;
        this.channelVolumes = channelVolumes;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public float[][] getSequence() {
        return sequence;
    }

    public void setSequence(float[][] sequence) {
        this.sequence = sequence;
    }

    public int getTempo() {
        return tempo;
    }

    public void setTempo(int tempo) {
        this.tempo = tempo;
    }

    public float[] getChannelVolumes() {
        return channelVolumes;
    }

    public void setChannelVolumes(float[] channelVolumes) {
        this.channelVolumes = channelVolumes;
    }

    public String getJsonSequence() {
        return new Gson().toJson(sequence);
    }

    public String getJsonChannelVolumes() {
        return new Gson().toJson(channelVolumes);
    }

    public static float[][] jsonToSequence (String json) {
        return new Gson().fromJson(json, float[][].class);
    }

    public static float[] jsonToChannelVolumes (String json) {
        return new Gson().fromJson(json, float[].class);
    }
}
