package client;

import server.Server;

import java.util.ArrayList;

public class ProcessedChunk {
    private int result;
    private int serverId;
    private int start;
    private int end;
    private ArrayList<String> operations;

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

    public ArrayList<String> getOperations() {
        return operations;
    }

    public ProcessedChunk(int result, int serverId, int start, int end, ArrayList<String> o) {
        this.result = result;
        this.serverId = serverId;
        this.start = start;
        this.end = end;
        this.operations = new ArrayList<String>(o.subList(start, end));
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
