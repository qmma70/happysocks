/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author mayu
 */
public class Server {

    /**
     * @param args the command line arguments
     */
    static private String password = "123456!";

    public static void main(String[] args) throws Exception {
        // TODO code application logic here
        /*System.out.println("OK!");
         AES aes=new AES(password);
         byte[] pt="Hello Cipher!".getBytes();
         System.out.println(new String(aes.encrypt(pt)));
         System.out.println(new String(aes.decrypt(aes.encrypt(pt))));*/
        int port = 8899;

        ServerSocket server = new ServerSocket(port);

        while (true) {
            //System.out.println("Server listening...");
            Socket socket = server.accept();
            
            TCPForwarding service=new TCPForwarding(socket,password);
            service.start();
        }

    }

}
