package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import server.AES;

public class ServerServiceIn extends Thread {

    private DataInputStream serverIn;
    private DataInputStream clientIn;
    private DataOutputStream serverOut;
    private DataOutputStream clientOut;

    private AES aes;

    public ServerServiceIn(DataInputStream serverIn, DataInputStream clientIn, DataOutputStream serverOut, DataOutputStream clientOut, String pass) throws Exception {
        this.serverIn = serverIn;
        this.serverOut = serverOut;
        this.clientIn = clientIn;
        this.clientOut = clientOut;
        aes = new AES(pass);
    }

    public void run() {
        while (true) {
            try {
                byte[] buffer = new byte[65535];
                int len = clientIn.read(buffer);
                if (len < 0) {
                    break;
                }
                byte[] msg = new byte[len];
                for (int i = 0; i < len; i++) {
                    msg[i] = buffer[i];
                }
                serverOut.write(aes.decrypt(msg));
            } catch (Exception ex) {
                try {
                    //ex.printStackTrace();
                    serverIn.close();
                    serverOut.close();
                    clientIn.close();
                    clientOut.close();
                } catch (IOException ex1) {
                    //Logger.getLogger(ServerServiceIn.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }

        }
    }

}
