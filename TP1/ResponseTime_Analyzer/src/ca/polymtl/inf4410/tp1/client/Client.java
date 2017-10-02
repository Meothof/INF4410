package ca.polymtl.inf4410.tp1.client;

import java.util.*;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import ca.polymtl.inf4410.tp1.shared.ServerInterface;
import ca.polymtl.inf4410.tp1.shared.Tools;

public class Client {
	public static void main(String[] args) throws IOException{
		Client client = new Client();
		//Genere un identifiant à partir du server
		client.getId();
		//gestion des commandes
		client.handleArgs(args);
		//client.run();
	}

	FakeServer localServer = null;
	private ServerInterface localServerStub = null;
	private ServerInterface distantServerStub = null;
	private Tools tools = new Tools();

	public Client() {
		super();
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		localServer = new FakeServer();
		localServerStub = loadServerStub("127.0.0.1");
				distantServerStub = loadServerStub("132.207.12.216");
//		distantServerStub = loadServerStub("127.0.0.1");

	}

	//	private void run() {
	//		appelNormal();
	//
	//		if (localServerStub != null) {
	//			appelRMILocal();
	//		}
	//
	//		if (distantServerStub != null) {
	//			appelRMIDistant();
	//		}
	//	}


	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
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

	/*
	* Vérifie si le fichier fileId existe
	* si il existe on ne fait rien
	* sinon on crée un nouveau fichier binaire contenant un UUID généré par le serveur
	*/
	private void getId() throws IOException {
		String currentDirectory= Paths.get("").toAbsolutePath().toString();
		File file= new File(currentDirectory + "/fileId"  );
		if(!file.exists()){
			Path path = Paths.get(currentDirectory + "/fileId");
			Files.write(path, distantServerStub.generateClientId());
		}
	}

	/*
	* Affiche les commandes disponibles pour l'utilisateur.
	*/


	private void get(String fileName) throws IOException{
		String currentDirectory= Paths.get("").toAbsolutePath().toString();
		String filePath = "/client-files/"+ fileName;
		File file= new File(currentDirectory + filePath );
		if(!file.exists()){
			Path path = Paths.get(currentDirectory + filePath);
			Files.write(path, distantServerStub.get(fileName, null));
			System.out.println("Fichier ajouté : "+fileName);
		}
		else{
			Path path = Paths.get(currentDirectory +filePath);
			byte[] newContent = distantServerStub.get(fileName, tools.checksum(filePath));
			if(newContent != null){
				Files.write(path, newContent );
				System.out.println("Fichier mis à jour : " + fileName);
			}
			else{
				System.out.println("Le fichier est déjà à jour : " + fileName);
			}
		}
	}

	/*
	* Gestion des commandes à l'aide d'un switch/case
	*/
	private void handleArgs(String args[]) throws IOException{
		ArrayList<String> fileNames;
		byte[] clientId;
		String currentDirectory;
		Path path;
		if(args.length ==0){
			System.out.println("Saisissez un argument");
		}
		else{
			switch(args[0]){
				case("create"):
					int resCreate = 0;
					try{
						resCreate =distantServerStub.create(args[1]);
					} catch (IOException e) {
						e.printStackTrace();
					}

					if(resCreate==1){
						System.out.println("fichier cree");
					}else{
						System.out.println("fichier deja cree");
					}

					break;
				case("list"):
					fileNames = distantServerStub.list();
					for(String fileName : fileNames){
						System.out.println("* "+fileName );
					}
					break;
				case("get"):
					get(args[1]);
					break;
				case("syncLocalDir"):
					fileNames = distantServerStub.list();
					for(String fileName : fileNames){
						get(fileName);
					}
					break;
				case("lock"):
					clientId = null;
					currentDirectory= Paths.get("").toAbsolutePath().toString();
					path = Paths.get(currentDirectory + "/fileId");
					clientId = Files.readAllBytes(path);
					if(distantServerStub.lock(args[1],clientId)==0) {
						System.out.println(args[1]+ " déjà verrouillé par un autre utilisateur");
					}
					else {
						System.out.println(args[1]+ " verrouillé ");
						get(args[1]);
					}
					break;
				case("push"):
					clientId = null;
					currentDirectory= Paths.get("").toAbsolutePath().toString();
					path = Paths.get(currentDirectory + "/fileId");
					clientId = Files.readAllBytes(path);
					int resPush = 0;
					byte[] content = null;
					try {
						Path file = Paths.get(Paths.get("").toAbsolutePath().toString() + "/client-files/" + args[1]);
						content = Files.readAllBytes(file);

					} catch (IOException e) {
						e.printStackTrace();
					}
					try{
						resPush =distantServerStub.push(args[1], content, clientId);
					} catch (IOException e) {
						e.printStackTrace();
					}
					if( resPush != 1) {
						System.out.println("Vous devez lock le fichier avant de push.");
					}
					else {
						System.out.println("Fichier mis à jour.");
					}
					break;
				default:
					System.out.println("Saisissez un argument valide");
					break;
			}
		}
	}
}
