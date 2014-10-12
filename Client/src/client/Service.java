/*
 * The MIT License
 *
 * Copyright 2014 mayu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package client;

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
public class Service extends Thread {

    int port;
    int serverPort;
    String serverAddress;
    String password;
    AES aes;
    Socket socket;

    public Service(int port, int sPort, String sAddr, String pass, Socket sock) throws Exception {
        this.port = port;
        this.serverPort = sPort;
        this.serverAddress = sAddr;
        this.password = pass;
        this.socket = sock;
        aes = new AES(password);
    }

    public void run() {

        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            byte buf = 0;
            buf = in.readByte();
            int version = buf;
            if (version == 0x05) {
                //System.out.print("[Socks Packet 1]  Version = 5, ");
            } else if (buf == 0x04) {
                //System.out.print("[Socks Packet 1]  Version = 4, ");
                return;
                
            }
            buf = in.readByte();
            int nMethods = buf;
            if (nMethods > 255) {
                // error: nMethods too large
                return;
            }
            //System.out.print("Num Methods = " + nMethods + ", Methods = {");
            int index = 0;
            int ifNoAuthSupported = 0;
            for (; index < nMethods; index++) {
                buf = in.readByte();
                if (buf == 0) {
                    ifNoAuthSupported = 1;
                }
                if (index > 0) {
                    //System.out.print(",");
                }
                //System.out.print("0x" + Integer.toHexString(buf));
            }
            //System.out.println("}");
            if (version != 5 || ifNoAuthSupported == 0) {
                System.out.println("Unsupported client version. Closing connection.");
                return;
            }
            System.out.println("[Reply Packet 1]  Version = 5, Method = 0x00(NO_AUTHENTICATION)");
            out.writeByte(5);
            out.writeByte(0);
            byte ver, cmd, rsv, atyp;
            //byte[] b=new byte[256];
            //int ret=in.read(b);
            //System.out.println((int)b[0]);
            ver = in.readByte();
            cmd = in.readByte();
            rsv = in.readByte();
            atyp = in.readByte();
            System.out.println("[Client Request]  Version = " + ver + ", Command=" + cmd + ", Address Type = " + atyp);
            if (ver != 5 || rsv != 0) {
                System.out.println("Unsupported client version. Closing connection.");
                return;
            }
            byte dstaddr[] = null;
            String dst = null;
            byte rep = 0;
            switch (atyp) {
                case 1:
                    dstaddr = new byte[4];
                    for (index = 0; index < 4; index++) {
                        dstaddr[index] = in.readByte();
                    }
                    dst = String.valueOf(dstaddr[0]) + "." + String.valueOf(dstaddr[1]) + "." + String.valueOf(dstaddr[2]) + "." + String.valueOf(dstaddr[3]);
                    System.out.println("Requested IPV4 Address:" + dst);
                    break;
                case 3:
                    int length = (int) in.readByte();
                    System.out.println("Number of Octets in DN:" + length);
                    dstaddr = new byte[length];
                    for (index = 0; index < length; index++) {
                        dstaddr[index] = in.readByte();
                    }

                    String domain = new String(dstaddr);
                    dst = domain;
                    System.out.println("Requested Domain:" + dst);
                    break;
                case 4:
                    dstaddr = new byte[16];
                    for (index = 0; index < 16; index++) {
                        dstaddr[index] = in.readByte();
                    }

                    System.out.println("IPV6 Address omitted.");
                    break;
                default:
                    rep = 8;
                    break;

            }
            int dstport = (int) in.readShort();
            System.out.println("Destination port:" + dstport);
            switch (cmd) {
                case 1:
                    Socket s = new Socket(serverAddress, serverPort);
                    DataInputStream input
                            = new DataInputStream(s.getInputStream());
                    DataOutputStream output
                            = new DataOutputStream(s.getOutputStream());
                    output.writeInt(dstaddr.length); // write length of addr
                    output.writeByte(atyp); // write addr type
                    output.writeByte(0); // write command byte 0
                    byte[] encryptedAddr = aes.encrypt(dstaddr);
                    output.write(encryptedAddr, 0, encryptedAddr.length); //write encrypted address
                    byte[] dstPortArray = intToByteArray(dstport);
                    output.write(aes.encrypt(dstPortArray), 0, 4);
                    byte reply = input.readByte();
                    if (reply == 0) {
                        System.out.println("Reply from server received, connection established.");

                    } else {
                        System.out.println("error: server unable to establish connection.");
                        rep = 3;
                        return;
                    }

                    out.writeByte(5);
                    out.writeByte(rep);
                    out.writeByte(0);
                    out.writeByte(1);
                    InetAddress myIP = InetAddress.getByName("127.0.0.1");
                    out.write(myIP.getAddress(), 0, myIP.getAddress().length);
                    out.writeShort(port);
                    System.out.println("Reply sent. Start forwarding packets...");
                    ServiceIn sin = new ServiceIn(input, in, output, out, password);
                    ServiceReply srp = new ServiceReply(input, in, output, out, password);
                    sin.start();
                    srp.start();
                    break;
                case 2:
                    break;
                case 3:
                    break;
                default:
                    rep = 7;
                    break;
            }
        } catch (IOException ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Service.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

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

}
