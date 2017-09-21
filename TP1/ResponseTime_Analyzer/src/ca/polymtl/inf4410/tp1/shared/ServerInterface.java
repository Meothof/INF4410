package ca.polymtl.inf4410.tp1.shared;
import java.util.Arrays;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;

public interface ServerInterface extends Remote {
	int execute(int a, int b) throws RemoteException;
	int execute(byte[] b) throws RemoteException;
	void create(String fileName) throws RemoteException;
	ArrayList<String> list() throws RemoteException;
}
