package com.anton.beatmake.database;

public class SampleDbSchema {
    public static final class SampleTable {
        public static final String NAME = "samples";

        public static final class Cols {
            public static final String TITLE = "title";
            public static final String SEQUENCE = "sequence";
            public static final String TEMPO = "tempo";
            public static final String CHANNEL_VOLUMES = "channelVolumes";
        }
    }
}
