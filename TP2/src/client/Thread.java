package client;

import server.Server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class Thread implements Callable {
    private int start;
    private int end;
    private ArrayList<String> operations;
    private ServerObj server;

    public Thread(int start, int end, ArrayList<String> operations, ServerObj server) {
        this.start = start;
        this.end = end;
        this.operations = operations;
        this.server = server;
    }

    public ProcessedChunk call(){
        return new ProcessedChunk(this.server.processTask( new ArrayList<String>(operations.subList(start, end))), server.getId(), start, end);
    }

}
