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
		String distantHostname = null;
		int x = 1;

		if (args.length > 0) {
			distantHostname = args[0];
		}
		if (args.length > 1) {
			x = Integer.parseInt(args[1]);

		}


		Client client = new Client(distantHostname,x);
		client.run();
	}

	FakeServer localServer = null; // Pour tester la latence d'un appel de
	// fonction normal.
	private ServerInterface localServerStub = null;
	private ServerInterface distantServerStub = null;
	private double power;
	private byte[] data = new byte[(int)power];
	public Client(String distantServerHostname, int x) {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		localServer = new FakeServer();
		localServerStub = loadServerStub("127.0.0.1");

		if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname);
		}
		power = java.lang.Math.pow(10,x);

		data = getRandomData((int)power);
		System.out.println("size of arg "  + data.length);
	}

	private void run() {
		appelNormal();

		if (localServerStub != null) {
			appelRMILocal();
		}

		if (distantServerStub != null) {
			appelRMIDistant();
		}
	}

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

	private void appelNormal() {
		int result = localServer.execute(4, 7);

		long start = System.nanoTime();
		result=localServer.execute(data);
		long end = System.nanoTime();

		System.out.println("Temps écoulé appel normal: " + (end - start)
				+ " ns");
		System.out.println("Résultat appel normal: " + result);
	}

	private void appelRMILocal() {
		try {
			int result = localServerStub.execute(4, 7);

			long start = System.nanoTime();
			result=localServerStub.execute(data);
			long end = System.nanoTime();

			System.out.println("Temps écoulé appel RMI local: " + (end - start)
					+ " ns");
			System.out.println("Résultat appel RMI local: " + result);
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
	}

	private void appelRMIDistant() {
		try {
			int result = distantServerStub.execute(4, 7);

			long start = System.nanoTime();
			result=distantServerStub.execute(data);
			long end = System.nanoTime();

			System.out.println("Temps écoulé appel RMI distant: "
					+ (end - start) + " ns");
			System.out.println("Résultat appel RMI distant: " + result);
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
	}
	private byte[] getRandomData(int size){
		byte[] data = new byte[size];
		Random rand = new Random();
		rand.nextBytes(data);
		return data;
	}
}
