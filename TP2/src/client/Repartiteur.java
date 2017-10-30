package client;

import shared.ServerInterface;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by thmof on 17-10-16.
 */


/* TODO
    Gerer le taux de refus
    Gerer le cas de la panne ou un serveur est interrompu
    Gerer la verification des resultat par deux serveurs au moins
*/

public class Repartiteur {

    private int resultat;
    private Boolean secure;
    private ArrayList<ServerObj> listServer;
    String operationList[];


    public static void main(String[] args) throws IOException {
        Repartiteur rep = new Repartiteur();


        //gestion des commandes
        if(args.length == 0){
            System.out.println("Saisissez un argument");
        }
        else{
            rep.splitWork(Paths.get(Paths.get("").toAbsolutePath().toString() +"/fichiers/"+ args[0]));
        }
    }


    public Repartiteur(){
        resultat =0;
        secure = false;
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        Path serverConfig = Paths.get(Paths.get("").toAbsolutePath().toString() +"/server-list");
        listServer = new ArrayList<>();
        int i =0;
        try (Stream<String> lines = Files.lines(serverConfig)) {
            for (String line : (Iterable<String>) lines::iterator)
            {
                listServer.add(new ServerObj(i,line));
                i++;
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

        showServer();


    }

    /*
    * Display informations of connected server
    */
    void showServer(){
        for(ServerObj server : listServer){
            System.out.println("*** server "+server.getId()+" ***");
            System.out.println("ip :"+server.getIp());
            System.out.println("q :"+server.getQ());
            System.out.println();
        }
    }

    /*
     * Divise le travail de facon naive (en divise lintervalle en n serveur bloques et on reparti de maniere uniforme)
     */
    private void splitWork(Path file){
        try {
            List<String> lines = Files.readAllLines(file);
            operationList = lines.toArray(new String[lines.size()]);
            for(int i = 0; i< listServer.size() - 1; i++){
                processChunk(listServer.get(i), i*operationList.length/listServer.size(), (i+1)*operationList.length/listServer.size() );
            }
            processChunk(listServer.get(listServer.size()-1),(listServer.size()-1)*operationList.length/listServer.size(), operationList.length);
            joinWork();
        }
        catch(IOException e){
            e.printStackTrace();
        }

    }

    /*
     * Chaque serveur calcul son propre chunk
     */
    private void processChunk(ServerObj server, int start, int end){
        for(int i = start ; i < end ; i++){
            server.processLine(operationList[i]);
        }
    }



    /*
     * On joint les resultats des differents serveurs
     */
    private void joinWork(){
        for(ServerObj server : listServer){
            resultat+=server.getResultatPartiel();

        }

        System.out.println("resultat :"+resultat%4000);
    }





}