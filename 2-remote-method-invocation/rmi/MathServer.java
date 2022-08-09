import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class MathServer extends UnicastRemoteObject implements MathService {
    // TODO: Add a private variable to keep the client count
    public MathServer() throws RemoteException{
        super();
    }

    // TODO: add a method to increment the cleint count. Make it thread safe

    public int add(int a, int b) throws RemoteException {
        System.out.println("Adding " + a + " and " + b + " in the server");
        return a+b;
    }

    public int subtract(int a, int b) throws RemoteException {
        System.out.println("Subtracting " + a + " and " + b + " in the server");
        return a-b;
    }

    public int multiply(int a, int b) throws RemoteException {
        System.out.println("Multiplying " + a + " and " + b + " in the server");
        return a*b;
    }

    public int divide(int a, int b) throws RemoteException {
        // Uncomment this to observer the client get blocked
        for (double i = 0; i < 10000000000000000.0; i++){
            System.out.println("I'm doing something that takes a long time");
        }

        if(b != 0) {
            System.out.println("Dividing " + a + " and " + b + " in the server");
            return a / b;
        } else{
            System.out.println("Can not divide by 0");
            return a;
        }
    }

    // This method is not remotely accessible as its not in the remote interface
    public int test(int a){
        System.out.println("This is a test");
        return 0;
    }

    public static void main(String[] args) {
        // Set the policy file as the system security policy
        System.setProperty("java.security.policy", "file:allowall.policy");

        try {
            MathServer svr = new MathServer();

            // Bind the remove object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("CalculatorService", svr);

            System.out.println("Service started... ");
        } catch (RemoteException re){
            System.err.println(re.getMessage());
        } catch (AlreadyBoundException abe){
            System.err.println(abe.getMessage());
        }
    }
}
