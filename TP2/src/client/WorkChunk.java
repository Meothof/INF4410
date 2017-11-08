package client;

import java.util.ArrayList;

/**
 * Created by thmof on 17-11-07.
 */
public class WorkChunk {
    private ArrayList<String> chunk;

    public WorkChunk(ArrayList<String> chunk) {
        this.chunk = chunk;
    }

    public ArrayList<String> getChunk(){
        return this.chunk;
    }


}
