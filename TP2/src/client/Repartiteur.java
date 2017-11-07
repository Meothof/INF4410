package client;

import shared.ServerInterface;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.*;
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
    private Queue<WorkChunk> workChunks;
    String operationList[];

    public static void main(String[] args) throws IOException {
        long debut = System.currentTimeMillis();
        Repartiteur rep = new Repartiteur();
        if(args.length == 0){
            System.out.println("Saisissez un argument");
        }
        else{
            try{
                List<String> lines = Files.readAllLines(Paths.get(Paths.get("").toAbsolutePath().toString() +"/fichiers/"+ args[0]));
                rep.operationList = lines.toArray(new String[lines.size()]);
                rep.workChunks.offer(new WorkChunk(0, lines.size()));
//                rep.splitWork(rep.operationList);
                rep.processQueue();
            }
            catch(IOException e){
                e.printStackTrace();
            }

        }
        System.out.print("Temps execution: ");
        System.out.println(System.currentTimeMillis()-debut);
    }

    public Repartiteur(){
        resultat = 0;
        workChunks = new ArrayDeque<WorkChunk>();
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

    private void processQueue(){
        WorkChunk tmpChunk = null;
        while(!workChunks.isEmpty()){
            tmpChunk = workChunks.remove();
            splitWork(Arrays.copyOfRange(operationList, tmpChunk.getStart(), tmpChunk.getEnd()));
        }
        System.out.println("Résultat : " + (resultat%4000));
    }

    /*
     * Répartition du travail de manière à maximiser la taille des tâches envoyées sans recevoir trop de refus (10%)
     */
    private void splitWork(String[] op) {
            int nbOpTotal = op.length;
//            System.out.println("Nombre d'opérations à traiter : "+nbOpTotal);
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
//                    System.out.println(n+" operations envoyees au serveur "+server.getId());

                    //Calcul d'une partie des résultats par le serveur i
                    res = server.processTask(Arrays.copyOfRange(op, nbOpTraites, nbOpTraites + n));
//                    System.out.println(server.getId()+" is working "+server.isWorking());


                    //le serveur est en panne
                    if(!server.isWorking()){

                        System.out.println("Panne du serveur"+ server.getId());
                        //On supprime le serveur
                        listServer.remove(server);

                        //On rajoute la partie non traitee dans la queue de traitement
                        workChunks.offer(new WorkChunk(nbOpTraites, nbOpTraites + n));

                        //on compte l'operation comme traite
                        nbOpTraites += n;
                    }

                    else{
                        if (!this.secure) {

                            //modif theo
                            //En mode non securise tant que la valeur nest pas validee par au moins deux serveurs, on relance le calcul
                            while(!verif(op, res, i, nbOpTraites, nbOpTraites + n)){
                                res = server.processTask(Arrays.copyOfRange(op, nbOpTraites, nbOpTraites + n));
                            }
                        }
                        if (res != -1) {
                            nbOpTraites += n;
                            resultat += res;
                        }
                    }
                }
            }




    }


    private boolean verif(String[] op, int res, int s, int start, int end) {
        ServerObj server = null;
        int res2 = 0;
        for (int i=0; i<listServer.size(); i++) {
            if (i != s) {
//                System.out.println("    verification du resultat du serveur "+s+" avec le serveur "+i);
                server = listServer.get(i);

                //Si le serveur de verification a une plus petite capacite que le serveur initial, on decoupe lintervalle de maniere adequat
                int n_chunk = (end - start)/server.getQ();
                int r = 0;
                for(int j = 0; j < n_chunk; j++){
                    r = server.processTask(Arrays.copyOfRange(op, start + j*server.getQ(), start + (j+1)*server.getQ()));
                    if(!server.isWorking()){
//                        listServer.remove(server);
//                        verif(op, res, s, start, end);
                        break;
                    }else{
                        res2 += r;
                    }
                }
                res2 += server.processTask(Arrays.copyOfRange(op, start + ((end - start)/server.getQ())*server.getQ(), end));
                if (res == res2) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Repartiteur{" +
                "workChunks=" + workChunks +
                '}';
    }
}