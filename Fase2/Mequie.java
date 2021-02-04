import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import java.security.*;

//Cliente Mequie
public class Mequie {
	// Input
	private static ObjectInputStream in;
	// Output
	private static ObjectOutputStream out;
	private static KeyStore TStore;
	private static KeyStore KStore;

	public static void main(String[] args) throws Exception {
		// Não tem os argumentos obrigatorios
		if (args.length < 5) {
			System.out.println(
					"Formato de execução: Mequie <serverAddress> <truststore> <keystore> <keystore-password> <localUserID>");
			System.exit(0);
		}
		// Servidor
		MequieServer server = new MequieServer();

		// Port do servidor
		String[] serverAddress = args[0].split(":");
		String portAdd = serverAddress[0];
		int portServer = Integer.parseInt(serverAddress[1]);
		System.setProperty("javax.net.ssl.trustStore", args[1]);
		System.setProperty("javax.net.ssl.keyStore", args[2]);
		System.setProperty("javax.net.ssl.keyStorePassword", args[3]);
		System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");
		System.setProperty("javax.net.ssl.trustStoreType", "JCEKS");
		System.setProperty("javax.net.ssl.keyFactoryType", "AES");
		SocketFactory sf = SSLSocketFactory.getDefault();

		try (SSLSocket echoSocket = (SSLSocket) sf.createSocket(portAdd, portServer)) {

			// ----------------------------------------------------FASE2--------------------------------------------------------------------------//
			// Buscar ficheiro Truststore com certificado do server
			FileInputStream Tfile = new FileInputStream(args[1]);
			TStore = KeyStore.getInstance("JCEKS");
			// TEM PASSWORD?? EH IGUAL A ALGUMA CONHECIDA???
			TStore.load(Tfile, "client".toCharArray());

			// Buscar keystore do client
			FileInputStream kfile = new FileInputStream(args[2]);
			KStore = KeyStore.getInstance("JCEKS");
			KStore.load(kfile, args[3].toCharArray());
			// ----------------------------------------------------FASE2--------------------------------------------------------------------------//

			// Input
			in = new ObjectInputStream(echoSocket.getInputStream());
			// Output
			out = new ObjectOutputStream(echoSocket.getOutputStream());
			// LocalUserID
			String userID = args[4];

			Scanner sc = new Scanner(System.in);

			// Utilizador corrente
			User user = new User(userID);

			// Enviar para server dados de autentificacao
			out.writeObject(userID);

			// caso contrario
			//// receber nonce
			// cifrar nonce
			// enviar nonce cifrado
			Long nonce = (Long) in.readObject();

			// Estah certo????
			Certificate cert = KStore.getCertificate(userID);
			char[] newPass = args[3].toCharArray();
			PrivateKey privateKey = (PrivateKey) KStore.getKey(userID, newPass);

			Signature s = Signature.getInstance("MD5withRSA");
			s.initSign(privateKey);
			byte buf[] = ByteBuffer.allocate(Long.BYTES).putLong(nonce).array();
			s.update(buf);

			out.writeObject(s.sign());

			// se receber flag
			// receber nonce
			// cifrar nonce chave privada
			// enviar nonce, nonce cifrado e certificado

			// se true recebe flag, por isso nao esta autenticado
			if ((boolean) in.readObject()) {
				out.writeObject(nonce);
				out.writeObject(cert);
			}

			// Se o utilizador nao esta registado fecha se o programa
			if (in.readObject().equals("close"))
				System.exit(0);

			Boolean quit = false;
			// Enquanto o cliente quiser fazer operacoes
			while (!quit) {
				System.out.print(">>>");
				// Lê o comando
				String comando = sc.nextLine();
				out.writeObject(comando);
				out.writeObject(userID);
				// Divide por espaco
				String[] parsela = comando.split(" ");
				// Vai ver qual o comando escrito
				if (parsela[0].equals("c")) {
					gerarChaveSimetrica(parsela[1], out);
					File file = new File("SecureClient\\" + "ChaveGrupo" + parsela[1] + ".key");
					System.out.println((String) in.readObject());
				} else if (parsela[0].equals("a")) {
					gerarChaveUser(parsela[2],out);
					System.out.println((String) in.readObject());
				} else if (parsela[0].equals("r")) {
					gerarChaveUser(parsela[2],out);
					System.out.println((String) in.readObject());
				} else if (parsela[0].equals("g")) {
					System.out.println((String) in.readObject());
				} else if (parsela[0].equals("u")) {
					System.out.println((String) in.readObject());
				} else if (parsela[0].equals("m")) {



					String msg = aux(parsela);
					enviaMensagemCifrada(msg, parsela[1], out, user);
					System.out.println((String) in.readObject());


				} else if (parsela[0].equals("p")) {
					File fotoFile = new File(parsela[2]);
					enviaFotoCifrada(fotoFile, parsela[1], out,user);
					//out.writeObject(content);
					System.out.println((String) in.readObject());
					// server.receiveFile(parsela[1]);
					// server.photo(parsela[1], parsela[2], user);
				} else if (parsela[0].equals("co")) {
					// server.collect(parsela[1], user);
					System.out.println((String) in.readObject());
				} else if (parsela[0].equals("h")) {
					System.out.println((String) in.readObject());
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

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void gerarChaveSimetrica(String grupoID, ObjectOutputStream out) {
		KeyGenerator kg;
		try {
			kg = KeyGenerator.getInstance("AES");
			kg.init(128);
			SecretKey chaveGrupo = kg.generateKey();
			PublicKey ku = TStore.getCertificate("Mequie").getPublicKey();
			Cipher ci = Cipher.getInstance("RSA");
			ci.init(Cipher.WRAP_MODE, ku);
			byte[] wrappedKey = ci.wrap(chaveGrupo);

			FileOutputStream kos = new FileOutputStream("SecureClient\\" + "ChaveGrupo" + grupoID + ".key");
			ObjectOutputStream oos = new ObjectOutputStream(kos);
			oos.writeObject(wrappedKey);
			oos.close();
			kos.close();
			File file = new File("SecureClient\\" + "ChaveGrupo" + grupoID + ".key");
			byte[] content = Files.readAllBytes(file.toPath());
			out.writeObject(content);
			file.delete();

		} catch (IOException | IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException
				| KeyStoreException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	///////////////////////////////// Cifrar chaves com as chaves de todos os
	///////////////////////////////// elementos
	public static void gerarChaveUser(String grupoId, ObjectOutputStream out) {

		KeyGenerator kg;
		try {
			kg = KeyGenerator.getInstance("AES");
			kg.init(128);
			SecretKey chaveGrupo = kg.generateKey();

			//Recebe todos os elementos do grupo, incluindo no novo elemento
			String userIds = (String) in.readObject();
			String userDivididos[] = userIds.split(",");
			Cipher ci = Cipher.getInstance("RSA");
			byte[] wrappedKey = null ;
			for (int i = 0; i < userDivididos.length; i++) {
				if( userDivididos[i]!=null){
				File file = new File("PubKeys\\" + userDivididos[i] + ".cert");
				CertificateFactory cf;
				cf = CertificateFactory.getInstance("X509");
				Certificate cert = cf.generateCertificate(new FileInputStream(file));
				ci.init(Cipher.WRAP_MODE, cert.getPublicKey());
				wrappedKey = ci.wrap(chaveGrupo);
				//Sera assim???????
				FileOutputStream kos = new FileOutputStream("SecureClient\\" +  userDivididos[i] + ".key");
				ObjectOutputStream oos = new ObjectOutputStream(kos);
				oos.writeObject(wrappedKey);
				oos.close();
				kos.close();
				File f = new File("SecureClient\\" + userDivididos[i] + ".key"); ////Why here????
				byte[] content = Files.readAllBytes(f.toPath());
				out.writeObject(content);
				f.delete();
				}
				
			}
		} catch (IOException | IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException
				| NoSuchAlgorithmException | ClassNotFoundException | CertificateException e) {
			e.printStackTrace();
		}

	}

	public static void enviaMensagemCifrada(String msg, String grupoId, ObjectOutputStream out, User user) {
		byte[] keyBytes;
		try {
			keyBytes = (byte[]) in.readObject();

			// decifrar key
			Cipher cii = Cipher.getInstance("RSA");
			Key unWrappedKey;
			PrivateKey privateKey = (PrivateKey) KStore.getKey(user.getNome(), user.getNome().toCharArray());
			cii.init(Cipher.UNWRAP_MODE, privateKey);
			unWrappedKey = cii.unwrap(keyBytes, "AES" , Cipher.PRIVATE_KEY);
			
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, unWrappedKey);

			byte[] input = msg.getBytes();
			byte encrypted[] = c.doFinal(input);
			out.writeObject(encrypted);

		} catch ( NoSuchPaddingException | IOException | NoSuchAlgorithmException 
				| InvalidKeyException | IllegalBlockSizeException | BadPaddingException |ClassNotFoundException
				| UnrecoverableKeyException | KeyStoreException e) {
			e.printStackTrace();
		}
	}


	public static void enviaFotoCifrada(File foto, String grupoId, ObjectOutputStream out,User user) {

		byte[] keyBytes;
		try {


			keyBytes = (byte[]) in.readObject();
			String[] userIds = (String[]) in.readObject();

			// decifrar key
			Cipher cii = Cipher.getInstance("RSA");
			Key unWrappedKey;
			PrivateKey privateKey = (PrivateKey) KStore.getKey(user.getNome(), user.getNome().toCharArray());
			cii.init(Cipher.UNWRAP_MODE, privateKey);
			unWrappedKey = cii.unwrap(keyBytes, "AES" , Cipher.PRIVATE_KEY);
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, unWrappedKey);

/////////////
			FileInputStream fis;
			FileOutputStream fos;
			CipherOutputStream cos;
			
			fis = new FileInputStream(foto);
			fos = new FileOutputStream("foto.cif");
		
			cos = new CipherOutputStream(fos, c);
			byte[] b = new byte[16];
			int i = fis.read(b);
			while (i != -1) {
				cos.write(b, 0, i);
				i = fis.read(b);
			}

			byte[] bytes = new byte[16 * 1024];
			InputStream in = new FileInputStream(foto);
	
			int count;
			while ((count = in.read(bytes)) > 0) {
				System.out.println(count);
				out.write(bytes, 0, count);
			}
			
			fos.close();
			fis.close();
			cos.close();
			new File("foto.cif").delete();
			in.close();
		} catch (IOException | InvalidKeyException
				| NoSuchAlgorithmException | NoSuchPaddingException | ClassNotFoundException | UnrecoverableKeyException
				| KeyStoreException e) {
			e.printStackTrace();
		}
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
}
