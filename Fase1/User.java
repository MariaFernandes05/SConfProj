//User do Mequie

public class User {
    public String nome = "";
    public String password = "";

    //Construtor
    public User(String nome,String password){
        this.nome = nome;
        this.password = password;
    }

    //Construtor so com nome
    public User(String nome){
      this.nome = nome;
    }
    //Retorna o nome do user
    public String getNome(){
        return this.nome;
    }

    //Retorna a password do user
    public String getPassword(){
        return this.password;
    }
}
