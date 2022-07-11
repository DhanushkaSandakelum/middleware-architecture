package chatserver;


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text field for entering messages and a
 * textarea to see the whole dialog.
 *
 * The client follows the Chat Protocol which is as follows.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired screen name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with "MESSAGE " then all characters following
 * this string should be displayed in its message area.
 */
public class ChatClient {

    BufferedReader in;
    PrintWriter out;

    JFrame frame = new JFrame("Chatter");
    Container container;

    JLabel clientListTitle = new JLabel("Clients");
    JTextField textField = new JTextField(40);

    JTextArea messageArea = new JTextArea(8, 40);

    JCheckBox checkBox = new JCheckBox("Broadcast");

    // TASK 8: List box for GUI
    DefaultListModel<String> model = new DefaultListModel<>();
    JList listBox = new JList(model);

    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Return in the
     * listener sends the textfield contents to the server.  Note
     * however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED
     * message from the server.
     */
    public ChatClient() {

        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.setBounds(0, 0, 700, 400);
        frame.setResizable(false);
        container = frame.getContentPane();
        container.setLayout(null);

        textField.setBounds(5, 5, 400, 30);
        container.add(textField);

        messageArea.setBounds(5, 40, 400, 370);
        container.add(messageArea);

        clientListTitle.setBounds(420,5,50, 20);
        container.add(clientListTitle);

        listBox.setBounds(420,25,300, 370);
        container.add(listBox);

        checkBox.setBounds(600,5,250, 20);
        container.add(checkBox);
//        frame.getContentPane().add(textField, "North");
//        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
//        frame.getContentPane().add(checkBox, "South");
//        frame.getContentPane().add(new JScrollPane(listBox), "South");


        // Listing for text field input
        textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending
             * the contents of the text field to the server.    Then clear
             * the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });

        // listing for list box selection
        listBox.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if(!e.getValueIsAdjusting()){
                    if(listBox.getSelectedValue() != null)
                        textField.setText(listBox.getSelectedValue().toString() + ">>");
                    checkBox.setSelected(false);
                }
            }
        });

        // Listing to check box
        checkBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED){
                    textField.setText("");
                    listBox.clearSelection();
                    checkBox.setSelected(true);
                }
            }
        });
    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
                frame,
                "Enter IP Address of the Server:",
                "Welcome to the Chatter",
                JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
        return JOptionPane.showInputDialog(
                frame,
                "Choose a screen name:",
                "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {

        // Make connection and initialize streams
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.

        // TODO: You may have to extend this protocol to achieve task 9 in the lab sheet
        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
                out.println(getName());
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
            } else if (line.startsWith("ENTERCLIENT")) {
                // TASK 8: Add users to the listBox
                model.addElement(line.substring(11));
            }else if (line.startsWith("LEAVECLIENT")) {
                // TASK 8: Add users to the listBox
                model.removeElement(line.substring(11));
            }
        }
    }

    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}