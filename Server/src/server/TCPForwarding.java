/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mayu
 */
public class TCPForwarding extends Thread {

    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF
                | (b[2] & 0xFF) << 8
                | (b[1] & 0xFF) << 16
                | (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[]{
            (byte) ((a >> 24) & 0xFF),
            (byte) ((a >> 16) & 0xFF),
            (byte) ((a >> 8) & 0xFF),
            (byte) (a & 0xFF)
        };
    }
    private DataInputStream in;
    private DataOutputStream out;
    private Socket sock;
    private AES aes;
    private String pass;

    public TCPForwarding(Socket socket, String password) throws Exception {
        sock = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        aes = new AES(password);
        pass=password;
    }

    public void run() {
        try {
            System.out.print("client command: ");
            int addrLen = in.readInt();
            Byte atyp=in.readByte();
            byte cmd = in.readByte();
            byte[] dstaddr = new byte[addrLen];
            int index = 0;
            for (; index < addrLen; index++) {
                dstaddr[index] = in.readByte();
            }
            dstaddr = aes.decrypt(dstaddr);
            byte[] dstPortArray = new byte[4];
            dstPortArray[0] = in.readByte();
            dstPortArray[1] = in.readByte();
            dstPortArray[2] = in.readByte();
            dstPortArray[3] = in.readByte();
            dstPortArray = aes.decrypt(dstPortArray);
            int dstPort = byteArrayToInt(dstPortArray);
            

            System.out.println("addrLen=" + addrLen +", addrType="+atyp+ ", command=" + cmd + ", dstPort=" + dstPort);
            String dst;
            if(cmd==0){
                if(atyp==3) dst=new String(dstaddr);
                else dst=InetAddress.getByAddress(dstaddr).toString();
                Socket s = new Socket(dst, dstPort);
                DataInputStream remoteInput
                        = new DataInputStream(s.getInputStream());
                DataOutputStream remoteOutput
                        = new DataOutputStream(s.getOutputStream());
                System.out.println("connected to "+dst);
                out.writeByte(0);
                //System.out.println("a reply (0) has been sent to client. Start forwarding...");
                ServerServiceIn ssin=new ServerServiceIn(remoteInput,in,remoteOutput,out,pass);
                ServerServiceReply ssrp=new ServerServiceReply(remoteInput,in,remoteOutput,out,pass);
                ssin.start();
                ssrp.start();
            }
        } catch (Exception ex) {
            //ex.printStackTrace();
            
        } 

    }

}
