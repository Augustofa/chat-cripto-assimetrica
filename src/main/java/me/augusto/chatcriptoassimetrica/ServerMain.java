package me.augusto.chatcriptoassimetrica;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Vector;
import javafx.util.Pair;

public class ServerMain extends Thread{
    private static Vector clientes;
    private static Vector<Pair<String,String>> chavesClientes;
    private final Socket conexao;
    private String meuNome;

    public ServerMain(Socket con){
        this.conexao = con;
    }

    public static void main(String[] args) {
        clientes = new Vector();
        chavesClientes = new Vector();

        try{
            ServerSocket ss = new ServerSocket(2222);

            while(true){
                System.out.println("Aguardando uma conexão...");
                Socket con = ss.accept();
                System.out.println("Conexão realizada");

                Thread t = new ServerMain(con);
                t.start();

            }
        }catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    public void run(){
        try{
            BufferedReader entrada = new BufferedReader(new InputStreamReader(this.conexao.getInputStream()));
            PrintStream saida = new PrintStream(this.conexao.getOutputStream());

            String intro = entrada.readLine();
            String[] partesIntro = intro.split(": ");
            this.meuNome = partesIntro[1];
            
            if(this.meuNome == null){
                return;
            }

            clientes.add(saida);
            chavesClientes.add(new Pair(partesIntro[1], partesIntro[2]));
            
            enviarParaTodos(saida, " entrou", " no chat");
            enviarIntro(saida, intro);

            String linha = entrada.readLine();

            while(linha != null && !(linha.trim().isEmpty())){
                enviarParaTodos(saida, ": ", linha);
                linha = entrada.readLine();
            }
            enviarParaTodos(saida, " saiu", " do chat");

            this.conexao.close();
        }catch(IOException ex){
            System.out.println("Conexão Encerrada!");
        }
    }

    public void enviarParaTodos(PrintStream saida, String acao, String linha){
        Enumeration e = clientes.elements();

        while(e.hasMoreElements()){
            PrintStream chat = (PrintStream) e.nextElement();
            if(chat != saida){
                chat.println(this.meuNome + acao + linha);
            }else{
                chat.println("Você" + acao + linha);
            }
        }
    }
    
    public void enviarIntro(PrintStream saida, String intro){
        for(int i = 0; i < clientes.size(); i++){
            PrintStream cliente = (PrintStream) clientes.get(i);
            
            for(int j = 0; j < clientes.size(); j++){
                if(i != j){
                    cliente.println("usuario: " + chavesClientes.get(j).getKey() + ": " + chavesClientes.get(j).getValue());
                }
            }
        }
    }
}
