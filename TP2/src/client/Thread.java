package client;

import java.util.Arrays;
import java.util.concurrent.Callable;

public class Thread implements Callable {
    private int start;
    private int end;
    private String[] operations;
    private ServerObj server;

    public Thread(int start, int end, String[] operations, ServerObj server) {
        this.start = start;
        this.end = end;
        this.operations = operations;
        this.server = server;
    }

    public ProcessedChunk call(){
        return new ProcessedChunk(this.server.processTask(Arrays.copyOfRange(operations, start, end )), server.getId(), start, end);
    }

}
