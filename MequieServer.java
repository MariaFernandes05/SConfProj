import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.Writer;

//Servidor MequieServer
public class MequieServer {

	public static void main(String[] args) {
		System.out.println("servidor: main");
		MequieServer server = new MequieServer();
        //Nao tem os argumentos obrigatorios
        if(args.length <1){
            System.out.println("Formato de execução: MequieServer <port>" );
            System.exit(0);
        }
        //Port do socket    
        int port = Integer.parseInt(args[0]);
        //Iniciar socket
		server.startServer(port);
	}

	public void startServer (int port){
		ServerSocket sSoc = null;
		try {
            //Socket 
			sSoc = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
         
		while(true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		    
		}
		//sSoc.close();
	}

	//Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;
		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}
 
		public void run(){
			try {
                //Output
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                //Input
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
                //Credenciais a preencher
				String user = null;
				String passwd = null;
                //Ficheiro com os utilizadores registados
                File file = new File("users.txt");
				try {
                    //Lê do cliente
					user = (String)inStream.readObject();
					passwd = (String)inStream.readObject();
					System.out.println("thread: depois de receber a password e o user");
				}catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}
                //Constroi string do tipo <use>:<pass>
				StringBuilder sb = new StringBuilder();
                sb.append(user);
                sb.append(":");
                sb.append(passwd);

                //Faz scan do file
                Scanner scanFile = new Scanner(file);
                String stringFile;
                Boolean exists = false;

                //Enquanto o ficheiro tem texto
                while(scanFile.hasNextLine()){
                    //Vai buscar a proxima linha
                    stringFile = scanFile.nextLine();
                    //Se utilizador existe
                    if(stringFile.equals(sb.toString())){
                        exists = true;
                        System.out.println("O utilizador foi autenticado");
                        menu();
                    }
                }
                scanFile.close();
                //String indica se o programa acaba
                String programa = "open";
                //Se utilizador nao existe
                if(!exists){
                    //escreve no ficheiro o user:password                              
                    Writer writer = new BufferedWriter(new FileWriter(file, true));
				    writer.append(sb.toString() + "\n");
				    writer.close();
                    System.out.println("Utilizador não estah registado");
                    //O programa precisa de terminar
                    programa = "close";
                    outStream.writeObject("close");
                    System.exit(0);
                }
                //Fechar o programa
			    outStream.writeObject(programa);
                if(programa.equals("close"))
                    System.exit(0);

                //Fechar streams   
				outStream.close();
				inStream.close();

                //Fechar socket
				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			} 
		}

        private void menu(){
            System.out.println("Comandos: \n[c] create <groupID>\n" +
                    "[a] addu<userID> <groupID> \n" +
                    "[r] removeu<userID> <groupID>\n" +
                    "[g] ginfo<groupID> \n" +
                    "[u] uinfo\n" +
                    "[m] msg<groupID><msg>\n" +
                    "[p] photo<groupID> <photo>\n" +
                    "[co] collect<groupID>\n" +
                    "[h] history<groupID>\n");
        }
	}
}