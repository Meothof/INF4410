package client;

import server.Server;
import shared.ServerInterface;

import java.io.*;
import java.net.PortUnreachableException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.stream;

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
    private ArrayList<String> operationList;

    public static void main(String[] args) throws IOException {
        long debut = System.currentTimeMillis();
        Repartiteur rep = new Repartiteur();
        if(args.length == 0){
            System.out.println("Saisissez un argument");
        }
        else{
            try{
                //Lecture initial du fichier d'opérations
                File file = new File(Paths.get("").toAbsolutePath().toString() +"/fichiers/"+ args[0]);
                Scanner scan = new Scanner(file);
                while (scan.hasNextLine()){
                    rep.operationList.add( scan.nextLine() );
                }
                scan.close();
                rep.workChunks.offer(new WorkChunk( rep.operationList));
                rep.processQueue();
            }
            catch(IOException e){
                e.printStackTrace();
            }

        }
        System.out.println("Résultat : " + (rep.resultat));
        System.out.print("Temps execution: ");
        System.out.println(System.currentTimeMillis()-debut);
        System.exit(0);
    }

    public Repartiteur(){
        resultat = 0;
        this.operationList = new ArrayList<String>();

        workChunks = new ArrayDeque<WorkChunk>();
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        listServer = new ArrayList<>();
        int i = 0;
        try {
            // Configuration du répartiteur
            File file = new File(Paths.get("").toAbsolutePath().toString() +"/conf_repartiteur");
            Scanner scan = new Scanner(file);
            if(scan.hasNext()){
                if(scan.nextLine().equals("true")){
                    this.secure = true;
                }
                else{
                    this.secure =false;
                }
            }
            while (scan.hasNextLine()){
                listServer.add(new ServerObj(i, scan.nextLine()));
                i++;
            }
            scan.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Mode secure : " + secure);
        showServer();
    }

    /*
     * Affiche les informations ddes différents serveurs connectés
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
     * Re-itère l'opération tant que la queue de travail n'est pas vide
     */
    private void processQueue(){
        int count = 0;
        WorkChunk tmpChunk = null;
        //Tant quil reste des elements non traite dans la queue on les envoie a la fonction splitWork
        while(!workChunks.isEmpty()){
            ArrayList<String> toProcess = new ArrayList<>();
            while(!workChunks.isEmpty()){
                toProcess.addAll(workChunks.remove().getChunk());
            }

            this.splitWork(toProcess);
            resultat = resultat%4000;
        }

    }


    /*
     * Envoi les opération au serveur, recupère les résultats, et replace les opérations non traités dans la queue
     */
    private void splitWork(ArrayList<String> op){
        int res = 0;

        ArrayList<Future<ProcessedChunk>> futures = new ArrayList<Future<ProcessedChunk>>();

        ArrayList<Future<ProcessedChunk>> futuresToFinish = new ArrayList<Future<ProcessedChunk>>();

        // Récupération des résultats partiels renvoyés par les serveurs
        futures = splitWorkToServers(op);


        try {
            while(!futures.isEmpty()) {
                futuresToFinish.clear();
                for (Future<ProcessedChunk> future : futures) {
                    ProcessedChunk processedChunk = null;

                    //Si la tache est terminee
                    if (future.isDone()) {

                        processedChunk = future.get();

                        if (processedChunk.getResult() == -2) { //le serveur est tombé en panne
                            if(listServer.contains(getServerById(processedChunk.getServerId()))){
                                System.out.println("Panne du serveur " + processedChunk.getServerId());
                            }

                            //On supprime le serveur de notre liste
                            listServer.remove(getServerById(processedChunk.getServerId()));

                            //On rajoute la partie non traitée dans la queue de traitement
                            workChunks.offer(new WorkChunk(new ArrayList<String>(op.subList(processedChunk.getStart(),processedChunk.getEnd()))));

                            future.cancel(true);


                        } else if (processedChunk.getResult() == -1) { //La tache à ete refusée par le serveur
                            workChunks.offer(new WorkChunk(new ArrayList<String>(op.subList(processedChunk.getStart(),processedChunk.getEnd()))));
                        } else {
                            if (this.secure) {
                                //En mode securise, on admet que le resultat renvoye par le serveur est valide.
                                resultat += processedChunk.getResult();
                            } else {

                                if(listServer.size()>=2) {
                                    if (!verif(processedChunk.getOperations(), processedChunk.getResult(), processedChunk.getServerId())) {
                                        //                                    System.out.println("Le serveur "+processedChunk.getServerId() + " a renvoye une erreur");
                                        workChunks.offer(new WorkChunk(processedChunk.getOperations()));
                                    } else {
                                        resultat += processedChunk.getResult();
                                    }
                                }else{
                                    System.out.println("nombre de serveur insuffisant");
                                    System.exit(0);
                                }
                            }

                        }
                    } else {
                        // Cette partie n'a pas ete exécuté, on le rajoute donc dans la file des taches en attente
                        futuresToFinish.add(future);
                    }
                }
                futures.clear();
                // On rajoute les taches en attente dans la listes des taches
                futures.addAll(futuresToFinish);
            }


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }


    /*
     * Repartie les opérations op entre les différents serveur de manière non-bloquante
     */
    private ArrayList<Future<ProcessedChunk>> splitWorkToServers(ArrayList<String> op){
        ArrayList<Future<ProcessedChunk>> futures = new ArrayList<Future<ProcessedChunk>>();

        ExecutorService executor = Executors.newFixedThreadPool(listServer.size());

        int nbOpTotal = op.size();
//            System.out.println("Nombre d'opérations à traiter : "+nbOpTotal);
        int nbOpTraites = 0;
        int n = 0;
        ServerObj server = null;
        while (nbOpTraites < nbOpTotal) {
            for (int i = 0; i < listServer.size(); i++) {
                //System.out.println(nbOpTraites);
                server = listServer.get(i);
                if ((nbOpTotal - nbOpTraites) < server.getQ()) {
                    n = nbOpTotal - nbOpTraites;
                } else {
                    n = (int) Math.floor(1.5 * (float) (server.getQ())); //pour un taux de refus de 10%
                    if (n > (nbOpTotal - nbOpTraites)) {
                        n = server.getQ();
                    }
                }
                //Creation d'un callable Thread pour le serveur i avec le bon nombre d'operations
                if(n!=0) {
                    Callable<ProcessedChunk> thread = new Thread(nbOpTraites, nbOpTraites + n, op, server);

                    //Récuperation du resultat retourné par le serveur.
                    Future<ProcessedChunk> future = executor.submit(thread);

                    futures.add(future);

                    nbOpTraites += n;
                }

            }
        }

        executor.shutdown();
        return futures;
    }


    /*
     * Vérifie si au moins un des serveur à un resultats différent pour les opérations op que le serveur s
     */
    private boolean verif(ArrayList<String> op, int res, int s) {

        ArrayList<Future<ProcessedChunk>> futures = new ArrayList<Future<ProcessedChunk>>();

        ExecutorService executor = Executors.newFixedThreadPool(listServer.size());

        int res2 = 0;
        for (ServerObj server : listServer) {
            if (server.getId() != s) {


                //Si le serveur de vérification a une plus petite capacité que le serveur initial, on découpe l'intervalle de manière adequat
                int n_chunk = op.size()/server.getQ();
                int r = 0;

                for(int j = 0; j < n_chunk; j++){

                    //Création d'un callable Thread pour le serveur i avec le bon nombre d'opérations
                    Callable<ProcessedChunk> thread = new Thread(j*server.getQ(),  + (j+1)*server.getQ(), op, server);

                    //Recuperation du résultat retourné par le serveur.
                    Future<ProcessedChunk> future = executor.submit(thread);

                    futures.add(future);


                }

                //Création d'un callable Thread pour le serveur i avec le bon nombre d'operations
                Callable<ProcessedChunk> thread = new Thread( (op.size()/server.getQ())*server.getQ(), op.size(), op, server);


                //Récuperation du resultat retourné par le serveur.
                Future<ProcessedChunk> future = executor.submit(thread);

                futures.add(future);

            }


        }
        // Récuperation des résultats des différents serveurs
        for(ServerObj serverObj : listServer){
            if(s!=serverObj.getId()) {

                ProcessedChunk processedChunk = null;
                while (!futures.isEmpty()) {
                    ArrayList<Future<ProcessedChunk>> nextFutures = new ArrayList<Future<ProcessedChunk>>();
                    try {
                        for (Future<ProcessedChunk> future : futures) {
                            if (future.isDone()) {
                                processedChunk = future.get();
                                if (serverObj.getId() == processedChunk.getServerId()) {
                                    if(res == -2){ //Le serveur est tombé en panne
                                        // On abandonne ce résultat
                                        continue;
                                    }
                                    else{
                                        res2 += processedChunk.getResult();
                                    }
                                }
                            } else {
                                nextFutures.add(future);
                            }
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    futures.clear();
                    futures.addAll(nextFutures);
                }
                if (res2 == res) {
                    // Le résultat du serveur a été valide par un autre serveur
                    return true;
                }
                else{
                    if (res2 == -2){
                        // Le serveur est tombé en panne
                        continue;
                    }
                }
            }
        }
        // Le resultat n'a pas été valide par les autres serveur
        return false;
    }


    private ServerObj getServerById(int id){
        for(ServerObj server : listServer){
            if (server.getId() == id){
                return server;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Repartiteur{" +
                "workChunks=" + workChunks +
                '}';
    }
}