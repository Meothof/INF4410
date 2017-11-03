package client;

import shared.ServerInterface;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/* TODO
    Gerer le taux de refus : OK
    Gerer le cas de la panne ou un serveur est interrompu
    Gerer la verification des resultat par deux serveurs au moins
*/

public class Repartiteur {

    private int resultat;
    private Boolean secure = true;
    private ArrayList<ServerObj> listServer;
    String operationList[];

    public static void main(String[] args) throws IOException {
        Repartiteur rep = new Repartiteur();
        if(args.length == 0){
            System.out.println("Saisissez un argument");
        }
        else{
            rep.splitWork(Paths.get(Paths.get("").toAbsolutePath().toString() +"/fichiers/"+ args[0]));
        }
    }

    public Repartiteur(){
        resultat = 0;
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        Path serverConfig = Paths.get(Paths.get("").toAbsolutePath().toString() +"/server-list");
        listServer = new ArrayList<>();
        int i = 0;
        try (Stream<String> lines = Files.lines(serverConfig)) {
            for (String line : (Iterable<String>) lines::iterator) {
                listServer.add(new ServerObj(i,line));
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        showServer();
    }

    /*
    * Display informations of connected server
    */
    private void showServer() {
        for(ServerObj server : listServer){
            System.out.println("*** server "+server.getId()+" ***");
            System.out.println("ip :"+server.getIp());
            System.out.println("q :"+server.getQ());
            System.out.println();
        }
    }

    /*
     * Répartition du travail de manière à maximiser la taille des tâches envoyées sans recevoir trop de refus (10%)
     */
    private void splitWork(Path file) {
        try {
            List<String> lines = Files.readAllLines(file);
            operationList = lines.toArray(new String[lines.size()]);
            int nbOpTotal = operationList.length;
            System.out.println("Nombre d'opérations à traiter : "+nbOpTotal);
            int nbOpTraites = 0;
            int n = 0;
            int res = 0;
            int nbRefus = 0;
            ServerObj server = null;
            while (nbOpTraites < nbOpTotal) {
                for (int i=0; i<listServer.size(); i++) {
                    //System.out.println(nbOpTraites);
                    server = listServer.get(i);
                    if ((nbOpTotal - nbOpTraites) < server.getQ()) {
                        n = nbOpTotal - nbOpTraites;
                    }
                    else {
                        n = (int)Math.floor(1.5 * (float)(server.getQ())); //pour un taux de refus de 10%
                        if (n > (nbOpTotal - nbOpTraites)) {
                            n = server.getQ();
                        }
                    }
                    res = server.processTask(Arrays.copyOfRange(operationList, nbOpTraites, nbOpTraites + n));
                    if (!this.secure) {
                        if (!verif(res, i, nbOpTraites, nbOpTraites + n)) {
                            res = -1; //résultat pas fiable -> pas pris en compte
                        }
                    }
                    if (res != -1) {
                        nbOpTraites += n;
                        resultat += res;
                    }
                    else {
                        nbRefus++;
                    }
                }
            }
            System.out.println("Résultat : " + (resultat%4000));
            System.out.println("Nombre de refus reçus : " + nbRefus);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private boolean verif(int res, int s, int start, int end) {
        ServerObj server = null;
        int res2 = 0;
        for (int i=0; i<listServer.size(); i++) {
            if (i != s) {
                server = listServer.get(i);
                res2 = server.processTask(Arrays.copyOfRange(operationList, start, end));
                if (res == res2) {
                    return true;
                }
            }
        }
        return false;
    }
}