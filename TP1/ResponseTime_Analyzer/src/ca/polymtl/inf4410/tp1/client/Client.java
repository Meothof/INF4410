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
//		client.displayOptions();
		//Genere un identifiant à partir du server
		client.getId();
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
		//		distantServerStub = loadServerStub("132.207.12.216");
		distantServerStub = loadServerStub("127.0.0.1");

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
	private void displayOptions(){
		System.out.println("Créer un fichier : create <filename> " );
	}

	private void get(String fileName) throws IOException{
		String currentDirectory= Paths.get("").toAbsolutePath().toString();
		String filePath = "/client-files/"+ fileName;
		File file= new File(currentDirectory + filePath );
		if(!file.exists()){
			Path path = Paths.get(currentDirectory + filePath);
			Files.write(path, distantServerStub.get(fileName, null));
			System.out.println("Fichier ajoute  : "+fileName);
		}
		else{
			Path path = Paths.get(currentDirectory +filePath);
			byte[] newContent = distantServerStub.get(fileName, tools.checksum(filePath));
			if(newContent != null){
				Files.write(path, newContent );
				System.out.println("Fichier mis a jour  : "+fileName);
			}
			else{
				System.out.println("Fichier est deja a jour  : "+fileName);
			}

		}

	}
	/*
	* Gestion des commandes à l'aide d'un switch/case
	*/
	private void handleArgs(String args[]) throws IOException{
		ArrayList<String> fileNames;
		if(args.length ==0){
			System.out.println("Saisissez un argument");
		}
		else{
			switch(args[0]){
				case("create"):
					distantServerStub.create(args[1]);
					System.out.println("file created");
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
					byte[] clientId = null;
					String currentDirectory= Paths.get("").toAbsolutePath().toString();
					Path path = Paths.get(currentDirectory + "/fileId");
					clientId = Files.readAllBytes(path);

					if(distantServerStub.lock(args[1],clientId)==0){
						System.out.println(args[1]+ " deja verrouille par :");
					}else{
						System.out.println(args[1]+ " verrouille ");
						get(args[1]);
					}
					break;

				default:
					System.out.println("Saisissez un argument valide");
					break;

			}


		}


	}


}
