package client;

import shared.ServerInterface;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

public class ServerObj {
    private int id;
    private String ip;
    private ServerInterface stub;
    private int q;
    private int resultatPartiel;

    public ServerObj(int id, String ip) {
        this.id = id;
        this.ip = ip;
        this.stub = loadServerStub(getIp());
        this.resultatPartiel = 0;
    }

    public ServerInterface loadServerStub(String hostname) {
        ServerInterface stub = null;
        try {
            Registry registry = LocateRegistry.getRegistry(hostname, 5031);
            stub = (ServerInterface) registry.lookup("server");
            setQ(stub.getQ());
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

    public int processTask(String task[]) {
        boolean taskRefused = refuseTask(task.length);
        if (taskRefused) {
            return -1;
        }
        else {
            this.setResultatPartiel(0);
            for (int i = 0; i < task.length; i++) {
                this.processLine(task[i]);
            }
        }
        return this.getResultatPartiel();
    }

    public void processLine(String line){
        int tmpRes = 0;
        String[] splitLine = line.split("\\s+");
        try {
            if(splitLine[0].equals("pell")){
                tmpRes += this.getStub().pell(Integer.parseInt(splitLine[1])) % 4000;
            }
            else if(splitLine[0].equals("prime")){
                tmpRes += this.getStub().prime(Integer.parseInt(splitLine[1])) % 4000;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        this.setResultatPartiel(tmpRes + this.getResultatPartiel());
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

    public int getResultatPartiel() {
        return resultatPartiel;
    }

    public void setResultatPartiel(int resultatPartiel) {
        this.resultatPartiel = resultatPartiel;
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
