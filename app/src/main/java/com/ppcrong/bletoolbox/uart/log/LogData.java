package com.ppcrong.bletoolbox.uart.log;

/**
 * LogData.
 */

public class LogData {

    private int level;
    private long time;
    private String data;

    public LogData(int level, long time, String data) {
        this.level = level;
        this.time = time;
        this.data = data;
    }

    @Override
    public String toString() {
        return data;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public static class Builder {

        private int level;
        private long time;
        private String data;

        public Builder setLevel(int level) {
            this.level = level;
            return this;
        }

        public Builder setTime(long time) {
            this.time = time;
            return this;
        }

        public Builder setData(String data) {
            this.data = data;
            return this;
        }

        public LogData build() {
            return new LogData(level, time, data);
        }
    }
}
