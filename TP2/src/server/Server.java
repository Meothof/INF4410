package server;

import shared.Operations;
import shared.ServerInterface;

import java.net.Inet4Address;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

public class Server implements ServerInterface {
    private int q;
    private float m; //si 0 non malicieux sinon malicieux
    private float t; //taux de refus

    public static void main(String[] args) {
        if(args.length <2){
            System.out.println("Saisissez les arguments n et m");

        } else if (Float.parseFloat(args[0])<0){
            System.out.println("Entrer une valeur de m entre 0 et 100");
        } else if (Integer.parseInt(args[1]) < 0) {
            System.out.println("Entrer une valeur de q positive");
        } else {
            Server server = new Server(Integer.parseInt(args[0]), Float.parseFloat(args[1]));
            server.run();
        }
    }

    public Server(int q, float m){
        super();
        this.q = q;
        this.m = m/100;
    }

    public int getQ() throws RemoteException{
        return q;
    }

    public float getM() throws RemoteException {
        return m;
    }


    private void run() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(this, 5030);
            Registry registry = LocateRegistry.getRegistry(5031);
            registry.rebind("server", stub);
            System.out.println("Server ready.");
            System.out.println("IP :"+Inet4Address.getLocalHost().getHostAddress());
        } catch (ConnectException e) {
            System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
            System.err.println();
            System.err.println("Erreur: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }

    @Override
    public int pell(int x) throws RemoteException{
//        System.out.println("pell "+x);
            return Operations.pell(x);

    }

    @Override
    public int prime(int x) throws RemoteException{
//            System.out.println("prime "+x);
            return Operations.prime(x);

    }

    //Retourne si le resultat renvoye sera correct avec la probabilite m.
    public Boolean resultIsCorrect() {
        Random rnd = new Random();
        float val = rnd.nextFloat();
        System.out.println("m "+ m+" v" +val);
        if(val < m) {
//            System.out.println("Serveur malicieux");
            return false;
        }
        else {
//            System.out.println("Serveur non malicieux");
            return true;
        }
    }

}
