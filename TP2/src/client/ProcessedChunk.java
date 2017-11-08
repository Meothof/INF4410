package client;

import server.Server;

public class ProcessedChunk {
    private int result;
    private int serverId;
    private int start;
    private int end;

    public int getResult() {
        return result;
    }

    public int getServerId() {
        return serverId;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public ProcessedChunk(int result, int serverId, int start, int end) {
        this.result = result;
        this.serverId = serverId;
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return "ProcessedChunk{" +
                "result=" + result +
                ", serverId=" + serverId +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
