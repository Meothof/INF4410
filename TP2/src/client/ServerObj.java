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
    private float m;
    private int resultatPartiel;
    private boolean isWorking;

    public ServerObj(int id, String ip) {
        this.id = id;
        this.ip = ip;
        this.m = 0;
        this.stub = loadServerStub(getIp());
        this.resultatPartiel = 0;
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

    public int processTask(String task[]) {
        boolean taskRefused = refuseTask(task.length);
        if (taskRefused) {
            return -1;
        }
        else {
            this.setResultatPartiel(0);
            for (int i = 0; i < task.length; i++) {
                this.processLine(task[i]);
                if(!this.isWorking()){
                    // Si le serveur est en panne on ne va plus lui faire traiter d'opérations
                    return -2;
//                    break;
                }
            }
        }
        if(resultIsCorrect()){
            return this.getResultatPartiel();
        }
        else{
            Random rnd = new Random();
            return rnd.nextInt();
        }

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
            //Ici le serveur n'a pas pu etre contacte, on considere qu'il est tombé en panne.
            this.setWorking(false);
            return;
        }
        this.setResultatPartiel(tmpRes + this.getResultatPartiel());
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

    public int getResultatPartiel() {
        return resultatPartiel;
    }

    public void setResultatPartiel(int resultatPartiel) {
        this.resultatPartiel = resultatPartiel;
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
