package ca.polymtl.inf4410.tp1.server;

import java.io.*;
import java.util.*;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import ca.polymtl.inf4410.tp1.shared.ServerInterface;

public class Server implements ServerInterface {
	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}

	public Server() {
		super();
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject
				.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err
				.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

	/*
	 * Méthode accessible par RMI. Additionne les deux nombres passés en
	 * paramètre.
	 */
	@Override
	public int execute(int a, int b) throws RemoteException {
		return a + b;
	}
	@Override
	public int execute(byte[] b) throws RemoteException {
		return b.length;
	}

	
	public byte[] generateClientId() throws RemoteException {
		String uniqueID = UUID.randomUUID().toString();
		System.out.println("New client id "+uniqueID);
		return uniqueID.getBytes();
	}

	@Override
	public void create(String fileName) throws RemoteException {
		String currentDir = Paths.get("").toAbsolutePath().toString();
		File file = new File(currentDir+"/"+fileName);
		Boolean bool;

		if(!file.exists()) {
			try{
				bool = file.createNewFile();
				System.out.println("File created: "+bool);
			}catch(IOException e) {
				e.printStackTrace();
			}

		}


	}

	@Override
	public ArrayList<String> list() throws RemoteException {
		ArrayList<String> fileNames=new ArrayList<String>();
		String currentDir = Paths.get("").toAbsolutePath().toString();
		File folder = new File(currentDir+"/");
		File[] listOfFiles = folder.listFiles();
		for (File file : listOfFiles) {
			if (file.isFile()) {
				fileNames.add(file.getName());
			}
		}
		return fileNames;


	}
}
