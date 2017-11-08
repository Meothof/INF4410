package client;

import server.Server;
import shared.ServerInterface;

import java.io.*;
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
//                List<String> lines = Files.readAllLines(Paths.get(Paths.get("").toAbsolutePath().toString() +"/fichiers/"+ args[0]));


//                Path path = Paths.get();
                File file = new File(Paths.get("").toAbsolutePath().toString() +"/fichiers/"+ args[0]);
                Scanner scan = new Scanner(file);
                while (scan.hasNextLine()){
                    rep.operationList.add( scan.nextLine() );
                }
                scan.close();
                rep.workChunks.offer(new WorkChunk(0, rep.operationList.size()));
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
        int count = 0;
        WorkChunk tmpChunk = null;
        while(!workChunks.isEmpty()){
            tmpChunk = workChunks.remove();
            System.out.println("Itération: "+ count++);
            System.out.println("Nouvel itération sur  "+ tmpChunk.getStart()+", "+tmpChunk.getEnd());
            this.splitWork(tmpChunk.getStart(), tmpChunk.getEnd());
            resultat = resultat%4000;
        }

    }

    /*
     * Répartition du travail de manière à maximiser la taille des tâches envoyées sans recevoir trop de refus (10%)
     */
    /*
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
*/
    private void splitWork(int start, int end){
        int res = 0;

        ArrayList<String> op = (ArrayList<String>) operationList.subList(start, end);

        ArrayList<Future<ProcessedChunk>> futures = new ArrayList<Future<ProcessedChunk>>();

        ArrayList<Future<ProcessedChunk>> futuresToFinish = new ArrayList<Future<ProcessedChunk>>();



        futures = splitWorkToServers(op);





        try {
            while(!futures.isEmpty()) {
                futuresToFinish.clear();
                for (Future<ProcessedChunk> future : futures) {
                    ProcessedChunk processedChunk = null;

                    if (future.isDone()) {

                        processedChunk = future.get();
//                    System.out.println(processedChunk.toString());

                        if (processedChunk.getResult() == -2) { //le serveur est tombé en panne
                            System.out.println("Panne du serveur" + processedChunk.getServerId());

                            //On supprime le serveur de notre liste
                            listServer.remove(getServerById(processedChunk.getServerId()));

                            //On rajoute la partie non traitee dans la queue de traitement
                            workChunks.offer(new WorkChunk(processedChunk.getStart(), processedChunk.getEnd()));

                        } else if (processedChunk.getResult() == -1) {
                            System.out.println("chunk : " + processedChunk.getStart() + ", " + processedChunk.getEnd() + " - refusé par Server: " + processedChunk.getServerId());
                            workChunks.offer(new WorkChunk(processedChunk.getStart(), processedChunk.getEnd()));


                        } else {
                            if (this.secure) {
//                                System.out.println("chunk : " + processedChunk.getStart() + ", " + processedChunk.getEnd() + " - accepté par Server: " + processedChunk.getServerId());
                                resultat += processedChunk.getResult();
                            } else {

//                                if (!verif(op, processedChunk.getResult(), processedChunk.getServerId(), processedChunk.getStart(), processedChunk.getEnd())) {
//                                    workChunks.offer(new WorkChunk(processedChunk.getStart(), processedChunk.getEnd()));
//                                } else {
//                                    resultat += processedChunk.getResult();
//                                }
                            }

                        }


                    } else {
                        futuresToFinish.add(future);
//                        System.out.println("future is not done");
                    }
                }

                futures.clear();

                ArrayList<String> operationRefusees = new ArrayList<>();
//                operationRefusees =

                for(WorkChunk workChunk: workChunks){

                }

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

        ExecutorService executor = Executors.newFixedThreadPool(4);

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


    private boolean verif(ArrayList<String> op, int res, int s, int start, int end) {

        ArrayList<Future<ProcessedChunk>> futures = new ArrayList<Future<ProcessedChunk>>();

        ExecutorService executor = Executors.newFixedThreadPool(4);

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
//                    r = server.processTask(Arrays.copyOfRange(op, start + j*server.getQ(), start + (j+1)*server.getQ()));

                    //Creation d'un callable Thread pour le serveur i avec le bon nombre d'operations
                    Callable<ProcessedChunk> thread = new Thread(start + j*server.getQ(), start + (j+1)*server.getQ(), op, server);


                    //Recuperation du resultat retourné par le serveur.
                    Future<ProcessedChunk> future = executor.submit(thread);

                    futures.add(future);


//                    if(!server.isWorking()){
//                        listServer.remove(server);
//                        verif(op, res, s, start, end);
//                        break;
//                    }else{
//                        res2 += r;
//                    }
                }

                //Creation d'un callable Thread pour le serveur i avec le bon nombre d'operations
                Callable<ProcessedChunk> thread = new Thread(start + ((end - start)/server.getQ())*server.getQ(), end, op, server);


                //Recuperation du resultat retourné par le serveur.
                Future<ProcessedChunk> future = executor.submit(thread);

                futures.add(future);




                for(Future<ProcessedChunk> f : futures){
                    try {
                        ProcessedChunk processedChunk = null;
                        while(!f.isDone()){}

                        processedChunk = f.get();
                        res2 += processedChunk.getResult();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

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