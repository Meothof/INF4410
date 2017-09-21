package ca.polymtl.inf4410.tp1.client;
import java.util.*;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ca.polymtl.inf4410.tp1.shared.ServerInterface;
import java.util.Random;
import java.io.IOException;

public class Client {
	public static void main(String[] args) throws IOException{
		Client client = new Client();
		client.displayOptions();
		client.handleArgs(args);


		//client.run();
	}

	FakeServer localServer = null;
	private ServerInterface localServerStub = null;
	private ServerInterface distantServerStub = null;

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

	private void displayOptions(){
		System.out.println("Créer un fichier : create <filename> " );
	}

	private void handleArgs(String args[]) throws IOException{
		ArrayList<String> fileNames;
		if(args.length ==0){
			System.out.println("Saisissez un argument");
		}
		else{
			switch(args[0]){
				case("create"):
					distantServerStub.create(args[1]);
					break;
				case("list"):
					fileNames = distantServerStub.list();
					for(String fileName : fileNames){
						System.out.println("* "+fileName);
					}
					break;
				default:
					System.out.println("Saisissez un argument valide");
					break;

			}
		}

	}


}
