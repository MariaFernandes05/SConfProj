import java.io.File;
import java.util.*;
//Grupos do Mequie

public class Grupo {
    //Id do grupo
    public String id = "";
    //Dono do grupo
    public User owner;
    //Lista de utilizadores do grupo
    public List<String> users;
    //Lista de mensagens do grupo
    public List<Mensagem> msgs;
    //Lista de fotos do grupo
    public List<Photo> fotos; 
    //public List<int> linhasJaLidas; 

    public int identificadorAtual;


    private Map<User, Map<Integer, File>> chavesGrupoUtilizadores;

    //Construtor so com o id do grupo
    public Grupo(String id){
        this.identificadorAtual =0;
        this.id = id;
        this.users = new ArrayList<String>();
        this.msgs  = new ArrayList<Mensagem>();
        this.fotos = new ArrayList<Photo>();
        this.chavesGrupoUtilizadores = new HashMap<User, Map<Integer, File>>();
    }

    //Construtor com o id e o dono do grupo
    public Grupo(String id,User owner){
        this.identificadorAtual =0;
        this.id = id;
        this.owner = owner;
        this.users = new ArrayList<String>();
        this.msgs  = new ArrayList<Mensagem>();
        this.fotos = new ArrayList<Photo>();
        users.add(owner.getNome());
        this.chavesGrupoUtilizadores = new HashMap<User, Map<Integer, File>>();
    }


    //Adiciona um utilizador
    //return -1 --- caso deia erro
    public int addUser(String idUser){
      this.identificadorAtual ++;
      if(users.contains(idUser)){
            return -1;
      }
        users.add(idUser);
        return 0;
    }
///////////////////////fase2


    public int getIdentificador(){
        return this.identificadorAtual;
    }

    public Map<User, Map<Integer, File>> getMapUtEChaves(){
        return this.chavesGrupoUtilizadores;
    }


    public void addUserToMap(User user){
        Map<Integer, File> iF = new HashMap<>();
        this.chavesGrupoUtilizadores.put(user, iF);
    }

    public void addChaveToUserMap(User user,int ind, File chave){
        if(this.chavesGrupoUtilizadores.containsKey(user))
            this.chavesGrupoUtilizadores.get(user).put(ind, chave);          
    }


/////////////////////////////////////
    //Vê se o utilizador pertence ao grupo
    public boolean containsUser(String userId){
        return users.contains(userId);
    }

    //Vê se o utilizador e o dono
    public boolean isOwner(User idUser){
        return idUser.equals(this.owner);
    }

    //Retorna o dono do grupo
    public User getOwner(){
        return this.owner;
    }

    //Retorna o id do grupo
    public String getId(){
        return this.id;
    }

    //Retorna os utilizadores do grupo
    public List<String> getUsers(){
        return this.users;
    }

    //Remove um utilizador do grupo
    public void remove(String userId){
        this.users.remove(userId);
    }

    //Adiciona uma mensagem
    public void addMsg(Mensagem msg){
        this.msgs.add(msg);
    }

    //Adiciona uma foto
    public void addFoto(Photo foto){
        this.fotos.add(foto);
    }

    public List<Mensagem> msgVistasPor(User user){
        List<Mensagem> vistas = new ArrayList<>();
        for(Mensagem msg : this.msgs){
            if(msg.userViu(user.getNome()))
                vistas.add(msg);
        }
        return vistas;
    }

    public List<Mensagem> msgNaoVistasPor(User user){
        List<Mensagem> naoVistas = new ArrayList<>();
        for(Mensagem msg : this.msgs){
            if(!msg.userViu(user.getNome()))
                naoVistas.add(msg);
        }
        return naoVistas;
    }

    public void removeMsg(Mensagem msg){
        this.msgs.remove(msg);
    }
    
    public Boolean vistoPorTodos(Mensagem msg){
        boolean visto = true;
        for(String user: this.users)
            if(!msg.userViu(user))
                visto = false;
        return visto;
    }



    /*public void removeFoto(Photo foto){
        this.fotos.remove(foto);
    }
    
    
    public Boolean photoVistoPorTodos(Photo foto){
        boolean visto = true;
        for(String user: this.users)
            if(!foto.userViu(user))
                visto = false;
        return visto;
    }*/

    
}
