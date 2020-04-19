/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Helpers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
/**
 *
 * @author ljhedges
 */
public abstract class FTPDownload {
       
    public static Boolean downloadFile(String targetPath, String user, String pass, String address, String filename) throws IOException {
       int port = 21;
       
       FTPClient ftpClient = new FTPClient();
       
       ftpClient.connect(address, port);
       ftpClient.login(user, pass);
       ftpClient.enterLocalPassiveMode();
       
       ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
       
       File targetFile = new File(targetPath);
       OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(targetFile));
       InputStream inputStream = ftpClient.retrieveFileStream("\\" + filename);
       
       byte[] buffer = new byte[1024];
       int bytesRead = -1;
       
       while((bytesRead = inputStream.read(buffer)) != -1) {
           outputStream.write(buffer, 0, bytesRead);
       }
       
       Boolean status = ftpClient.completePendingCommand();
       return status;
    }
    
}
