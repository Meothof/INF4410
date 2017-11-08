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
        int count = 0;
        WorkChunk tmpChunk = null;
        //Tant quil reste des elements non traite dans la queue on les envoie a la fonction splitWork
        while(!workChunks.isEmpty()){
//            tmpChunk = workChunks.remove();
//            System.out.println("Itération: "+ count++);
//            System.out.println("Nouvel itération sur  "+ tmpChunk.getStart()+", "+tmpChunk.getEnd());
            ArrayList<String> toProcess = new ArrayList<>();
            while(!workChunks.isEmpty()){
                toProcess.addAll(workChunks.remove().getChunk());
            }

            this.splitWork(toProcess);
            resultat = resultat%4000;
        }

    }


    private void splitWork(ArrayList<String> op){
        int res = 0;

        ArrayList<Future<ProcessedChunk>> futures = new ArrayList<Future<ProcessedChunk>>();

        ArrayList<Future<ProcessedChunk>> futuresToFinish = new ArrayList<Future<ProcessedChunk>>();

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

                            System.out.println("Panne du serveur" + processedChunk.getServerId());

                            //On supprime le serveur de notre liste
                            listServer.remove(getServerById(processedChunk.getServerId()));

                            //On rajoute la partie non traitee dans la queue de traitement
                            workChunks.offer(new WorkChunk(new ArrayList<String>(op.subList(processedChunk.getStart(),processedChunk.getEnd()))));

                            future.cancel(true);


                        } else if (processedChunk.getResult() == -1) {
                            //La tache a ete refusee par le serveur
                            workChunks.offer(new WorkChunk(new ArrayList<String>(op.subList(processedChunk.getStart(),processedChunk.getEnd()))));
                        } else {
                            if (this.secure) {
                                //En mode securise, on admet que le resultat renvoye par le serveur est valide.
                                resultat += processedChunk.getResult();
                            } else {

                                if (!verif(processedChunk.getOperations(), processedChunk.getResult(), processedChunk.getServerId())) {
                                    System.out.println("Le serveur "+processedChunk.getServerId() + " a renvoye une erreur");
                                    workChunks.offer(new WorkChunk(processedChunk.getOperations()));
                                } else {
                                    resultat += processedChunk.getResult();
                                }
                            }

                        }


                    } else {
                        // Cette partie n'a pas ete exécuté, on le rajoute donc dans la file des taches en attente
                        futuresToFinish.add(future);
                    }
                }

                futures.clear();

                ArrayList<String> operationRefusees = new ArrayList<>();

//                while(!workChunks.isEmpty()){
//                    operationRefusees.addAll(workChunks.remove().getChunk());
//                }

                futures = splitWorkToServers(operationRefusees);

                // On rajoute les taches en attente dans la listes des taches
                futures.addAll(futuresToFinish);
            }


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }


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

//                System.out.println("thread : "+i+ " chunk : "+nbOpTraites + ", "+(nbOpTraites+n));

                //Creation d'un callable Thread pour le serveur i avec le bon nombre d'operations
                if(n!=0) {
                    Callable<ProcessedChunk> thread = new Thread(nbOpTraites, nbOpTraites + n, op, server);


                    //Recuperation du resultat retourné par le serveur.
                    Future<ProcessedChunk> future = executor.submit(thread);

                    futures.add(future);

                    nbOpTraites += n;
                }

            }
        }

        executor.shutdown();
        return futures;
    }


    private boolean verif(ArrayList<String> op, int res, int s) {

        ArrayList<Future<ProcessedChunk>> futures = new ArrayList<Future<ProcessedChunk>>();

        ExecutorService executor = Executors.newFixedThreadPool(listServer.size());

        ServerObj server = null;
        int res2 = 0;
        for (int i=0; i<listServer.size(); i++) {
            if (i != s) {
//                System.out.println("    verification du resultat du serveur "+s+" avec le serveur "+i);
                server = listServer.get(i);

                //Si le serveur de verification a une plus petite capacite que le serveur initial, on decoupe lintervalle de maniere adequat
                int n_chunk = op.size()/server.getQ();
                int r = 0;
                for(int j = 0; j < n_chunk; j++){
//                    r = server.processTask(Arrays.copyOfRange(op, start + j*server.getQ(), start + (j+1)*server.getQ()));

                    //Creation d'un callable Thread pour le serveur i avec le bon nombre d'operations
                    Callable<ProcessedChunk> thread = new Thread(j*server.getQ(),  + (j+1)*server.getQ(), op, server);


                    //Recuperation du resultat retourné par le serveur.
                    Future<ProcessedChunk> future = executor.submit(thread);

                    futures.add(future);


                }

                //Creation d'un callable Thread pour le serveur i avec le bon nombre d'operations
                Callable<ProcessedChunk> thread = new Thread( (op.size()/server.getQ())*server.getQ(), op.size(), op, server);


                //Recuperation du resultat retourné par le serveur.
                Future<ProcessedChunk> future = executor.submit(thread);

                futures.add(future);






//                for(Future<ProcessedChunk> f : futures){
//                    try {
//                        ProcessedChunk processedChunk = null;
//                        while(!f.isDone()){}
//
//                        processedChunk = f.get();
//                        res2 += processedChunk.getResult();
//
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    } catch (ExecutionException e) {
//                        e.printStackTrace();
//                    }
//
//                }
//                if (res2 == res) {
////                    System.out.println("Résultat du serveur "+ s +" vérifié par le serveur "+server.getId());
//                    return true;
//                }
//
//            }
            }


        }
        // Recuperation des resultats des differents serveurs
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
                                    res2 += processedChunk.getResult();
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
//                    System.out.println("Résultat du serveur "+ s +" vérifié par le serveur "+server.getId());
                    return true;
                }
            }
        }



//        System.out.println("Résultat du serveur "+ s +" invalidé");
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