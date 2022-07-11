package chatserver;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * A multithreaded chat room server.  When a client connects the
 * server requests a screen name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name.  The
 * broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple
 * chat server, there are a few features that have been left out.
 * Two are very useful and belong in production code:
 *
 *     1. The protocol should be enhanced so that the client can
 *        send clean disconnect messages to the server.
 *
 *     2. The server should do some logging.
 */
public class ChatServer {
    // TASK 6: For thread security
    private static final Object stateLock = new Object();

    // TASK 7: List of writers alongside there usernames
    private static HashMap<String, PrintWriter> writersWithNames = new HashMap<>();

    /**
     * The port that the server listens on.
     */
    private static final int PORT = 9001;

    /**
     * The set of all names of clients in the chat room.  Maintained
     * so that we can check that new clients are not registering name
     * already in use.
     */
    private static HashSet<String> names = new HashSet<String>();

    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages.
     */
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

    /**
     * The appplication main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                Socket socket  = listener.accept();
                Thread handlerThread = new Thread(new Handler(socket));
                handlerThread.start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    private static class Handler implements Runnable {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {
                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }

                    // In order to avoid the race conditions we must adhere a lock.
                    // Therefore, stateLock is defined and lock is implemented
                    // the shared variable 'names'
                    synchronized (stateLock) {
                        if (!names.contains(name)) {
                            names.add(name);
                            break;
                        }
                    }
                }

                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                out.println("NAMEACCEPTED");
                writers.add(out);

                // TASK 7: Storing socket + name for private communication
                writersWithNames.put(name, out);

                // TODO: You may have to add some code here to broadcast all clients the new
                // client's name for the task 9 on the lab sheet.
                // Update own client list
                writersWithNames.forEach((clientName, clientWriter) -> {
                    if(clientName != name){
                        out.println("ENTERCLIENT" + clientName);
                    }
                });
                // Update others client list and notify
                writersWithNames.forEach((clientName, clientWriter) -> {
                    // Notify others new client has joined
                    if(clientName != name){
                        clientWriter.println("MESSAGE " + name + " has joined the Chat!");
                        clientWriter.println("ENTERCLIENT" + name);
                    }
                });


                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }

                    /** TASK 7
                     * First we check whether the input contains >> characters
                     * For instance sunil>>hi means sunil = username and hi = message
                     * Therefore by considering >> as a delimiter we extract username from the input
                     * Then check whether it is a valid username in the names list
                     * IF valid then we obtain the client's socket from socketList hashmap
                            * Then get the client's print writer
                            * Then forward the message to the receiver and the sender(self)
                     * ELSE
                            * Print a message in the senders window(self) that user is not found
                     */
                    if(input.contains(">>")) {
                        String receiverName = input.substring(0, input.indexOf(">>"));
                        System.out.println("Private message to --> " + receiverName);

                        // Check whether client with the name exists or not
                        if(names.contains(receiverName)){
                            // If client found forward the message to that client
                            System.out.println("Client found");

                            PrintWriter receiverWriter = writersWithNames.get(receiverName);

                            // Add message to the receiver
                            receiverWriter.println("MESSAGE " + " [Private]" + ": " + input);
                            // Add message to the self
                            out.println("MESSAGE " + " [Private]" + ": " + input);
                        } else {
                            // If client not found print client not found in the senders view
                            System.out.println("Client not exists");

                            // Add message to the self
                            out.println("MESSAGE " + receiverName + " is not found");
                        }
                    }
                    else {
                        // Send message to all the clients
                        for (PrintWriter writer : writers) {
                            writer.println("MESSAGE " + name + ": " + input);
                        }
                    }
                }
            }
            catch (IOException e) {
                // TASK 8
                // Update others client list and notify
                writersWithNames.forEach((clientName, clientWriter) -> {
                    // Notify others new client has joined
                    if(clientName != name){
                        clientWriter.println("MESSAGE " + name + " has leave the Chat!");
                        clientWriter.println("LEAVECLIENT" + name);
                    }
                });

                try {
                    in.close();
                    out.close();
                } catch (IOException ex) {
                    ex.getStackTrace();
                }
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if (name != null) {
                    names.remove(name);

                    // remove client from the name+writer hash map
                    writersWithNames.remove(name);
                }
                if (out != null) {
                    writers.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}