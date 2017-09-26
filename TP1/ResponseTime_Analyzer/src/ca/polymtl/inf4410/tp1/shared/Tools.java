package ca.polymtl.inf4410.tp1.shared;

import java.security.MessageDigest;
import java.io.InputStream;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;


public class Tools {

    public Tools() {

    }

    public byte[] checksum(String fichier) {
        byte[] d = null;
        try {
            Path file = Paths.get(Paths.get("").toAbsolutePath().toString() + "/" + fichier);
            byte[] b = Files.readAllBytes(file);
            d = MessageDigest.getInstance("MD5").digest(b);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return d;
    }
}
