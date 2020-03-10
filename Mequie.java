import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

//Cliente Mequie
public class Mequie {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
        //Não tem os argumentos obrigatorios
        if(args.length < 2){
            System.out.println("Formato de execução: Mequie <serverAddress> <localUserID> [password]" );
            System.exit(0);
        }
        //Port do servidor
        int portServer = Integer.parseInt(args[0]);
		//Socket a conectar
		Socket echoSocket = new Socket("localhost", portServer);
		//Input
		ObjectInputStream in = new ObjectInputStream(echoSocket.getInputStream());
		//Output
		ObjectOutputStream out = new ObjectOutputStream(echoSocket.getOutputStream());
		//Username
		String user = args[1];
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

		//Enviar para server dados de autentificacao
		out.writeObject(user);
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
			//Vai ver qual o camndo escrito
			if(parsela[0].equals("c")){

			}else if(parsela[0].equals("a")){

			}else if(parsela[0].equals("r")){
							
			}else if(parsela[0].equals("g")){
				
			}else if(parsela[0].equals("u")){
				
			}else if(parsela[0].equals("m")){
				
			}else if(parsela[0].equals("p")){
				
			}else if(parsela[0].equals("co")){
				
			}else if(parsela[0].equals("h")){
				
			}else{
				System.out.println("Não existe este comando");
				quit= true;
			}
		}
		//Fechar streams
		out.close();
		in.close();

		
		//Fechar socket
		echoSocket.close();
	}
}
