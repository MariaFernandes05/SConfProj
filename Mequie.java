import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

//Cliente Mequie
public class Mequie {
//Input
private static ObjectInputStream in;
//Output
private static ObjectOutputStream out;
	public static void main(String[] args) throws Exception {
        //Não tem os argumentos obrigatorios
        if(args.length < 2){
            System.out.println("Formato de execução: Mequie <serverAddress> <localUserID> [password]" );
            System.exit(0);
        }
		//Servidor
		MequieServer server = new MequieServer();
		//Port do servidor
		String[] serverAddress = args[0].split(":");
        int portServer = Integer.parseInt(serverAddress[1]);
		//Socket a conectar
		Socket echoSocket = new Socket(serverAddress[0], portServer);
		//Input
		in = new ObjectInputStream(echoSocket.getInputStream());
		//Output
		out = new ObjectOutputStream(echoSocket.getOutputStream());
		//Username
		String nome = args[1];
        //Password
        String password;

    	Scanner sc = new Scanner(System.in);
        //Se o utilizador escreveu a password
        if(args.length == 3){
		    password = args[2];
        //Senão pedesse ao cliente para escrever a password
        }else{
            System.out.println("Introduza password:");
            password = sc.nextLine();
        }

		//Utilizador corrente
		User user= new User(nome,password);

		//Enviar para server dados de autentificacao
		out.writeObject(nome);
		out.writeObject(password);

		//Se o utilizador nao esta registado fecha se o programa
        if(in.readObject().equals("close"))
            System.exit(0);



		Boolean quit = false;
		//Enquanto o cliente quiser fazer operacoes
		while(!quit){
			System.out.print(">>>");
			//Lê o comando
			String comando = sc.nextLine();
			//Divide por espaco
			String[] parsela = comando.split(" ");
			//Vai ver qual o comando escrito
			if(parsela[0].equals("c")){
				server.create(parsela[1],user);
			}else if(parsela[0].equals("a")){
				server.addUtilizador(parsela[1],parsela[2],user);
			}else if(parsela[0].equals("r")){
				server.remover(parsela[1],parsela[2],user);
			}else if(parsela[0].equals("g")){
				server.ginfo(parsela[1],user);
			}else if(parsela[0].equals("u")){
				server.uinfo(user);
			}else if(parsela[0].equals("m")){
				server.msg(parsela[1], aux(parsela), user);
			} else if (parsela[0].equals("p")) {
				File fotoFile = new File(parsela[2]);
				byte[] content = Files.readAllBytes(fotoFile.toPath());
				out.writeObject(content);
				server.receiveFile(parsela[1]);
				server.photo(parsela[1], parsela[2], user);
			} else if (parsela[0].equals("co")) {
				server.collect(parsela[1], user);
			} else if (parsela[0].equals("h")) {
				server.history(parsela[1], user);
			} else {
				System.out.println("Não existe este comando");
				quit = true;
			}
		}
		// Fechar streams
		out.close();
		in.close();
		sc.close();

		// Fechar socket
		echoSocket.close();
	}

	// junta todas as parselas apos o comendo e o id do grupo, com um espaco entre
	// elas.
	// Retira o ultimo espaco quando retorna
	private static String aux(String[] frase) {
		StringBuilder sb = new StringBuilder();
		for (int i = 2; i < frase.length; i++)
			sb.append(frase[i] + " ");
		return sb.toString().substring(0, sb.length() - 1);
	}

	/*
	private static void sendFile(File file) throws Exception {
		
	}*/
	
}
