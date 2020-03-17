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

//User do Mequie

public class User {
    //public List<Grupo> gOwn;
    //public List<Grupo> gPert;
    public String nome = "";
    public String password = "";

    //Construtor
    public User(String nome,String password){
        this.nome = nome;
        this.password = password;
        //this.gOwn = new ArrayList<Grupo>();
        //this.gPert = new ArrayList<Grupo>();
    }

    //Retorna o nome do user
    public String getNome(){
        return this.nome;
    }

    //Retorna a password do user
    public String getPassword(){
        return this.password;
    }
/*
    public Grupo[] getGruposOwner(){
        return this.gOwn;
    }

    public Grupo[] getGruposPertence(){
        return this.gPert;
    }

    public void addGO(Grupo grupo){
        this.gOwn.add(grupo);
    }

    public void addGP(Grupo grupo){
        this.gPert.add(grupo);
    }

    public void remove(Grupo grupo){
        this.gPert.remove(grupo);
    }*/
}