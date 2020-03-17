import java.io.*;
import java.net.*;
import java.util.*;

//Servidor MequieServer
public class MequieServer {
   //Grupo geral
   private Grupo geral = new Grupo("geral");
   //Todos os grupos criados
   private Map<String,Grupo> grupos = new HashMap<String,Grupo>();

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
       }
       public void run(){
           try {
               //Output
               ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
               //Input
               ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
               //Credenciais a preencher
               String nomeUser = null;
               String passwd = null;
               //Ficheiro com os utilizadores registados
               File file = new File("users.txt");
               try {
                   //Lê do cliente
                   nomeUser = (String)inStream.readObject();
                   passwd = (String)inStream.readObject();
               }catch (ClassNotFoundException e1) {
                   e1.printStackTrace();
               }
               //Constroi string do tipo <use>:<pass>
               StringBuilder sb = new StringBuilder();
               sb.append(nomeUser);
               sb.append(":");
               sb.append(passwd);
               System.out.println(sb.toString());
               //Faz scan do file
               Scanner scanFile = new Scanner(file);
               String stringFile;
               Boolean exists = false;
               //String indica se o programa acaba
               String programa = "open";
               String aux= "";
               String[] dup;

               //Recuperar users
               while(scanFile.hasNextLine()){
                   stringFile = scanFile.nextLine();
                   aux=stringFile;
                   dup = aux.split(":");
                   //Regista o utilizador no geral
                   geral.addUser(dup[0]);
               }

               //Repõe o scanner no inicio do ficheiro
               scanFile = new Scanner(file);

               //Enquanto o ficheiro tem texto
               while(scanFile.hasNextLine()){
                   //Vai buscar a proxima linha
                   stringFile = scanFile.nextLine();
                   //Se utilizador existe
                   aux=stringFile;
                   dup = aux.split(":");
                   //se encontrarmos uma linha com user e password correta, autentica
                   if(dup[0].equals(nomeUser) && dup[1].equals(passwd)){
                       exists =true;
                       System.out.println("O utilizador foi autenticado");
                       menu();
                   //caso nao for completamente igual, significa que ou o user ou a password esa mal
                   //entao vamos verificar se o user ja existe
                   }else if(dup[0].equals(nomeUser) && !dup[1].equals(passwd)){
                       System.out.println("Password incorreta");
                       programa = "close";
                       outStream.writeObject("close");
                       System.exit(0);
                   }
               }

               //Se utilizador nao existe
               if(!exists){
                   //Cria um novo utilizador
                   //User user = new User(nomeUser,passwd);
                   //Regista o utilizador no geral
                   geral.addUser(nomeUser);
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
               scanFile.close();

               //Fechar socket
               socket.close();

           } catch (IOException e) {
               e.printStackTrace();
           }
       }
   }

   //Dispõe os comandos válidos ao cliente
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

   //Cria um novo grupo
   public void create(String grupoId, User owner) throws IOException{
       //Grupo ja existe
       if(grupos.containsKey(grupoId)){
           System.out.println("Grupo ja existe");
           System.exit(0);
       }
       //Guardar os grupos criados num ficheiro
       File gruposFile = new File("grupos.txt");
       Writer write = new BufferedWriter(new FileWriter(gruposFile, true));
       write.append(grupoId + "\n");
       write.close();

       //Novo grupo
       Grupo novo = new Grupo(grupoId,owner);
       //Adiciona ao map
       grupos.put(grupoId,novo);
       //Cria uma pasta que vai conter as informacoes do grupo
       new File(grupoId).mkdir();
       //Cria ficherio que guarda mensagens
       createFiles(grupoId +"//mensagens.txt");
       //Cria ficherio que guarda owner
       createFiles(grupoId +"//owner.txt");
       //Escreve owner no ficheiro
       writeStringFile(owner.getNome(),grupoId+"//owner.txt");
       //Cria ficherio que guarda utilizadores
       createFiles(grupoId +"//utilizadores.txt");
       //Escreve o owner no ficheiro, pois o mesmo e um utilizador
       writeStringFile(owner.getNome(),grupoId+"//utilizadores.txt");

       System.out.println(owner.getNome() + " criou um grupo: " + grupoId);
   }

   //Adiciona utilizador
   public void addUtilizador(String userId, String grupoId, User user) throws IOException {
       //Grupo nao existe
       if(!grupos.containsKey(grupoId)){
           System.out.println("Grupo nao existe");
           System.exit(0);
       }
       //Vai se buscar o grupo
       Grupo gp = grupos.get(grupoId);
       //O user não e o dono do grupo
       if(!gp.getOwner().getNome().equals(user.getNome())){
           System.out.println("Utilizador nao e o owner do grupo");
           System.exit(0);
       }
       //O utilizador ja pertence ao grupo
       if(gp.addUser(userId) == -1){
           System.out.println("Utilizador ja pertence ao grupo");
           System.exit(0);
       }

       //Escrever na pasta do grupo
       writeStringFile(userId, grupoId + "//utilizadores.txt");
       System.out.println("Utilizador " + userId + " foi adicionado ao grupo");
       System.out.println(gp.getUsers());
   }

   //Remover um utilizador
   public void remover(String userId, String grupoId, User user) throws IOException {
     //Grupo nao existe
       if(!grupos.containsKey(grupoId)){
           System.out.println("Grupo nao existe");
           System.exit(0);
       }
      //Vai buscar o grupo
       Grupo gp = grupos.get(grupoId);
       //O user nao e o owner
       if(!gp.getOwner().getNome().equals(user.getNome())){
           System.out.println("Utilizador nao e o owner do grupo");
           System.exit(0);
       }
       //Utilizador nao pertence ao grupo
       if(!gp.containsUser(userId)){
           System.out.println("Utilizador nao pertence ao grupo");
           System.exit(0);
       }
       //Remove utilizador
       gp.remove(userId);
       //Remover utilizador da pasta
       removeUserFile(userId,grupoId+"//utilizadores.txt");
       System.out.println(gp.getUsers());
   }

   //Informacao sobre um grupo
   public void ginfo(String grupoId, User user){
       //GRupo nao existe
       if(!grupos.containsKey(grupoId)){
           System.out.println("Grupo nao existe");
           System.exit(0);
       }
       //Vai buscar o grupo
       Grupo gp = grupos.get(grupoId);
       //Imprime informacoes
       System.out.println("Dono: " + gp.getOwner().getNome());
       System.out.println("Numero de utilizadores: " + gp.getUsers().size());
       System.out.print("Utilizadores: ");
       if(gp.getOwner().getNome().equals(user.getNome())){
           System.out.println(gp.getUsers());
       }
   }

   //informacoes sobre o utilizador
   public void uinfo(User user){
       //Grupos de que o user e owner
       List<String> gruposOwner = new ArrayList<String>();
       //Grupos a que o user pertence
       List<String> gruposPertence = new ArrayList<String>();
       //Percorrer todos os grupos criados
       for (Map.Entry<String, Grupo> entry : grupos.entrySet()) {
          //Pertence ao grupo
           if(entry.getValue().containsUser(user.getNome())){
               gruposPertence.add(entry.getKey());
           }
           //E owner do grupo
           if(entry.getValue().getOwner().equals(user)){
               gruposOwner.add(entry.getKey());
           }
       }
       System.out.println("O utilizador e dono dos grupos: " + gruposOwner.toString());
       System.out.println("O utilizador pertence aos grupos: " + gruposPertence.toString());
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
   }
}