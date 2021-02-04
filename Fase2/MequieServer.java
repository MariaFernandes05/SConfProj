import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;

import java.security.cert.CertificateFactory;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.PublicKey;
import java.security.PrivateKey;

//Servidor MequieServer
public class MequieServer {
	// Grupo geral
	private Grupo geral = new Grupo("geral");
	// Todos os grupos criados
	private Map<String, Grupo> grupos = new HashMap<String, Grupo>();
	private int count;
	// Output
	private ObjectOutputStream outStream;
	// Input
	private ObjectInputStream inStream;
	private static KeyStore kstore;

	private static String keyStore;
	private static String keyStorePass;

	private int nomenclatura = 0;

	public static void main(String[] args) throws Exception {
		System.out.println("servidor: main");
		MequieServer server = new MequieServer();
		// Nao tem os argumentos obrigatorios
		if (args.length < 3) {
			System.out.println("Formato de execucao: MequieServer <port> <keystore> <keystore-password>");
			System.exit(0);
		}

		new File("SecureClient").mkdir();
		File userz = new File("users.txt");

		boolean ficEx = userz.exists();

		// ---------------------------------------------FASE2----------------------------------------------------------------------------------//
		// Buscar ficheiro keystore do server
		FileInputStream kfile = new FileInputStream(args[1]);
		kstore = KeyStore.getInstance("JCEKS");
		kstore.load(kfile, args[2].toCharArray());

		keyStore = args[1];
		keyStorePass = args[2];
		// ---------------------------------------------FASE2----------------------------------------------------------------------------------//
		// SERA QUE EH ASSIM PARA RSA??? OU EH SO PARA AES
		if (ficEx) {
			///Cifrar
			cifraFiles(userz,new File("users.cif"));
		}

		// Port do socket
		int port = Integer.parseInt(args[0]);
		// Iniciar socket
		server.startServer(port);
	}

