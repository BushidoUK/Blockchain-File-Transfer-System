/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mechanisms;

import Helpers.FTPUpload;
import blockchain.Block;
import blockchain.Blockchain;
import blockchain.Node;
import com.google.gson.Gson;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import utilities.FileEncryption;
import utilities.Hashing;

/**
 *
 * @author student
 */
@MultipartConfig
public class FileUpload extends HttpServlet {

    private File uploadFolder;
    private File uploadedFile;
    private Blockchain chain;
    
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
             throws ServletException, IOException, FileUploadException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, Exception {
     
            response.setContentType("text/html;charset=UTF-8");
            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            HttpSession session = request.getSession();
            
            uploadFolder = new File("C:\\Repo");
            //Removes the built in size limit on AES keys to avoid runtime error
            Security.setProperty("crypto.policy", "unlimited");
            
            //Deserialize blockchain so we can add new blocks to it
            byte[] encodedJson = Files.readAllBytes(Paths.get("C:\\Blockchain\\Blockchain.json"));
            String jsonStringRep = new String(encodedJson);
            
            //Maps elements within JSON string to a blockchain object
            //Class is nested but should still work as intended
            Gson gson = new Gson();
            chain = gson.fromJson(jsonStringRep, Blockchain.class);
            File jsonFile = new File("C:\\Blockchain\\Blockchain.json");
            
            String user = (String)session.getAttribute("uname");
            
            String description = request.getParameter("description");
            Part filePart = request.getPart("file");
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            InputStream fileContent = filePart.getInputStream();
            
            uploadedFile = new File(uploadFolder.getAbsolutePath() + "\\" + fileName);
            uploadedFile.createNewFile();
            
            FileUtils.copyInputStreamToFile(fileContent, uploadedFile);
            
            //Generate keys
            SecureRandom random = new SecureRandom();
            KeyPair kp = FileEncryption.getRSAKeyPair(random);
            
            PublicKey publicKey = kp.getPublic();
            byte[] publicKeyBytes = publicKey.getEncoded();
            String publicKey64 = Base64.encodeBase64String(publicKeyBytes);
            
            //Create file shards
            int startIndex = chain.getTailBlock().getIndex() + 1;
            
            //Sets up a byte array that will provide the buffer 
            //Doing it this way creates file shards and prevents whole files being loaded into heap memory at once
            int fileSize = 1024 * 1024;
            byte[] buffer = new byte[fileSize];
            
            FileInputStream fileInput = new FileInputStream(uploadedFile);
            BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);
            
            int bytesAmount = 0;
            
            while ((bytesAmount = bufferedInput.read(buffer)) > 0) {
                String shardName = "index" + startIndex + ".dat";
                File shardFile = new File(uploadFolder.getAbsolutePath() + "\\" + shardName);
                Boolean start = shardFile.createNewFile();
                
                //Only executes if the DAT file that will contain the shard has been created
                if(start) {
                    
                    SecretKey aesKey = FileEncryption.getAESKey();
                    byte[] unencAesKey = aesKey.getEncoded();
                    
                    byte[] encAesKey = FileEncryption.encryptFileRSA(unencAesKey, publicKey);
                    
                    String aesKey64 = Base64.encodeBase64String(unencAesKey);
                    
                    byte[] iv = FileEncryption.generateInitialisationVector();
                    byte[] encIv = FileEncryption.encryptFileRSA(iv, publicKey);
                    String iv64 = Base64.encodeBase64String(iv);
                    
                    IvParameterSpec ivSpec = new IvParameterSpec(iv);
                    byte[] data = FileEncryption.dataCryptography(buffer, Cipher.ENCRYPT_MODE, aesKey, ivSpec);
                    String data64 = Base64.encodeBase64String(data);
                    
                    FileOutputStream fileOutput = new FileOutputStream(shardFile);
                    fileOutput.write(data64.getBytes(), 0, bytesAmount);
                    
                    Block newBlock = new Block(startIndex, user, description, Hashing.createHash(data64), publicKey64, aesKey64, iv64, chain.getTailBlock().getHash());
                    chain.addBlock(newBlock);
                    
                    if(!chain.getNodes().isEmpty()) {
                    for(Node node : chain.getNodes()) {
                        FTPUpload.ftpUpload(node, shardFile, shardName, 21);
                    }}
                    
                    startIndex++;
                }
            }
            
            Gson endGson = new Gson();
            String updatedBlockchainJson = endGson.toJson(chain);
            byte[] encodedUpdateChain = updatedBlockchainJson.getBytes();
            FileUtils.writeByteArrayToFile(jsonFile, encodedUpdateChain, false);
            
            PrivateKey privateKey = kp.getPrivate();
            byte[] privateKeyBytes = privateKey.getEncoded();
            String privateKeyb64 = Base64.encodeBase64String(privateKeyBytes);
            
            // out.println("<div style=\"height:120px;width:120px;border:1px solid #ccc;font:16px/26px Georgia, Garamond, Serif;overflow:auto;\">\n" + privateKeyb64 + "</div>");
            
//            out.println("<html>");
//            out.println("<head>");
//            out.println("<script type=\"text/javascript\">");
//            out.println("function copyKeyFunction() {");
//            out.println("/* Get the text element */");
//            out.println("var keyCopy = document.getElementbyId(\"pkeytext\");");
//            out.println("keyCopy.select();");
//            out.println("/* Copy text from inside the selected text field*/");
//            out.println("document.execCommand(\"Copy\");");
//            out.println("/* Notify user that key has been copied */");
//            out.println("alert(\"Private key has been copied to clipboard, please save otherwise you will lose access to your files. \");");
//            out.println("}");
//            out.println("</script>");
//            out.println("</head>");
//            out.println("<body>");
//            out.println("<h3>Please copy and keep the private key below, otherwise you will not be able to retrieve your file later.</h3>");
//            out.println("<br><br><br>");
//           
//            out.println("<input type=\"text\" value=\"" + privateKeyb64);
//            out.print("\" id=\"pkeytext\">");
//            out.println("<button onclick=\"copyKeyFunction()\">Copy private key to clipboard</button>");
//            out.println("</body>");
//            out.println("</html>");
            
            PrintWriter out = response.getWriter();
            
            out.println("<h3>Please copy and keep the private key below, otherwise you will not be able to retrieve your file later.</h3>");
            out.println("<br><br><br>");
            out.println("<input type=\"text\" value=\"" + privateKeyb64);
            out.print("\" id=\"pkeytext\">");
            out.println("<a href=\"http://localhost:8080/BCFT_v3/WelcomeUser.jsp?name=\" class=\"button\">\"  Go back to Main Menu  </a>");
            
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
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidAlgorithmParameterException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
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
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidAlgorithmParameterException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";}}
    // </editor-fold>

