package net.rmelick.hanabi.bot.live.connector.schemas.java;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TableSpectate {
    private long tableID;

    @JsonProperty("tableID")
    public long getTableID() { return tableID; }
    @JsonProperty("tableID")
    public void setTableID(long value) { this.tableID = value; }
}