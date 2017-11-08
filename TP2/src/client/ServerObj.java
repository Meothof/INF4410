package client;

import shared.ServerInterface;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Random;

public class ServerObj {
    private int id;
    private String ip;
    private ServerInterface stub;
    private int q;
    private float m;
    private boolean isWorking;

    public ServerObj(int id, String ip) {
        this.id = id;
        this.ip = ip;
        this.m = 0;
        this.stub = loadServerStub(getIp());
        this.isWorking = true;

    }

    public ServerInterface loadServerStub(String hostname) {
        ServerInterface stub = null;
        try {
            Registry registry = LocateRegistry.getRegistry(hostname, 5031);
            stub = (ServerInterface) registry.lookup("server");
            setQ(stub.getQ());
            setM(stub.getM());
        } catch (NotBoundException e) {
            System.out.println("Erreur: Le nom '" + e.getMessage() + "' n'est pas défini dans le registre.");
        } catch (AccessException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
        return stub;
    }

    private boolean refuseTask(int taskLength) {
        float t = ((float)taskLength - this.getQ()) / (5*this.getQ());
        Random rand = new Random();
        float r = rand.nextFloat();
        if (r > t) {
            return false;
        }
        return true;
    }

    public int processTask(ArrayList<String> tasks) {
        int resultTask = 0;
        boolean taskRefused = refuseTask(tasks.size());
        if (taskRefused) {
            return -1;
        }
        else {
            for (String task : tasks ){
                resultTask += this.processLine(task);
                if(!this.isWorking()){
                    // Si le serveur est en panne on ne va plus lui faire traiter d'opérations
                    return -2;

                }
            }
        }
        if(resultIsCorrect()){
            return resultTask;
        }
        else{
            Random rnd = new Random();
            return rnd.nextInt();
        }

    }

    public int processLine(String line){
        int resultLine = 0;
        String[] splitLine = line.split("\\s+");
        try {
            if(splitLine[0].equals("pell")){
                resultLine += this.getStub().pell(Integer.parseInt(splitLine[1])) % 4000;
            }
            else if(splitLine[0].equals("prime")){
                resultLine += this.getStub().prime(Integer.parseInt(splitLine[1])) % 4000;
            }
        } catch (RemoteException e) {
            //Ici le serveur n'a pas pu etre contacte, on considere qu'il est tombé en panne.
            this.setWorking(false);
            return -2;
        }
        return resultLine;
    }


    public Boolean resultIsCorrect() {
        Random rnd = new Random();
        float val = rnd.nextFloat();
        if(val < m) {
            return false;
        }
        else {
            return true;
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public ServerInterface getStub() {
        return stub;
    }

    public void setStub(ServerInterface stub) {
        this.stub = stub;
    }

    public int getQ() {
        return q;
    }

    public void setQ(int q) {
        this.q = q;
    }

    public void setWorking(boolean working) {
        isWorking = working;
    }

    public boolean isWorking() {
        return isWorking;
    }

    public float getM() {
        return m;
    }

    public void setM(float m) {
        this.m = m;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerObj serverObj = (ServerObj) o;
        return id == serverObj.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
