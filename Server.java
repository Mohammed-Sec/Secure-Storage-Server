package com.mycompany.mavenproject5;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {

    //user database 
    private static final Map<String, Map<String, Object>> userDatabase = new ConcurrentHashMap<>();
    
    //server vault
    private static final Map<String, Map<String, Object>> fileDatabase = new ConcurrentHashMap<>();
    
    private static final Map<String, Map<String, Object>> shareDatabase = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Server running on port 5000...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                
                Map<String, Object> request = (Map<String, Object>) in.readObject();
                String action = (String) request.get("ACTION");
                Map<String, Object> response = new HashMap<>();

                System.out.println("Received Action: " + action);

                switch (action) {
                    case "REGISTER": {
                        String user = (String) request.get("user");
                        String pass = (String) request.get("pass");
                        byte[] pubKey = (byte[]) request.get("publicKey");

                        if (userDatabase.containsKey(user)) {
                            response.put("STATUS", "FAIL");
                            response.put("MSG", "User already exists!");
                        } else {
                           
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("passwordHash", pass);
                            userData.put("publicKey", pubKey);
                            userDatabase.put(user, userData);
                            response.put("STATUS", "OK");
                        }
                        break;
                    }
                    case "LOGIN": {
                        String user = (String) request.get("user");
                        String pass = (String) request.get("pass");
                        Map<String, Object> userData = userDatabase.get(user);
                        if (userData != null && userData.get("passwordHash").equals(pass)) {
                            response.put("STATUS", "OK");
                        } else {
                            response.put("STATUS", "FAIL");
                        }
                        break;
                    }
                    case "UPLOAD": {
                        String fileId = UUID.randomUUID().toString();
                        Map<String, Object> fileData = new HashMap<>();
                        fileData.put("owner", request.get("user"));
                        fileData.put("fileName", request.get("fileName"));
                        fileData.put("blob", request.get("blob"));
                        fileData.put("hmac", request.get("hmac"));
                        fileData.put("rsaWrappedKey", request.get("rsaWrappedKey")); 
                        
                        fileDatabase.put(fileId, fileData);
                        response.put("STATUS", "OK");
                        response.put("fileId", fileId);
                        System.out.println("Blob uploaded securely. File ID: " + fileId);
                        break;
                    }
                    case "DOWNLOAD": {
                        String fileId = (String) request.get("fileId");
                        Map<String, Object> fileData = fileDatabase.get(fileId);
                        if (fileData != null) {
                            response.put("STATUS", "OK");
                            response.put("fileData", fileData);
                        } else {
                            response.put("STATUS", "FAIL");
                            response.put("MSG", "Download failed: File not found.");
                        }
                        break;
                    }
                    case "GET_PUBLIC_KEY": {
                        String targetUser = (String) request.get("targetUser");
                        Map<String, Object> userData = userDatabase.get(targetUser);
                        if (userData != null) {
                            response.put("STATUS", "OK");
                            response.put("publicKey", userData.get("publicKey"));
                        } else {
                            response.put("STATUS", "FAIL");
                            response.put("MSG", "User does not exist.");
                        }
                        break;
                    }
                    case "SHARE": {
                        String shareId = UUID.randomUUID().toString().substring(0, 8); 
                        Map<String, Object> shareData = new HashMap<>();
                        shareData.put("fileId", request.get("fileId"));
                        shareData.put("targetUser", request.get("targetUser"));
                        shareData.put("rsaWrappedKey", request.get("friendWrappedKey")); 
                        
                        shareDatabase.put(shareId, shareData);
                        response.put("STATUS", "OK");
                        response.put("shareId", shareId);
                        break;
                    }
                    case "DOWNLOAD_SHARED": {
                        String shareId = (String) request.get("shareId");
                        Map<String, Object> shareData = shareDatabase.get(shareId);
                        if (shareData != null) {
                            String fileId = (String) shareData.get("fileId");
                            Map<String, Object> fileData = fileDatabase.get(fileId);
                            
                            response.put("STATUS", "OK");
                            response.put("blob", fileData.get("blob"));
                            response.put("hmac", fileData.get("hmac"));
                            response.put("fileName", fileData.get("fileName"));
                            response.put("rsaWrappedKey", shareData.get("rsaWrappedKey")); 
                        } else {
                            response.put("STATUS", "FAIL");
                        }
                        break;
                    }
                }
                
                out.writeObject(response);
                out.flush();

            } catch (Exception e) {
                System.out.println("Client disconnected or error: " + e.getMessage());
            }
        }
    }
}