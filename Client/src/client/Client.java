/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Qiming Ma ma70@purdue.edu
 */
public class Client extends Thread {

    /**
     * @param args the command line arguments
     */
    private static AES aes;

    public static void main(String args[]) throws Exception {

        int port = 12321;
        int serverPort = 8899;
        String serverAddress = "127.0.0.1";
        String password = "123456!";
        ServerSocket server = new ServerSocket(port);
        while (true) {
            Socket socket = server.accept();
            Service s = new Service(port, serverPort, serverAddress, password, socket);
            s.start();
        }

    }
}
