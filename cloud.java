package com.mycompany.mavenproject5;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class cloud extends Application {

    private String currentPass;
    private String currentUser;
    private PrivateKey myPrivateKey;
    private PublicKey myPublicKey;

    private final String SERVER_IP = "127.0.0.1";
    private final int SERVER_PORT = 5000;

    private final String bgColor = "-fx-background-color: #1e1e2e;";
    private final String titleStyle = "-fx-text-fill: #89b4fa; -fx-font-size: 28px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', sans-serif;";
    private final String labelStyle = "-fx-text-fill: #bac2de; -fx-font-size: 14px; -fx-font-family: 'Segoe UI', sans-serif;";
    private final String fieldStyle = "-fx-background-color: #313244; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10px; -fx-font-size: 14px;";
    private final String buttonStyle = "-fx-background-color: #89b4fa; -fx-text-fill: #11111b; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 10px 20px; -fx-cursor: hand;";
    private final String buttonHoverStyle = "-fx-background-color: #74c7ec; -fx-text-fill: #11111b; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 10px 20px; -fx-cursor: hand;";
    private final String dangerButtonStyle = "-fx-background-color: #f38ba8; -fx-text-fill: #11111b; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 10px 20px; -fx-cursor: hand;";
    private final String dangerButtonHoverStyle = "-fx-background-color: #eba0ac; -fx-text-fill: #11111b; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 10px 20px; -fx-cursor: hand;";
    private final String textAreaStyle = "-fx-control-inner-background: #313244; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 5px; -fx-font-family: 'Consolas', monospace;";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Cloud");
        primaryStage.setScene(loginScene(primaryStage));
        primaryStage.show();
    }

    private void applyButtonHover(Button btn, boolean isDanger) {
        btn.setStyle(isDanger ? dangerButtonStyle : buttonStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(isDanger ? dangerButtonHoverStyle : buttonHoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(isDanger ? dangerButtonStyle : buttonStyle));
    }

    private Map<String, Object> sendToServer(Map<String, Object> request) throws Exception {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            out.writeObject(request);
            out.flush();
            return (Map<String, Object>) in.readObject();
        }
    }

    private KeyPair generateRSAKeys() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    private byte[] generateHMAC(File file, byte[] hmacKeyBytes) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(hmacKeyBytes, "HmacSHA256"));
        try (FileInputStream fileStream = new FileInputStream(file)) {
            byte[] chunk = new byte[4096];
            int byteNumber;
            while ((byteNumber = fileStream.read(chunk)) != -1) {
                mac.update(chunk, 0, byteNumber);
            }
        }
        return mac.doFinal();
    }

    private Scene loginScene(Stage primaryStage) {
        VBox loginLayout = new VBox(15);
        loginLayout.setAlignment(Pos.CENTER);
        loginLayout.setStyle(bgColor);
        loginLayout.setPadding(new Insets(40));

        Label titleLabel = new Label("Cloud");
        titleLabel.setStyle(titleStyle);

        VBox formBox = new VBox(10);
        formBox.setAlignment(Pos.CENTER);
        formBox.setMaxWidth(250);

        Label userLabel = new Label("Username");
        userLabel.setStyle(labelStyle);
        TextField userField = new TextField();
        userField.setStyle(fieldStyle);

        Label passLabel = new Label("Password");
        passLabel.setStyle(labelStyle);
        PasswordField passField = new PasswordField();
        passField.setStyle(fieldStyle);

        formBox.getChildren().addAll(userLabel, userField, passLabel, passField);

        Button loginButton = new Button("Login");
        applyButtonHover(loginButton, false);
        loginButton.setMaxWidth(250);

        Button goToRegisterButton = new Button("Create New Account");
        goToRegisterButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #89b4fa; -fx-cursor: hand; -fx-underline: true;");

        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: #f38ba8; -fx-font-size: 13px;");

        loginLayout.getChildren().addAll(titleLabel, formBox, loginButton, goToRegisterButton, messageLabel);

        loginButton.setOnAction(e -> {
            try {
                if (myPrivateKey == null) {
                    messageLabel.setText("Register first to generate RSA keys for this session.");
                    return;
                }
                Map<String, Object> req = new HashMap<>();
                req.put("ACTION", "LOGIN");
                req.put("user", userField.getText());
                req.put("pass", passField.getText());
                
                Map<String, Object> res = sendToServer(req);
                if ("OK".equals(res.get("STATUS"))) {
                    currentUser = userField.getText();
                    currentPass = passField.getText();
                    primaryStage.setScene(mainDashboardScene(primaryStage));
                } else {
                    messageLabel.setText("Incorrect username or password.");
                }
            } catch (Exception ex) {
                messageLabel.setText("Server offline.");
            }
        });

        goToRegisterButton.setOnAction(e -> primaryStage.setScene(registerScene(primaryStage)));

        return new Scene(loginLayout, 800, 600);
    }

    private Scene registerScene(Stage primaryStage) {
        VBox registerLayout = new VBox(15);
        registerLayout.setAlignment(Pos.CENTER);
        registerLayout.setStyle(bgColor);
        registerLayout.setPadding(new Insets(40));

        Label titleLabel = new Label("Register");
        titleLabel.setStyle(titleStyle);

        VBox formBox = new VBox(10);
        formBox.setAlignment(Pos.CENTER);
        formBox.setMaxWidth(250);

        Label userLabel = new Label("New Username");
        userLabel.setStyle(labelStyle);
        TextField userField = new TextField();
        userField.setStyle(fieldStyle);

        Label passLabel = new Label("New Password");
        passLabel.setStyle(labelStyle);
        PasswordField passField = new PasswordField();
        passField.setStyle(fieldStyle);

        formBox.getChildren().addAll(userLabel, userField, passLabel, passField);

        Button registerButton = new Button("Register");
        applyButtonHover(registerButton, false);
        registerButton.setMaxWidth(250);

        Button backButton = new Button("Back to Login");
        backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a6adc8; -fx-cursor: hand; -fx-underline: true;");

        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);

        registerLayout.getChildren().addAll(titleLabel, formBox, registerButton, backButton, messageLabel);

        registerButton.setOnAction(e -> {
            String username = userField.getText();
            String password = passField.getText();

            if (!checkStrongPassword(password)) {
                messageLabel.setStyle("-fx-text-fill: #f38ba8;");
                messageLabel.setText("Password must be at least 8 chars and contain a symbol & uppercase letter.");
            } else {
                try {
                    KeyPair pair = generateRSAKeys();
                    myPrivateKey = pair.getPrivate();
                    myPublicKey = pair.getPublic();

                    Map<String, Object> req = new HashMap<>();
                    req.put("ACTION", "REGISTER");
                    req.put("user", username);
                    req.put("pass", password);
                    req.put("publicKey", myPublicKey.getEncoded());

                    Map<String, Object> res = sendToServer(req);
                    if ("OK".equals(res.get("STATUS"))) {
                        messageLabel.setStyle("-fx-text-fill: #a6e3a1;");
                        messageLabel.setText("Registration successful!");
                    } else {
                        messageLabel.setStyle("-fx-text-fill: #f38ba8;");
                        messageLabel.setText("Username taken.");
                    }
                } catch (Exception ex) {
                    messageLabel.setStyle("-fx-text-fill: #f38ba8;");
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        backButton.setOnAction(e -> primaryStage.setScene(loginScene(primaryStage)));

        return new Scene(registerLayout, 800, 600);
    }

    private Scene mainDashboardScene(Stage primaryStage) {
        VBox mainLayout = new VBox(25);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle(bgColor);
        mainLayout.setPadding(new Insets(30));

        Label welcomeLabel = new Label("Welcome to Cloud, " + currentUser);
        welcomeLabel.setStyle(titleStyle);

        VBox topSection = new VBox(10);
        topSection.setAlignment(Pos.CENTER);

        Label listTitle = new Label("My Uploaded Files IDs:");
        listTitle.setStyle(labelStyle);

        HBox uploadBox = new HBox(15);
        uploadBox.setAlignment(Pos.CENTER);

        TextArea uploadedFilesList = new TextArea();
        uploadedFilesList.setPrefHeight(100);
        uploadedFilesList.setPrefWidth(350);
        uploadedFilesList.setStyle(textAreaStyle);
        uploadedFilesList.setEditable(false);

        Button uploadButton = new Button("Upload File");
        applyButtonHover(uploadButton, false);
        uploadButton.setPrefHeight(100);

        uploadBox.getChildren().addAll(uploadedFilesList, uploadButton);
        topSection.getChildren().addAll(listTitle, uploadBox);

        VBox actionCard = new VBox(15);
        actionCard.setAlignment(Pos.CENTER);
        actionCard.setStyle("-fx-background-color: #181825; -fx-background-radius: 12; -fx-padding: 25;");
        actionCard.setMaxWidth(500);

        Label actionTitle = new Label("File Actions");
        actionTitle.setStyle("-fx-text-fill: #89b4fa; -fx-font-size: 16px; -fx-font-weight: bold;");

        HBox fileIdBox = new HBox(10);
        fileIdBox.setAlignment(Pos.CENTER);
        Label idLabel = new Label("File ID or Share Code:");
        idLabel.setStyle(labelStyle);
        TextField searchIdField = new TextField();
        searchIdField.setStyle(fieldStyle);
        searchIdField.setPrefWidth(250);
        fileIdBox.getChildren().addAll(idLabel, searchIdField);

        HBox buttonsBox = new HBox(15);
        buttonsBox.setAlignment(Pos.CENTER);
        Button downloadButton = new Button("Download");
        applyButtonHover(downloadButton, false);
        buttonsBox.getChildren().addAll(downloadButton);

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 13px;");

        Separator separator = new Separator();
        separator.setMaxWidth(400);

        HBox shareBox = new HBox(15);
        shareBox.setAlignment(Pos.CENTER);
        Label userShareTitle = new Label("Target User:");
        userShareTitle.setStyle(labelStyle);
        TextField targetUserField = new TextField();
        targetUserField.setStyle(fieldStyle);
        targetUserField.setPrefWidth(120);
        Button shareButton = new Button("Share");
        applyButtonHover(shareButton, false);
        shareBox.getChildren().addAll(userShareTitle, targetUserField, shareButton);

        actionCard.getChildren().addAll(actionTitle, fileIdBox, buttonsBox, resultLabel, separator, shareBox);

        Button backButton = new Button("Logout");
        applyButtonHover(backButton, true);

        mainLayout.getChildren().addAll(welcomeLabel, topSection, actionCard, backButton);

        uploadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File to Upload");
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                try {
                    byte[] masterKey = new byte[64];
                    new SecureRandom().nextBytes(masterKey);
                    byte[] aesKey = Arrays.copyOfRange(masterKey, 0, 32);
                    byte[] hmacKey = Arrays.copyOfRange(masterKey, 32, 64);

                    File encFile = new File(selectedFile.getAbsolutePath() + ".enc");
                    Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    aesCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"));
                    
                    try (FileOutputStream out = new FileOutputStream(encFile); FileInputStream in = new FileInputStream(selectedFile)) {
                        byte[] chunk = new byte[4096];
                        int read;
                        while ((read = in.read(chunk)) != -1) {
                            byte[] encChunk = aesCipher.update(chunk, 0, read);
                            if (encChunk != null) out.write(encChunk);
                        }
                        byte[] lastBytes = aesCipher.doFinal();
                        if (lastBytes != null) out.write(lastBytes);
                    }

                    byte[] hmac = generateHMAC(selectedFile, hmacKey);

                    Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                    rsaCipher.init(Cipher.ENCRYPT_MODE, myPublicKey);
                    byte[] rsaWrappedKey = rsaCipher.doFinal(masterKey);

                    byte[] blob = Files.readAllBytes(encFile.toPath());
                    
                    Map<String, Object> req = new HashMap<>();
                    req.put("ACTION", "UPLOAD");
                    req.put("user", currentUser);
                    req.put("fileName", selectedFile.getName());
                    req.put("blob", blob);
                    req.put("hmac", hmac);
                    req.put("rsaWrappedKey", rsaWrappedKey);

                    Map<String, Object> res = sendToServer(req);
                    
                    String fileId = (String) res.get("fileId");
                    String idAndName = selectedFile.getName() + " : " + fileId + "\n";
                    uploadedFilesList.appendText(idAndName);
                    
                    resultLabel.setText("Uploaded Successfully.");
                    resultLabel.setStyle("-fx-text-fill: #a6e3a1;");
                    
                    encFile.delete();

                } catch (Exception ex) {
                    resultLabel.setText("Upload Error: " + ex.getMessage());
                    resultLabel.setStyle("-fx-text-fill: #f38ba8;");
                }
            }
        });

        downloadButton.setOnAction(e -> {
            String inputCode = searchIdField.getText().trim();
            if (!inputCode.isEmpty()) {
                try {
                    Map<String, Object> req = new HashMap<>();
                    if (inputCode.length() == 8) {
                        req.put("ACTION", "DOWNLOAD_SHARED");
                        req.put("shareId", inputCode);
                    } else {
                        req.put("ACTION", "DOWNLOAD");
                        req.put("fileId", inputCode);
                    }
                    
                    Map<String, Object> res = sendToServer(req);

                    if ("OK".equals(res.get("STATUS"))) {
                        byte[] rsaWrappedKey;
                        byte[] blob;
                        byte[] hmacOrig;
                        String fileName;

                        if (inputCode.length() == 8) {
                            rsaWrappedKey = (byte[]) res.get("rsaWrappedKey");
                            blob = (byte[]) res.get("blob");
                            hmacOrig = (byte[]) res.get("hmac");
                            fileName = (String) res.get("fileName");
                        } else {
                            Map<String, Object> fileData = (Map<String, Object>) res.get("fileData");
                            rsaWrappedKey = (byte[]) fileData.get("rsaWrappedKey");
                            blob = (byte[]) fileData.get("blob");
                            hmacOrig = (byte[]) fileData.get("hmac");
                            fileName = (String) fileData.get("fileName");
                        }

                        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                        rsaCipher.init(Cipher.DECRYPT_MODE, myPrivateKey);
                        byte[] masterKey = rsaCipher.doFinal(rsaWrappedKey);

                        byte[] aesKey = Arrays.copyOfRange(masterKey, 0, 32);
                        byte[] hmacKey = Arrays.copyOfRange(masterKey, 32, 64);

                        File out = new File("Downloaded_" + fileName);
                        Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                        aesCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"));
                        Files.write(out.toPath(), aesCipher.doFinal(blob));

                        byte[] hmacCalc = generateHMAC(out, hmacKey);
                        if (MessageDigest.isEqual(hmacOrig, hmacCalc)) {
                            resultLabel.setText("Verification Successful.");
                            resultLabel.setStyle("-fx-text-fill: #a6e3a1;");
                        } else {
                            out.delete();
                            throw new Exception("HMAC Verification Failed.");
                        }
                    } else {
                        resultLabel.setText("File not found.");
                        resultLabel.setStyle("-fx-text-fill: #f38ba8;");
                    }
                } catch (Exception ex) {
                    resultLabel.setText("Error: " + ex.getMessage());
                    resultLabel.setStyle("-fx-text-fill: #f38ba8;");
                }
            }
        });

        shareButton.setOnAction(e -> {
            String fileId = searchIdField.getText().trim();
            String targetUser = targetUserField.getText().trim();
            
            if (!fileId.isEmpty() && !targetUser.isEmpty()) {
                try {
                    Map<String, Object> keyReq = new HashMap<>();
                    keyReq.put("ACTION", "GET_PUBLIC_KEY");
                    keyReq.put("targetUser", targetUser);
                    Map<String, Object> keyRes = sendToServer(keyReq);
                    
                    if (!"OK".equals(keyRes.get("STATUS"))) {
                        resultLabel.setText("Target user not found.");
                        resultLabel.setStyle("-fx-text-fill: #f38ba8;");
                        return;
                    }
                    
                    PublicKey friendKey = KeyFactory.getInstance("RSA").generatePublic(
                            new X509EncodedKeySpec((byte[]) keyRes.get("publicKey")));

                    Map<String, Object> dlReq = new HashMap<>();
                    dlReq.put("ACTION", "DOWNLOAD");
                    dlReq.put("fileId", fileId);
                    Map<String, Object> dlRes = sendToServer(dlReq);
                    byte[] myWrappedKey = (byte[]) ((Map<String, Object>) dlRes.get("fileData")).get("rsaWrappedKey");

                    Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                    rsaCipher.init(Cipher.DECRYPT_MODE, myPrivateKey);
                    byte[] masterKey = rsaCipher.doFinal(myWrappedKey);

                    rsaCipher.init(Cipher.ENCRYPT_MODE, friendKey);
                    byte[] friendWrappedKey = rsaCipher.doFinal(masterKey);

                    Map<String, Object> shareReq = new HashMap<>();
                    shareReq.put("ACTION", "SHARE");
                    shareReq.put("fileId", fileId);
                    shareReq.put("targetUser", targetUser);
                    shareReq.put("friendWrappedKey", friendWrappedKey);
                    
                    Map<String, Object> shareRes = sendToServer(shareReq);
                    
                    resultLabel.setText("Share ID: " + shareRes.get("shareId"));
                    resultLabel.setStyle("-fx-text-fill: #a6e3a1;");

                } catch (Exception ex) {
                    resultLabel.setText("Share Error.");
                    resultLabel.setStyle("-fx-text-fill: #f38ba8;");
                }
            }
        });

        backButton.setOnAction(e -> {
            currentUser = null;
            currentPass = null;
            myPrivateKey = null;
            myPublicKey = null;
            primaryStage.setScene(loginScene(primaryStage));
        });

        return new Scene(mainLayout, 800, 600);
    }

    public static boolean checkStrongPassword(String password) {
        int length = password.length();
        if (length < 8) {
            return false;
        }
        boolean upper = false;
        boolean sympol = false;
        for (int i = 0; i < length; i++) {
            char c = password.charAt(i);
            if (c == '@' || c == '#' || c == '$' || c == '%' || c == '^' || c == '*') {
                sympol = true;
            }
            if (Character.isUpperCase(c)) {
                upper = true;
            }
        }
        return sympol && upper;
    }

    public static void main(String[] args) {
        launch(args);
    }
}