	public void startServer(int port) {

		/////////////////////////////////////////////////// fase2//////////////////////////////////////////////////
		//

		System.setProperty("javax.net.ssl.keyStore", keyStore);
		System.setProperty("javax.net.ssl.keyStorePassword", keyStorePass);

		System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");
		ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
		SSLServerSocket ss = null;
		try {
			ss = (SSLServerSocket) ssf.createServerSocket(port);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
		/////////////////////////////////////////////////// fase2//////////////////////////////////////////////////

		while (true) {
			try {
				Socket inSoc = ss.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// sSoc.close();
	}

	// Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
		}

		public void run() {
			try {
				/////////////////////////////// Decifra////////////////////////////////////////////////////
				decifraFiles(new File("users.cif"),new File("users.txt"));
				

				//////////////////////////////////////////////////////////////
				// Output
				outStream = new ObjectOutputStream(socket.getOutputStream());
				// Input
				inStream = new ObjectInputStream(socket.getInputStream());
				// Credenciais a preencher
				String userID = null;
				// Ficheiro com os utilizadores registados
				File file = new File("users.txt");
				try {
					// Le do cliente
					userID = (String) inStream.readObject();

				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}

				// Faz scan do file
				Scanner scanFile = new Scanner(file);
				String stringFile;
				Boolean exists = false;
				// String indica se o programa acaba
				String programa = "open";
				String aux = "";
				String[] dup;

				// Recuperar users
				while (scanFile.hasNextLine()) {
					stringFile = scanFile.nextLine();
					aux = stringFile;
					dup = aux.split(",");
					// Regista o utilizador no geral
					geral.addUser(dup[0]);
				}

				// Recupera grupos
				loadGrupos();
				// Repoe o scanner no inicio do ficheiro
				scanFile = new Scanner(file);

				// cria nonce
				Random random = new Random();
				long randomLong = random.nextLong();

				// Enquanto o ficheiro tem texto
				while (scanFile.hasNextLine()) {
					// Vai buscar a proxima linha
					stringFile = scanFile.nextLine();
					// Se utilizador existe
					aux = stringFile;
					dup = aux.split(",");
					// se encontrarmos uma linha com user, autentica
					if (dup[0].equals(userID)) {
						exists = true;
						System.out.println("O utilizador existe.");
						// envia nonce

						outStream.writeObject(randomLong);
						outStream.writeObject(false);

						// recebe assinatura
						Signature s = Signature.getInstance("MD5withRSA");
						byte signature[] = (byte[]) inStream.readObject();

						///batota
						String nomeCert = dup[1];
						File fileCert = new File("PubKeys" + "\\" + nomeCert);

						// PORQUE NAO FUNCIONAR??
						CertificateFactory cf = CertificateFactory.getInstance("X509");
						Certificate cert = cf.generateCertificate(new FileInputStream(fileCert));

						PublicKey pk = cert.getPublicKey();
						s.initVerify(pk);
						s.update(ByteBuffer.allocate(Long.BYTES).putLong(randomLong).array());
						//////////////////////////// CIFRA//////////////////////////////////////////
						cifraFiles(file, new File("users.cif"));

						// Nao sei pq nao da.....
						// if (!s.verify(signature)) {
						if (s.verify(signature)) {
							System.out.println("A autenticacao nao foi bem sucedida.");
							// O programa precisa de terminar
							programa = "close";
						} else {
							outStream.writeObject("aberto");
							// Como fez login bem, vai para o menu
							menu();

						}

					}
				}

				// Se utilizador nao existe
				if (!exists) {
					// envia nonce e flag
					outStream.writeObject(randomLong);
					outStream.writeObject(true);
					// Recebe assinatura do nonce
					byte signature[] = (byte[]) inStream.readObject();
					// Recebe nonce original
					Long nonce = (Long) inStream.readObject();
					// Recebe o certificado 
					Certificate cert = (Certificate) inStream.readObject();
					// Ver se nonce enviado == recebido e (==) certificado(assinatura)
					PublicKey pk = cert.getPublicKey();
					Signature s = Signature.getInstance("MD5withRSA");
					s.initVerify(pk);
					s.update(ByteBuffer.allocate(Long.BYTES).putLong(randomLong).array());

					if (!(randomLong == nonce && s.verify(signature))) {
						System.out.println("Registo e a autenticacao nao foram bem-sucedidos");
						programa = "close";
					}

					// Se o de cima for verdade adiciona o user ao grupo geral
					geral.addUser(userID); // ?????? fase2
					Writer writer = new BufferedWriter(new FileWriter(file, true));
					StringBuilder sb = new StringBuilder();
					sb.append(userID + "," + userID + ".cert");
					writer.append(sb.toString() + "\n");
					writer.close();
					System.out.println("O utilizador foi registado e autenticado");

					///////////////////// CIFRA//////////////////////////////////////////////////////////
					/// FAZER FUNCAO AUXILIAR
					cifraFiles(file,new File("users.cif"));

					//////////////////////////////////////////////////////////////////////////////////////////////////

					// O programa precisa de terminar
					programa = "close";
					outStream.writeObject("close");
					scanFile.close();
					file.delete();
					System.exit(0);
				}

				// Fechar o programa
				outStream.writeObject(programa);
				if (programa.equals("close")) {
					scanFile.close();
					file.delete();
					System.exit(0);
				}
				// Fechar streams
				outStream.close();
				inStream.close();
				scanFile.close();

				// Fechar socket
				socket.close();

			} catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException
					| SignatureException | CertificateException e) {
				e.printStackTrace();
			}
		}
	}

