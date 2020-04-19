/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mechanisms;

import Helpers.FTPDownload;
import blockchain.Block;
import blockchain.Blockchain;
import blockchain.Node;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import utilities.FileEncryption;
import utilities.Hashing;

/**
 *
 * @author ljhedges
 */
public class FileRetrieval extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, Exception {
        
        String description = request.getParameter("description");
        String filename = request.getParameter("filename");
        
        //Reconstructs private key from user input
        byte[] privateKeyInputBytes = request.getParameter("privatekey").getBytes();
        byte[] privateKeyDecoded = Base64.decodeBase64(privateKeyInputBytes);
        
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyDecoded));

        
        File blockchainFile = new File("C:\\Blockchain\\Blockchain.json");
        
        File holdingFile = new File("C:\\Repo\\" + filename);
        holdingFile.createNewFile();
        
        Gson gson = new Gson();
        String chainJson = FileUtils.readFileToString(blockchainFile, "UTF-8");
        
        Blockchain chain = gson.fromJson(chainJson, Blockchain.class);
        
        for(Block currBlock : chain.getBlockchain()) {
            if(description.equals(currBlock.getDescription())) {
                
                String filePath = "C:\\Repo\\index" + currBlock.getIndex() + ".dat";
                String remoteFileName = "\\index" + currBlock.getIndex() + ".dat";
                
                //Get AES key back
                byte[] aesEncBytes = Base64.decodeBase64(currBlock.getAesKey());
                //byte[] aesUnencBytes = FileEncryption.decryptFileRSA(aesEncBytes, privateKey);
                SecretKey aesKey = new SecretKeySpec(aesEncBytes, "AES");
                
                //Get the initialisation vector back
                byte[] ivEnc = Base64.decodeBase64(currBlock.getInitialisationVector());
                //byte[] inUnenc = FileEncryption.decryptFileRSA(ivEnc, privateKey);
                IvParameterSpec iv = new IvParameterSpec(ivEnc);
                
                Boolean gotShard = false;
                int max = chain.getNodes().size();
                int start = 0;
                
                while(!gotShard) {
                    if(start < max) {
                        Node currNode = chain.getNodes().get(start);
                        gotShard = FTPDownload.downloadFile(filePath, currNode.getUser(), currNode.getPass(), currNode.getNodeAddress(), remoteFileName); }
                    else {
                        return;
                    }
                }
                
                File downloadedFile = new File(filePath);
                String downloadedFileContents = FileUtils.readFileToString(downloadedFile);
                
                //String dfcHash = Hashing.createHash(downloadedFileContents);
                
                //if(currBlock.getData().equals(dfcHash)) {
                    byte[] encFileContents = Base64.decodeBase64(downloadedFileContents);
                    byte[] unencFileContents = FileEncryption.dataCryptography(encFileContents, Cipher.DECRYPT_MODE, aesKey, iv);
                    FileUtils.writeByteArrayToFile(holdingFile, unencFileContents, true);
                //}
            }
        }
        
        response.setContentType("application/octet-stream");
        response.setContentLength((int) holdingFile.length());
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", holdingFile.getName()));
        
        OutputStream outputStream = response.getOutputStream();
        InputStream inputStream = new FileInputStream(holdingFile);
        
        byte[] buffer = new byte[4096];
        int length;
        
        while((length = inputStream.read()) > 0) {
            outputStream.write(buffer, 0, length);
        }
        
        outputStream.flush();
        
        holdingFile.delete();
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(FileRetrieval.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(FileRetrieval.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(FileRetrieval.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(FileRetrieval.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(FileRetrieval.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(FileRetrieval.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
