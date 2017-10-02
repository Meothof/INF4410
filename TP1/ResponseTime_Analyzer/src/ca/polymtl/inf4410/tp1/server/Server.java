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
import java.nio.file.Files;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.util.Arrays;
import ca.polymtl.inf4410.tp1.shared.Fichier;
import ca.polymtl.inf4410.tp1.shared.ServerInterface;
import ca.polymtl.inf4410.tp1.shared.Tools;


public class Server implements ServerInterface {

	private ArrayList<Fichier> listeFichiers;
	private Tools tools = new Tools();

	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}

	public Server() {
		super();
		listeFichiers = new ArrayList<Fichier>();
		try{
			ArrayList<String> files = list();
			for(String l : files){
				listeFichiers.add(new Fichier(l,null));
			}
		}
		catch(RemoteException e){
			System.err.println("Erreur: " + e.getMessage());
		}
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
	public int create(String fileName) throws RemoteException {
		File file = new File(Paths.get("").toAbsolutePath().toString()+"/server-files/"+fileName);
		Boolean bool;
		if(!file.exists()) {
			try{
				bool = file.createNewFile();
				System.out.println("Fichier créé : " + bool);
				listeFichiers.add(new Fichier(fileName, null));
			}
			catch(IOException e) {
				e.printStackTrace();
			}
			return 1;
		}
		return 0;
	}

	@Override
	public ArrayList<String> list() throws RemoteException {
		ArrayList<String> fileNames=new ArrayList<String>();
		File folder = new File(Paths.get("").toAbsolutePath().toString()+"/server-files/");
		File[] listOfFiles = folder.listFiles();
		for (File file : listOfFiles) {
			if (file.isFile()) {
				fileNames.add(file.getName());
			}
		}
		return fileNames;
	}

	@Override
	public byte[] get(String nom, byte[] checksum) throws RemoteException {
		try {
			Path file = Paths.get(Paths.get("").toAbsolutePath().toString() + "/server-files/" + nom);
			if (checksum == null) {
				return Files.readAllBytes(file);
			}
			else if(!Arrays.equals(tools.checksum("/server-files/"+nom),checksum)){
				return Files.readAllBytes(file);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return  null;
	}

	@java.lang.Override
	public int lock(String nom, byte[] clientid) throws RemoteException {
		for(Fichier f : listeFichiers) {
			if(f.getNom().equals(nom)) {
				if(f.getLock()!=null) {
					return 0;
				}
				else {
					f.lock(clientid);
					return 1;
				}
			}
		}
		return 0;
	}

	@Override
	public int push(String nom, byte[] content, byte[] clientid) throws RemoteException {
		for(Fichier f : listeFichiers) {
			if(f.getNom().equals(nom)) {
				if(Arrays.equals(f.getLock(), clientid)) {
					String currentDirectory= Paths.get("").toAbsolutePath().toString();
					String filePath = "/server-files/" + nom;
					File file = new File(currentDirectory + filePath);
					Path path = Paths.get(currentDirectory + filePath);
					try {
						Files.write(path, content);
					}
					catch(IOException e) {
						e.printStackTrace();
					}
					f.unlock();
					return 1;
				}
				else {
					return 0;
				}
			}
		}
		return 0;
	}

}
