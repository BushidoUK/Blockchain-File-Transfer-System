/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Helpers;

import blockchain.Node;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

/**
 *
 * @author student
 */
public abstract class FTPUpload {
    
    public static Boolean ftpUpload(Node node, File file, String name, int port) throws IOException {
        String address = node.getNodeAddress();
        String user = node.getUser();
        String pass = node.getPass();
        
        FTPClient ftpClient = new FTPClient();
        
        ftpClient.connect(address, port);
        ftpClient.login(user, pass);
        ftpClient.enterLocalPassiveMode();
        
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        
        InputStream is = new FileInputStream(file);
        
        OutputStream os = ftpClient.storeFileStream(name);
        byte[] bytesIn = new byte[4096];
        int read = 0;
        
        while ((read = is.read(bytesIn)) != -1) {
            os.write(bytesIn, 0, read);
        }
        
        is.close();
        os.close();
        
        boolean status = ftpClient.completePendingCommand();
        
        if(ftpClient.isConnected()) {
            ftpClient.logout();
            ftpClient.disconnect();
        }
        return status;
        
    }
}
