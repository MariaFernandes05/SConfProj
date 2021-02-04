import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.*;

public class Cifra {

    public static void main(String[] args) throws Exception {

    //gerar uma chave aleatória para utilizar com o AES
    KeyGenerator kg = KeyGenerator.getInstance("AES");
    kg.init(128);
    SecretKey key = kg.generateKey();

    Cipher c = Cipher.getInstance("AES");
    c.init(Cipher.ENCRYPT_MODE, key);

    FileInputStream fis = new FileInputStream("a.txt");
 
    FileOutputStream fos;
    CipherOutputStream cos;
    
    fis = new FileInputStream("a.txt");
    fos = new FileOutputStream("a.cif");

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

    FileInputStream kfile = new FileInputStream("key/myKeys");
    KeyStore kstore = KeyStore.getInstance("JCEKS");
    kstore.load(kfile, "plshelp".toCharArray());
    Certificate cert = kstore.getCertificate("keyRSA"); 

    PublicKey ku = cert.getPublicKey();
    Cipher ci = Cipher.getInstance("RSA");
    ci.init(Cipher.WRAP_MODE, ku);

    byte[] wrappedKey = ci.wrap(key);


    FileOutputStream kos = new FileOutputStream("a.key");
    ObjectOutputStream oos = new ObjectOutputStream(kos);
    oos.writeObject(wrappedKey);
    oos.close();
    kos.close();
    kfile.close();


 //DESCODIFICAR


    CipherInputStream cis;
    FileInputStream fis_key;
    FileInputStream fis_cif;
    FileOutputStream teste;


    fis_key = new FileInputStream("a.key");
    fis_cif = new FileInputStream("a.cif");
    teste = new FileOutputStream("teste.txt");

    ObjectInputStream oiso = new ObjectInputStream(fis_key);

    byte[] nwrappedKey = (byte[]) oiso.readObject();

    Key kr = kstore.getKey("keyRSA", "plshelp".toCharArray());
    
    Cipher cii = Cipher.getInstance("RSA");
    cii.init(Cipher.UNWRAP_MODE, kr);

    Key unWrappedKey = cii.unwrap(nwrappedKey, "AES" , Cipher.SECRET_KEY);

    Cipher ciii = Cipher.getInstance("AES");
    ciii.init(Cipher.DECRYPT_MODE, unWrappedKey);    //SecretKeySpec é subclasse de secretKey

    cis = new CipherInputStream(fis_cif, ciii);

    byte[] bi = new byte[16];  
    int j = cis.read(bi);
    while (j != -1) {
        teste.write(bi, 0, j);
        j = cis.read(bi);
    }

    oiso.close();
    cis.close();
    fis_key.close();
    fis_cif.close();
    teste.close();



    }
}

