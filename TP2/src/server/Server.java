package server;

import shared.Operations;
import shared.ServerInterface;

import java.net.Inet4Address;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by thmof on 17-10-16.
 */
public class Server implements ServerInterface {
    private int q;
    private float m; //si 0 non malicieux, >0 malicieux

    public static void main(String[] args) {
        if(args.length !=2){
            System.out.println("Saisissez les arguments q et m");
        }else{
            Server server = new Server(Integer.parseInt(args[0]), Float.parseFloat(args[1]));
            server.run();
        }
    }

    public Server(int q, float m){
        super();
        this.q = q;
        this.m = m;
    }

    public int getQ() {
        return q;
    }

    public float getM() {
        return m;
    }


    private void run() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            ServerInterface stub = (ServerInterface) UnicastRemoteObject
                    .exportObject(this, 5001);

            Registry registry = LocateRegistry.getRegistry(5002);
            registry.rebind("server", stub);
            System.out.println("Server ready.");
            System.out.println("IP :"+Inet4Address.getLocalHost().getHostAddress());
        } catch (ConnectException e) {
            System.err
                    .println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
            System.err.println();
            System.err.println("Erreur: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }

    @Override
    public int pell(int x) throws RemoteException{
        return Operations.pell(x);
    }
    @Override
    public int prime(int x) throws RemoteException{
        return Operations.prime(x);
    }


}
