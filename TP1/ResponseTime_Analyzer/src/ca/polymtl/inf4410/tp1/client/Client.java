package ca.polymtl.inf4410.tp1.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ca.polymtl.inf4410.tp1.shared.ServerInterface;
import java.util.Random;

public class Client {
	public static void main(String[] args) {
		Client client = new Client();
		if (args.length > 0) {
			if (args[0].equals("create")) {
				client.create(args[1]);
			}
		}

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
		distantServerStub = loadServerStub("132.207.12.216");
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

	private void create(String fileName){
		localServerStub.create(fileName);
	}

}
