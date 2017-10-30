package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by thmof on 17-10-16.
 */
public interface ServerInterface extends Remote {
    int getQ() throws RemoteException;
    int pell(int x) throws RemoteException;
    int prime(int x) throws RemoteException;
}
