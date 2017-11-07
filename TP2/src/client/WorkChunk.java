package client;

/**
 * Created by thmof on 17-11-07.
 */
public class WorkChunk {
    private int start;
    private int end;

    public WorkChunk(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return "WorkChunk{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
