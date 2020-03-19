import java.io.*;
import java.net.*;
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

    //Construtor so com o id do grupo
    public Grupo(String id){
        this.id = id;
        users = new ArrayList<String>();
        msgs  = new ArrayList<Mensagem>();
        fotos = new ArrayList<Photo>();
    }

    //Construtor com o id e o dono do grupo
    public Grupo(String id,User owner){
        this.id = id;
        this.owner = owner;
        users = new ArrayList<String>();
        msgs  = new ArrayList<Mensagem>();
        fotos = new ArrayList<Photo>();
        users.add(owner.getNome());
    }

    //Adiciona um utilizador
    //return -1 --- caso deia erro
    public int addUser(String idUser){
      if(users.contains(idUser)){
            return -1;
      }
        users.add(idUser);
        return 0;
    }

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
}
