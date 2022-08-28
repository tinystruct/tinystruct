package org.tinystruct.system;

import org.tinystruct.application.Context;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteDispatcher extends Remote {
    Object execute(String command, Context context) throws RemoteException;

    void install(Configuration<String> config, List<String> list) throws RemoteException;
}
