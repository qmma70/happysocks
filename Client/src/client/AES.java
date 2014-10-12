/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client;

import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


/**
 *
 * @author Qiming Ma
 */
public class AES {
    private byte[] hash;
    private SecretKeySpec key;
    private Cipher cipher;
    private IvParameterSpec ivSpec;
    public AES(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        hash = digest.digest(password.getBytes("UTF-8"));
        key = new SecretKeySpec(Arrays.copyOfRange(hash,0,16), "AES");
        ivSpec = new IvParameterSpec(Arrays.copyOfRange(hash,16,32));
        cipher = Cipher.getInstance("AES/CFB/NoPadding");
 
    }
    public byte[] encrypt(byte[] pt) throws Exception {
        cipher.init(Cipher.ENCRYPT_MODE, key,ivSpec);

        byte[] cipherText = new byte[cipher.getOutputSize(pt.length)];
        int ctLength = cipher.update(pt, 0, pt.length, cipherText, 0);
        ctLength += cipher.doFinal(cipherText, ctLength);
        
        return cipherText;
    }
    public byte[] decrypt(byte[] ct) throws Exception {
        cipher.init(Cipher.DECRYPT_MODE, key,ivSpec);
        byte[] plainText = new byte[cipher.getOutputSize(ct.length)];
        int ptLength = cipher.update(ct, 0, ct.length, plainText, 0);
        ptLength += cipher.doFinal(plainText, ptLength);
        
        return plainText;
    }

}
