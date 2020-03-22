import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
//Servidor MequieServer
public class MequieServer {
   //Grupo geral
   private Grupo geral = new Grupo("geral");
   //Todos os grupos criados
   private Map<String,Grupo> grupos = new HashMap<String,Grupo>();

   //Output
   private ObjectOutputStream outStream;
   //Input
   private ObjectInputStream inStream;

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
               outStream = new ObjectOutputStream(socket.getOutputStream());
               //Input
               inStream = new ObjectInputStream(socket.getInputStream());
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

               //Recupera grupos
               loadGrupos();
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
                   //caso nao for completamente igual, significa que ou o user ou a password esta mal
                   //entao vamos verificar se o user ja existe
                   }else if(dup[0].equals(nomeUser) && !dup[1].equals(passwd)){
                       System.out.println("Password incorreta");
                       programa = "close";
                       outStream.writeObject("close");
                       scanFile.close();
                       System.exit(0);
                   }

               }

               //Se utilizador nao existe
               if(!exists){
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
                   scanFile.close();
                   System.exit(0);
               }

               //Fechar o programa
               outStream.writeObject(programa);
               if(programa.equals("close")){
                   scanFile.close();
                   System.exit(0);
               }
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
   public void create(String grupoId, User owner) throws IOException,FileNotFoundException{
     loadGrupos();
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
       //new File(grupoId +"//mensagens").mkdir();
       //Cria um ficheiro para mensagens não vistas por todos
       createFiles(grupoId +"//mensagens.txt");
       //Cria um ficheiro com as mensagens vistas por todos
       createFiles(grupoId + "//historico.txt");
       //Cria uma pasta para fotos
       new File(grupoId +"//fotos").mkdir();
       //Cria ficheiro que guarda owner
       createFiles(grupoId +"//owner.txt");
       //Escreve owner no ficheiro
       writeStringFile(owner.getNome(),grupoId+"//owner.txt");
       //Cria ficheiro que guarda utilizadores
       createFiles(grupoId +"//utilizadores.txt");
       //Escreve o owner no ficheiro, pois o mesmo e um utilizador
       writeStringFile(owner.getNome(),grupoId+"//utilizadores.txt");

       System.out.println(owner.getNome() + " criou um grupo: " + grupoId);
   }

   //Adiciona utilizador
   public void addUtilizador(String userId, String grupoId, User user) throws IOException, FileNotFoundException {
     loadGrupos();
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
   public void remover(String userId, String grupoId, User user) throws IOException, FileNotFoundException {
     loadGrupos();
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
       writeStringFile("", grupoId+"//utilizadores.txt");
       System.out.println("Utilizador " + userId + " foi removido do grupo");
       System.out.println(gp.getUsers());
   }

   //Informacao sobre um grupo
   public void ginfo(String grupoId, User user) throws FileNotFoundException{
     loadGrupos();

       //Grupo nao existe
       if(!grupos.containsKey(grupoId)){
           System.out.println("Grupo nao existe");
           System.exit(0);
       }

       //Vai buscar o grupo
       Grupo gp = grupos.get(grupoId);
       //Imprime informacoes
       System.out.println("Nome do grupo: "+ grupoId);
       System.out.println("Dono: " + gp.getOwner().getNome());
       System.out.println("Numero de utilizadores: " + gp.getUsers().size());
       if(gp.getOwner().getNome().equals(user.getNome())){
           System.out.print("Utilizadores: ");
           System.out.println(gp.getUsers());
       }
   }

   //informacoes sobre o utilizador
   public void uinfo(User user) throws FileNotFoundException{
     loadGrupos();
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
           if(entry.getValue().getOwner().getNome().equals(user.getNome())){
               gruposOwner.add(entry.getKey());
           }
       }
       System.out.println("O utilizador e dono dos grupos: " + gruposOwner.toString());
       System.out.println("O utilizador pertence aos grupos: " + gruposPertence.toString());
   }

   public void msg(String grupoId, String mensagem, User user) throws FileNotFoundException, IOException{
     loadGrupos();
     //Grupo nao existe
     if(!grupos.containsKey(grupoId)){
         System.out.println("Grupo nao existe");
         System.exit(0);
     }
     //Vai buscar o grupo
      Grupo gp = grupos.get(grupoId);
     //Utilizador nao pertence ao grupo
     if(!gp.containsUser(user.getNome())){
         System.out.println("Utilizador nao pertence ao grupo");
         System.exit(0);
     }
     //Cria mensagem
     Mensagem msg = new Mensagem(mensagem, user.getNome());
     //Adiciona mensagem ao grupo
     gp.addMsg(msg);
     //Escreve no ficheiro de mensagens do grupo a mensagem
     writeStringFile(mensagem,grupoId+"/mensagens.txt");
     System.out.println("Mensagem foi enviada");
   }
   
   public void photo(String grupoId, String fotoName, User user) throws FileNotFoundException{
    loadGrupos();
     //Grupo nao existe
     if(!grupos.containsKey(grupoId)){
         System.out.println("Grupo nao existe");
         System.exit(0);
     }
     //Vai buscar o grupo
      Grupo gp = grupos.get(grupoId);
     //Utilizador nao pertence ao grupo
     if(!gp.containsUser(user.getNome())){
         System.out.println("Utilizador nao pertence ao grupo");
         System.exit(0);
     }
     //SERA QUE E SO ASSIM?
     //Cria foto
     Photo foto = new Photo(fotoName, user.getNome());
     //Adiciona foto ao grupo
     gp.addFoto(foto);
   }
   

    public void collect(String grupoId, User user) throws IOException, FileNotFoundException{
        loadGrupos();
        Grupo gp = grupos.get(grupoId);
        List<Mensagem> msgs = gp.msgNaoVistasPor(user);
        //List<Photo> fotos = gp.fotosNaoVistasPor(user);
        for(Mensagem msg : msgs){
            System.out.println(msg.getStringMsg());
            msg.addVisto(user.getNome());

            if(gp.vistoPorTodos(msg)){
                writeStringFile(msg.getStringMsg(), grupoId + "/historico.txt" );
                gp.removeMsg(msg);
                removeUserFile(msg.getStringMsg(), grupoId + "/mensagens.txt");
            }
        }
        /*for(Photo foto : fotos){
            //System.out.println() ???? ou enviar ficheiro de volta ????
            foto.addVisto(user.getNome());
            if(gp.photoVistoPorTodos(foto)){
                gp.removeFoto(foto);
            }
        } */
    }

   public void history(String grupoId, User user) throws FileNotFoundException{
    loadGrupos();
     //Grupo nao existe
     if(!grupos.containsKey(grupoId)){
         System.out.println("Grupo nao existe");
         System.exit(0);
     }
     //Vai buscar o grupo
      Grupo gp = grupos.get(grupoId);
     //Utilizador nao pertence ao grupo
     if(!gp.containsUser(user.getNome())){
         System.out.println("Utilizador nao pertence ao grupo");
         System.exit(0);
     }
     Scanner historicoFile = new Scanner(new File(grupoId + "/historico.txt"));
     //Linha corrente do ficheiro historico.txt
     String current;
     //Enquanto ha grupos
     while(historicoFile.hasNextLine()){
       current = historicoFile.nextLine();
       System.out.println(current);
     }
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
         gp.addUser(currentUt);
       }
       loadMensagens(novo);
       ownerFile.close();
       utilizadoresFile.close();
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

   public void receiveFile(String grupoId) throws IOException, ClassNotFoundException {
    File dest = new File(grupoId +"/fotos");
    byte[] content = (byte[]) inStream.readObject();
    Files.write(dest.toPath(),content);
    }
}
