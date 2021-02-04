import java.io.*;
import java.util.*;

public class Photo {
    //Mensagem
    public File foto;
    //Utilizadores que viram mensagem
    public List<String> vistoPor;

    public Photo(String fotoName, String user){
        this.foto= new File(fotoName);
        vistoPor = new ArrayList<String>();
        vistoPor.add(user);
    }

    //User viu a foto
    public void addVisto(String userName){
        vistoPor.add(userName);
    }

    //Ve se o user ja viu a foto
    public boolean userViu(String userName){
        return vistoPor.contains(userName);
    }

    /*
    public void addVisto(String userName){
        vistoPor.add(userName);
    }
    */

}