	// DispÃµe os comandos vÃ¡lidos ao cliente
	private void menu() {
		System.out.println("Comandos: \n[c] create <groupID>\n" + "[a] addu<userID> <groupID> \n"
				+ "[r] removeu<userID> <groupID>\n" + "[g] ginfo<groupID> \n" + "[u] uinfo\n"
				+ "[m] msg<groupID><msg>\n" + "[p] photo<groupID> <photo>\n" + "[co] collect<groupID>\n"
				+ "[h] history<groupID>\n");
		boolean quit = false;
		while (!quit) {
			try {

				String comando = (String) inStream.readObject();
				String userID = (String) inStream.readObject();
				User user = new User(userID);
				String[] parsela = comando.split(" ");

				if (parsela[0].equals("c")) {
					create(parsela[1], user);
					Grupo gp = this.grupos.get(parsela[1]);
					new File(parsela[1] + "\\"+user.getNome()).mkdir();
					File f = new File(parsela[1] + "\\"+user.getNome()+"\\"+gp.getIdentificador()+".key");
					byte[] content = (byte[]) inStream.readObject();
					Files.write(f.toPath(), content);
					//cifrar tudo do grupo
					cifraFiles(new File(parsela[1]+"\\utilizadores.txt"), new File(parsela[1]+"\\utilizadores.cif"));
					cifraFiles(new File(parsela[1]+"\\owner.txt"), new File(parsela[1]+"\\owner.cif"));
					cifraFiles(new File(parsela[1]+"\\mensagens.txt"), new File(parsela[1]+"\\mensagens.cif"));
					cifraFiles(new File(parsela[1]+"\\historico.txt"), new File(parsela[1]+"\\historico.cif"));


				} else if (parsela[0].equals("a")) {
					
					addUtilizador(parsela[1], parsela[2], user);
					// envia todos os utilizadores do grupo
					enviaTodosOsUsers(parsela[2]);
					Grupo gp = this.grupos.get(parsela[2]);
					List<String> users = gp.getUsers();
					int identificador = gp.getIdentificador();
					new File(parsela[2] + "\\"+parsela[1]).mkdir();
					for(String userId: users){
						//// atualizar a chave de grupo e as chaves de cada user
						File key = new File(parsela[2] + "\\"+userId+"\\"+identificador+".key");		
						byte[] content = (byte[]) inStream.readObject();	
						Files.write(key.toPath(), content);
					}

					String result = "Utilizador " + parsela[1] + " foi adicionado ao grupo " + parsela[2];
					outStream.writeObject(result);

				} else if (parsela[0].equals("r")) {
					remover(parsela[1], parsela[2], user);

					// envia todos os utilizadores do grupo
					enviaTodosOsUsers(parsela[2]);
					Grupo gp = this.grupos.get(parsela[2]);
					List<String> users = gp.getUsers();
					int identificador = gp.getIdentificador();
					new File(parsela[2] + "\\"+parsela[1]).mkdir();
					for(String userId: users){
						//// atualizar a chave de grupo e as chaves de cada user
						File key = new File(parsela[2] + "\\"+userId+"\\"+identificador+".key");		
						byte[] content = (byte[]) inStream.readObject();	
						Files.write(key.toPath(), content);
					}

					String result = "Utilizador " + parsela[1] + " foi removido do grupo " + parsela[2];
					outStream.writeObject(result);

				} else if (parsela[0].equals("g")) {
					ginfo(parsela[1], user);
				} else if (parsela[0].equals("u")) {
					uinfo(user);
				} else if (parsela[0].equals("m")) {


					//envia chave do grupo e os users que pertencem a esse grupo
					Grupo gp= grupos.get(parsela[1]);
					int ind = gp.getIdentificador();

					byte[] keyBytes = Files.readAllBytes(Paths.get(parsela[1]+"\\"+ userID+"\\"+ ind + ".key"));
					outStream.writeObject(keyBytes);
					
					byte[] msgBytes = (byte[]) inStream.readObject();
					String msg =new String(msgBytes);
					msg(parsela[1], msg, user);
					File f = new File(parsela[1] + "\\mensagens\\"+ count+".cif");
					Files.write(f.toPath(),msgBytes);
					count++;



				} else if (parsela[0].equals("p")) {
					//File fotoFile = new File(parsela[2]);
					//byte[] content = Files.readAllBytes(fotoFile.toPath());

					//byte[] keyBytes = Files.readAllBytes(Paths.get(parsela[1]+"\\chaveGrupo.key"));
					//outStream.writeObject(keyBytes);
					//enviaTodosOsUsers(parsela[1]);
					
					//byte[] msgBytes = (byte[]) inStream.readObject();
					//String msg =new  String(msgBytes);

					//msg(parsela[1], msg, user);
					FileOutputStream out;
					out = new FileOutputStream(parsela[1]+"\\fotos\\"+nomenclatura+".jpg");
					byte[] bytes = new byte[16*1024];
					
					int count;
					
					while ((count = inStream.read(bytes)) > 0 ) {
						System.out.println(count);
						
						out.write(bytes, 0, count);
						if(count<1024){
							System.out.println(count+"FDSS");
							out.write(bytes, 0, count);
						}
					}
					
					out.close();
					photo(parsela[1], String.valueOf(nomenclatura), user);
					nomenclatura ++;
				} else if (parsela[0].equals("co")) {
					collect(parsela[1], user);
				} else if (parsela[0].equals("h")) {
					history(parsela[1], user);
				} else {
					System.out.println("Não existe este comando");
					quit = true;
				}

			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Cria um novo grupo
	public void create(String grupoId, User owner) throws IOException, FileNotFoundException {
		// Grupo ja existe
		if (grupos.containsKey(grupoId)) {
			outStream.writeObject("Grupo ja existe");
			System.exit(0);
		}

		// Guardar os grupos criados num ficheiro
		File gruposFile = new File("grupos.txt");
		Writer write = new BufferedWriter(new FileWriter(gruposFile, true));
		write.append(grupoId + "\n");
		write.close();

		// Novo grupo
		Grupo novo = new Grupo(grupoId, owner);
		// Adiciona ao map
		grupos.put(grupoId, novo);
		// Cria uma pasta que vai conter as informacoes do grupo
		new File(grupoId).mkdir();
		// Cria um ficheiro para mensagens nao vistas por todos
		createFiles(grupoId + "//mensagens.txt");
		// Cria um ficheiro com as mensagens vistas por todos
		createFiles(grupoId + "//historico.txt");
		// Cria uma pasta para fotos
		new File(grupoId + "//fotos").mkdir();
		//Cria uma pasta para as mensagens
		new File(grupoId + "//mensagens").mkdir();
		// Cria ficheiro que guarda owner
		createFiles(grupoId + "//owner.txt");
		// Escreve owner no ficheiro
		writeStringFile(owner.getNome(), grupoId + "//owner.txt");
		// Cria ficheiro que guarda utilizadores
		createFiles(grupoId + "//utilizadores.txt");
		// Escreve o owner no ficheiro, pois o mesmo e um utilizador
		writeStringFile(owner.getNome()+","+owner.getNome(), grupoId + "//utilizadores.txt");

		String result = owner.getNome() + " criou um grupo: " + grupoId;
		outStream.writeObject(result);

	}

	// Adiciona utilizador
	public void addUtilizador(String userId, String grupoId, User user) throws IOException, FileNotFoundException {
		decifraFiles(new File(grupoId + "//utilizadores.cif"), new File(grupoId + "//utilizadores.txt"));
		// Grupo nao existe
		if (!grupos.containsKey(grupoId)) {
			outStream.writeObject("Grupo nao existe");
			System.exit(0);
		}
		// Vai se buscar o grupo
		Grupo gp = grupos.get(grupoId);
		// O user nao e o dono do grupo
		if (!gp.getOwner().getNome().equals(user.getNome())) {
			outStream.writeObject("Utilizador nao e o owner do grupo");
			System.exit(0);
		}
		// O utilizador ja pertence ao grupo
		if (gp.addUser(userId) == -1) {
			outStream.writeObject("Utilizador ja pertence ao grupo");
			System.exit(0);
		}

		// Escrever na pasta do grupo
		writeStringFile(userId+","+userId, grupoId + "//utilizadores.txt");
		cifraFiles(new File(grupoId + "//utilizadores.txt"),new File (grupoId + "//utilizadores.cif"));

		
	}

	// Remover um utilizador
	public void remover(String userId, String grupoId, User user) throws IOException, FileNotFoundException {
		decifraFiles(new File(grupoId + "//utilizadores.cif"), new File(grupoId + "//utilizadores.txt"));
		// Grupo nao existe
		if (!grupos.containsKey(grupoId)) {
			outStream.writeObject("Grupo nao existe");
			System.exit(0);
		}
		// Vai buscar o grupo
		Grupo gp = grupos.get(grupoId);
		// O user nao e o owner
		if (!gp.getOwner().getNome().equals(user.getNome())) {
			outStream.writeObject("Utilizador nao e o owner do grupo");
			System.exit(0);
		}
		// Utilizador nao pertence ao grupo
		if (!gp.containsUser(userId)) {
			outStream.writeObject("Utilizador nao pertence ao grupo");
			System.exit(0);
		}
		// Remove utilizador
		gp.remove(userId);
		// Remover utilizador da pasta
		System.out.println(userId);
		removeUserFile(userId+","+userId, grupoId + "//utilizadores.txt");
		//writeStringFile("", grupoId + "//utilizadores.txt");
		cifraFiles(new File(grupoId + "//utilizadores.txt"),new File (grupoId + "//utilizadores.cif"));

	}

	// Informacao sobre um grupo
	public void ginfo(String grupoId, User user) throws FileNotFoundException {

		// Grupo nao existe
		if (!grupos.containsKey(grupoId)) {
			try {
				outStream.writeObject("Grupo nao existe");
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.exit(0);
		}

		// Vai buscar o grupo
		Grupo gp = grupos.get(grupoId);
		// Imprime informacoes
		StringBuilder sb = new StringBuilder();

		sb.append("Nome do grupo: " + grupoId +"\n");
		sb.append("Dono: " + gp.getOwner().getNome()+"\n");
		sb.append("Numero de utilizadores: " + gp.getUsers().size()+"\n");
		if (gp.getOwner().getNome().equals(user.getNome())) {
			sb.append("Utilizadores: ");
			sb.append(gp.getUsers()+" ");
		}
		try {
			outStream.writeObject(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// informacoes sobre o utilizador
	public void uinfo(User user) throws FileNotFoundException {
		// Grupos de que o user e owner
		List<String> gruposOwner = new ArrayList<String>();
		// Grupos a que o user pertence
		List<String> gruposPertence = new ArrayList<String>();
		// Percorrer todos os grupos criados
		for (Map.Entry<String, Grupo> entry : grupos.entrySet()) {
			// Pertence ao grupo
			if (entry.getValue().containsUser(user.getNome())) {
				gruposPertence.add(entry.getKey());
			}
			// E owner do grupo
			if (entry.getValue().getOwner().getNome().equals(user.getNome())) {
				gruposOwner.add(entry.getKey());
			}
		}

		String result = "O utilizador e dono dos grupos: " + gruposOwner.toString()
				+ "\nO utilizador pertence aos grupos: " + gruposPertence.toString();
		try {
			outStream.writeObject(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void msg(String grupoId, String mensagem, User user) throws FileNotFoundException, IOException {
		decifraFiles(new File(grupoId + "//mensagens.cif"), new File(grupoId + "//mensagens.txt"));
		// Grupo nao existe
		if (!grupos.containsKey(grupoId)) {
			outStream.writeObject("Grupo nao existe");
			System.exit(0);
		}
		// Vai buscar o grupo
		Grupo gp = grupos.get(grupoId);
		// Utilizador nao pertence ao grupo
		if (!gp.containsUser(user.getNome())) {
			System.out.println("Utilizador nao pertence ao grupo");
			System.exit(0);
		}
		// Cria mensagem
		Mensagem msg = new Mensagem(mensagem, user.getNome());
		//Identificador da key
		int ind=gp.getIdentificador();
		msg.setIdentificador(ind);
		// Adiciona mensagem ao grupo
		gp.addMsg(msg);
		
		// Escreve no ficheiro de mensagens do grupo a mensagem
		writeStringFile(ind + "," + grupoId + "\\mensagens\\"+ count+".cif", grupoId + "/mensagens.txt");
		
		outStream.writeObject("Mensagem foi enviada");
		cifraFiles(new File(grupoId + "//mensagens.txt"),new File (grupoId + "//mensagens.cif"));
	}

	public void photo(String grupoId, String fotoName, User user) throws FileNotFoundException,IOException {
		// Grupo nao existe
		if (!grupos.containsKey(grupoId)) {
			outStream.writeObject("Grupo nao existe");
			System.exit(0);
		}
		// Vai buscar o grupo
		Grupo gp = grupos.get(grupoId);
		// Utilizador nao pertence ao grupo
		if (!gp.containsUser(user.getNome())) {
			outStream.writeObject("Utilizador nao pertence ao grupo");
			System.exit(0);
		}
		// SERA QUE E SO ASSIM?
		// Cria foto
		Photo foto = new Photo(fotoName, user.getNome());
		//Identificador da key
		foto.setIdentificador(gp.getIdentificador());
		// Adiciona foto ao grupo
		gp.addFoto(foto);
		outStream.writeObject("Foto foi enviada");
	}

	public void collect(String grupoId, User user) throws IOException, FileNotFoundException {
		Grupo gp = grupos.get(grupoId);
		List<Mensagem> msgs = gp.msgNaoVistasPor(user);
		// List<Photo> fotos = gp.fotosNaoVistasPor(user);
		StringBuilder sb = new StringBuilder();
		for (Mensagem msg : msgs) {
			sb.append(msg.getStringMsg() + "\n");
			msg.addVisto(user.getNome());

			if (gp.vistoPorTodos(msg)) {
				writeStringFile(msg.getStringMsg(), grupoId + "/historico.txt");
				gp.removeMsg(msg);
				removeUserFile(msg.getStringMsg(), grupoId + "/mensagens.txt");
			}
		}
		/*
		 * for(Photo foto : fotos){ //System.out.println() ???? ou enviar ficheiro de
		 * volta ???? foto.addVisto(user.getNome()); if(gp.photoVistoPorTodos(foto)){
		 * gp.removeFoto(foto); } }
		 */
		outStream.writeObject(sb.toString());
	}

	public void history(String grupoId, User user) throws FileNotFoundException, IOException {
		// Grupo nao existe
		if (!grupos.containsKey(grupoId)) {
			outStream.writeObject("Grupo nao existe");
			System.exit(0);
		}
		// Vai buscar o grupo
		Grupo gp = grupos.get(grupoId);
		// Utilizador nao pertence ao grupo
		if (!gp.containsUser(user.getNome())) {
			outStream.writeObject("Utilizador nao pertence ao grupo");
			System.exit(0);
		}
		Scanner historicoFile = new Scanner(new File(grupoId + "/historico.txt"));
		// Linha corrente do ficheiro historico.txt
		String current;
		// Enquanto ha grupos
		StringBuilder sb = new StringBuilder();
		while (historicoFile.hasNextLine()) {
			current = historicoFile.nextLine();
			sb.append(current + "\n");
		}
		outStream.writeObject(sb.toString());
	}



	//Cria ficheiros
	private void createFiles(String nameFile) throws IOException {
		File novo = new File(nameFile);
		novo.createNewFile();
	}

	//Escreve String num ficheiros
	private void writeStringFile(String frase, String file) throws IOException {
		Writer write = new BufferedWriter(new FileWriter(file, true));
		write.append(frase +"\n");
		write .close();
	}

	//Remove linha de um ficheiro
	private void removeUserFile(String rem, String nameFile) throws IOException {
		File old = new File(nameFile);
		Scanner sc = new Scanner(old);
		File tmp = new File("tmp.txt");
		tmp.createNewFile();
		Writer write = new BufferedWriter(new FileWriter(tmp, true));
		String current;
		while(sc.hasNextLine()){
			current = sc.nextLine();
			if(current.equals(rem)){
				continue;
			}
			write.append(current);
		}
		tmp.renameTo(old);
		write.close();
		sc.close();




		//public void removeLineFromFile(String file, String lineToRemove) {
		/*	File inFile = new File(nameFile);
		File tempFile = new File(inFile.getAbsolutePath() + ".tmp");

      BufferedReader br = new BufferedReader(new FileReader(nameFile));
      PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

      String line = null;

      //Read from the original file and write to the new
      //unless content matches data to be removed.
      while ((line = br.readLine()) != null) {

        if (!line.trim().equals(rem)) {

          pw.println(line);
          pw.flush();
        }
      }
      pw.close();
	  br.close();
	  tempFile.renameTo(new File(inFile.getAbsolutePath()));*/
	}

	/*
	 * vai reler tudo de cada grupo
	 *Ainda so recuperamos o dono, os utilizadores e mensagens
	 *
	 *TODO: a parte das imagens
	 */
	private void loadGrupos() throws FileNotFoundException {
		
		
		//Scanner para o ficheiro grupos.txt
		Scanner gruposFile = new Scanner(new File("grupos.txt"));
		//Linha corrente do ficheiro grupos.txt
		String current;
		//Enquanto ha grupos
		while(gruposFile.hasNextLine()){
			current = gruposFile.nextLine();
			decifraFiles(new File(current+"\\owner.cif"),new File(current+"\\owner.txt"));
			decifraFiles(new File(current+"\\utilizadores.cif"),new File(current+"\\utilizadores.txt"));
			decifraFiles(new File(current+"\\mensagens.cif"),new File(current+"\\mensagens.txt"));
			decifraFiles(new File(current+"\\historico.cif"),new File(current+"\\historico.txt"));
			//Scanner para o ficheiro owner.txt do grupo atual
			Scanner ownerFile = new Scanner(new File(current+"/owner.txt"));
			String tmpOwner = ownerFile.nextLine();
			//Criacao do owner
			User owner = new User(tmpOwner);
			//Criacao do grupo
			Grupo novo = new Grupo(current,owner);
			//Adicionar grupo ao mapa
			this.grupos.put(current,novo);
			//Scanner para o ficheiro utilizadores.txt do grupo atual
			Scanner utilizadoresFile = new Scanner(new File(current+"/utilizadores.txt"));
			//Utilizador atual
			String currentUt;
			//Enquanto ha utilizadores
			while(utilizadoresFile.hasNextLine()){
				currentUt = utilizadoresFile.nextLine();
				//Ir buscar grupos 
				Grupo gp = grupos.get(current);
				//Adicionar utilizador
				String userNome = currentUt.split(",")[0];
				gp.addUser(userNome);

				/////load das chaves         FASE2
				User n = new User(userNome);
				gp.addUserToMap(n);
				String[] pathnames;
	        	File f = new File(gp.getId()+"\\"+ userNome);
				pathnames = f.list();
				System.out.println(userNome);
				for (String pathname : pathnames) {//
					System.out.println(pathname);
					StringTokenizer multiTokenizer = new StringTokenizer(pathname, ".");
					String ind = multiTokenizer.nextToken();
					gp.addChaveToUserMap(n, Integer.parseInt(ind), new File(gp.getId()+"\\"+ userNome + pathname));
				}
			}
			loadMensagens(novo);
			ownerFile.close();
			utilizadoresFile.close();
			cifraFiles(new File(current+"\\owner.txt"),new File(current+"\\owner.cif"));
			cifraFiles(new File(current+"\\utilizadores.txt"),new File(current+"\\utilizadores.cif"));
			cifraFiles(new File(current+"\\mensagens.txt"),new File(current+"\\mensagens.cif"));
			cifraFiles(new File(current+"\\historico.txt"), new File(current+"\\historico.cif"));
		}
		gruposFile.close();
	}

	private void loadMensagens(Grupo grupo) throws FileNotFoundException {
		Scanner scM = new Scanner(new File(grupo.id + "/mensagens.txt"));
		String current;

		while(scM.hasNextLine()){
			current = scM.nextLine();
			Mensagem msg = new Mensagem(current);
			grupo.addMsg(msg);
		}
		scM.close();
	}

	public void receiveFile(String path) throws IOException, ClassNotFoundException {
		File dest = new File(path);
		byte[] content = (byte[]) inStream.readObject();
		Files.write(dest.toPath(),content);
	}

	private void enviaTodosOsUsers(String grupoId) throws IOException {

		Grupo gp = this.grupos.get(grupoId);
		List <String> users = gp.getUsers();
		StringBuilder sb = new StringBuilder();
		for(String user: users){
			sb.append(user+",");
		}
		sb.setLength(sb.length() - 1);
		outStream.writeObject(sb.toString());
	}



	private static void cifraFiles(File txt,File cif){
		PrivateKey key;
		try {
			key = (PrivateKey) kstore.getKey("MequieServer", keyStorePass.toCharArray());
		
			Cipher c = Cipher.getInstance("RSA");
			c.init(Cipher.ENCRYPT_MODE, key);

			FileInputStream fis = new FileInputStream(txt);

			FileOutputStream fos;
			CipherOutputStream cos;

			fos = new FileOutputStream(cif);

			cos = new CipherOutputStream(fos, c);
			byte[] b = new byte[16];
			int i = fis.read(b);
			while (i != -1) {
				cos.write(b, 0, i);
				i = fis.read(b);
			}
		

			cos.close();
			fis.close();
			fos.close();
			//Why you don't delete????
			txt.delete();

		} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException  | IOException e) {
			e.printStackTrace();
		}

	}

	private static void decifraFiles(File cif, File txt){
		try {
			CipherInputStream cis;
			FileInputStream fis_cif;
			FileOutputStream teste;

			fis_cif = new FileInputStream(cif);
			teste = new FileOutputStream(txt);

			PublicKey kr = kstore.getCertificate("MequieServer").getPublicKey();

			Cipher ciii;
		
			ciii = Cipher.getInstance("RSA");
		
			ciii.init(Cipher.DECRYPT_MODE, kr);
			
			cis = new CipherInputStream(fis_cif, ciii);

			byte[] bi = new byte[16];
			int j = cis.read(bi);
			while (j != -1) {
				teste.write(bi, 0, j);
				j = cis.read(bi);
			}

			cis.close();
			fis_cif.close();
			teste.close();
			
			
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | KeyStoreException
				| InvalidKeyException | IOException e) {
			e.printStackTrace();
		}

	}

}
