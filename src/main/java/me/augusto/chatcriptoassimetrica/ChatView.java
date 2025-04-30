/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package me.augusto.chatcriptoassimetrica;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 *
 * @author augus
 */
public class ChatView extends javax.swing.JFrame implements Runnable{
    private final Socket conexao;
    private static PrintStream saida;
    private static BufferedReader teclado;
    private final ChatView chatView;
    private static PrivateKey chavePrivada;
    private static PublicKey chavePublica;
    private static HashMap<String, PublicKey> usuarios;
    private static HashSet<String> nomesUsuarios;
//    private static GCMParameterSpec iv;
    
    public ChatView(){
        initComponents();
        this.conexao = null;
        this.chatView = null;
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saida.println("");
                System.exit(0);
            }
        });
        
        usuarios = new HashMap();
        nomesUsuarios = new HashSet();
    }
    
    public ChatView(Socket con, ChatView chatView){
        this.conexao = con;
        this.chatView = chatView;
    }
    
    public void geraChave() throws NoSuchAlgorithmException, InvalidKeySpecException{
        KeyPairGenerator geradorChaves = KeyPairGenerator.getInstance("RSA");
        geradorChaves.initialize(2048);
        KeyPair parChaves = geradorChaves.genKeyPair();
        chavePrivada = parChaves.getPrivate();
        chavePublica = parChaves.getPublic();
    }
    
    public PublicKey stringtoChave(String strChave) throws NoSuchAlgorithmException, InvalidKeySpecException{
        byte[] bytesChave = Base64.getDecoder().decode(strChave);
        
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(bytesChave);
        return keyFactory.generatePublic(publicKeySpec);
    }
    
    public String encriptaMsg(String msg, PublicKey chavePublicaAlvo) {
        try {
            Cipher cifraEncripta = Cipher.getInstance("RSA");
            cifraEncripta.init(Cipher.ENCRYPT_MODE, chavePublicaAlvo);
            byte[] msgEncriptada = cifraEncripta.doFinal(msg.getBytes());
            
            return Base64.getEncoder().encodeToString(msgEncriptada);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(ChatView.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public String decriptaMsg(String msgCifrada) {
        try {
            Cipher cifraDecripta = Cipher.getInstance("RSA");
            cifraDecripta.init(Cipher.DECRYPT_MODE, chavePrivada);
            byte[] msgBytes = cifraDecripta.doFinal(Base64.getDecoder().decode(msgCifrada));
            
            return new String(msgBytes, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException ex) {
            Logger.getLogger(ChatView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex){
            return "(MENSAGEM CRIPTOGRAFADA)";
        }
        return null;
    }
    
    public void iniciaChat(String nome) {
        try{
            this.setTitle("Chat de " + nome);
            geraChave();
            System.out.println("Minha chave: " + Base64.getEncoder().encodeToString(chavePublica.getEncoded()));

            Socket con = new Socket("127.0.0.1", 2222);

            saida = new PrintStream(con.getOutputStream());
            saida.println("usuario: " + nome + ": " + Base64.getEncoder().encodeToString(chavePublica.getEncoded()));
            
            Thread t = new Thread(new ChatView(con, this));

            t.start();
        }catch(IOException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
            ex.printStackTrace();
        }
    }
    
    public void run(){
        try{
            BufferedReader entrada = new BufferedReader(new InputStreamReader(this.conexao.getInputStream()));
            String linhaRecebida;

            while(true){
                linhaRecebida = entrada.readLine();
                if(linhaRecebida == null){
                    System.out.println("Conexão encerrada!");
                    break;
                }
                System.out.println("Recebendo: " + linhaRecebida);
                String[] partesMsg = linhaRecebida.split(": ");
                
                if(partesMsg[0].equals("usuario")){
                    System.out.println("Chave Recebida: " + partesMsg[2]);
                    adicionaUsuario(new Pair<>(partesMsg[1], stringtoChave(partesMsg[2])));
                }else{
                    if(partesMsg.length > 1){
                        String msgRecebida = partesMsg[1];
                        String msgOriginal = decriptaMsg(msgRecebida);
                        this.chatView.atualizaChat(partesMsg[0] + ": " + msgOriginal);
                    }else{
                        this.chatView.atualizaChat(partesMsg[0]);                 
                    }
                }
            }

            this.conexao.close();
        }catch(IOException ex){
            ex.printStackTrace();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            Logger.getLogger(ChatView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void atualizaChat(String msg) {
        String texto = this.chatBox.getText();
        this.chatBox.setText(texto + msg + "\n");
    }
    
    private void adicionaUsuario(Pair<String,PublicKey> parUsuario){
        usuarios.put(parUsuario.getKey(), parUsuario.getValue());
        nomesUsuarios.add(parUsuario.getKey());
        chatView.boxListUsuarios.setListData(nomesUsuarios.toArray(String[]::new));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        chatBox = new javax.swing.JTextArea();
        msgBox = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        btnEnviar = new javax.swing.JButton();
        btnVoltar = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        boxListUsuarios = new javax.swing.JList<>();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        chatBox.setBackground(new java.awt.Color(0, 0, 0));
        chatBox.setColumns(20);
        chatBox.setForeground(new java.awt.Color(255, 255, 255));
        chatBox.setLineWrap(true);
        chatBox.setRows(5);
        chatBox.setFocusable(false);
        jScrollPane1.setViewportView(chatBox);

        msgBox.setForeground(new java.awt.Color(102, 102, 102));
        msgBox.setText("Mensagem");
        msgBox.setToolTipText("Mensagem");
        msgBox.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                msgBoxFocusGained(evt);
            }
        });
        msgBox.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                msgBoxKeyPressed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Chat");

        btnEnviar.setText("Enviar");
        btnEnviar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviarActionPerformed(evt);
            }
        });

        btnVoltar.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnVoltar.setText("<");
        btnVoltar.setFocusable(false);
        btnVoltar.setMargin(new java.awt.Insets(0, 0, 5, 0));
        btnVoltar.setRequestFocusEnabled(false);
        btnVoltar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVoltarActionPerformed(evt);
            }
        });

        boxListUsuarios.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(boxListUsuarios);

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setText("Usuários");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 379, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 125, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(msgBox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnEnviar)))
                        .addGap(12, 12, 12))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnVoltar)
                        .addGap(122, 122, 122)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel2)
                        .addGap(49, 49, 49))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(jLabel2))
                    .addComponent(btnVoltar, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane2)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE))
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnEnviar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(msgBox, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnEnviarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnviarActionPerformed
        String msg = this.msgBox.getText();
        PublicKey chaveAlvo = usuarios.get(this.boxListUsuarios.getSelectedValue());
        String msgEncriptada = encriptaMsg(msg, chaveAlvo);
        System.out.println("Enviando: " + msgEncriptada);
        
        saida.println(msgEncriptada);
        this.msgBox.setText("");
    }//GEN-LAST:event_btnEnviarActionPerformed

    private void msgBoxFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_msgBoxFocusGained
        if (msgBox.getText().equals("Mensagem")) {
            msgBox.setText("");
            msgBox.setForeground(Color.BLACK);
        }
    }//GEN-LAST:event_msgBoxFocusGained

    private void msgBoxKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_msgBoxKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            this.btnEnviar.doClick();
        }
    }//GEN-LAST:event_msgBoxKeyPressed

    private void btnVoltarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVoltarActionPerformed
        saida.println("");
        this.dispose();
        new ClienteChatGUI().setVisible(true);
    }//GEN-LAST:event_btnVoltarActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList<String> boxListUsuarios;
    private javax.swing.JButton btnEnviar;
    private javax.swing.JButton btnVoltar;
    private javax.swing.JTextArea chatBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField msgBox;
    // End of variables declaration//GEN-END:variables
}
