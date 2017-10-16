package client;

import shared.ServerInterface;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Created by thmof on 17-10-16.
 */
public class Repartiteur {

    private int resultat;
    private ServerInterface stub = null;


    public static void main(String[] args) throws IOException {
        Repartiteur rep = new Repartiteur();

        //gestion des commandes
        if(args.length == 0){
            System.out.println("Saisissez un argument");
        }
        else{
            rep.readFile(Paths.get(Paths.get("").toAbsolutePath().toString() +"/fichiers/"+ args[0]));

        }


    }


    public Repartiteur(){
        resultat =0;
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        stub = loadServerStub("132.207.12.47");

    }

    private ServerInterface loadServerStub(String hostname) {
        ServerInterface stub = null;

        try {
            Registry registry = LocateRegistry.getRegistry(hostname, 5002);
            stub = (ServerInterface) registry.lookup("server");
        } catch (NotBoundException e) {
            System.out.println("Erreur: Le nom '" + e.getMessage()
                    + "' n'est pas défini dans le registre.");
        } catch (AccessException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
        return stub;
    }

    private void readFile(Path file){
        try (Stream<String> lines = Files.lines(file)) {
            for (String line : (Iterable<String>) lines::iterator)
            {
                processLine(line);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(resultat%4000);
    }

    private void processLine(String line){

        String[] splitLine = line.split("\\s+");
        try {
            if(splitLine[0].equals("pell")){
                resultat+=stub.pell(Integer.parseInt(splitLine[1]))%4000;
            }
            else if(splitLine[0].equals("prime")){
                resultat+=stub.prime(Integer.parseInt(splitLine[1]))%4000;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
