package ca.polymtl.inf4410.tp1.shared;

public class Fichier {

    private String nom;
    private byte[] contenu;
    private byte[] lockBy;

    public Fichier(String n){
        this(n, null);
    }

    public Fichier(String n, byte[] c) {
        this.nom = n;
        this.contenu = c;
        this.lockBy = null;
    }

    public String getNom() {
        return this.nom;
    }

    public void setNom(String n) {
        this.nom = n;
    }

    public byte[] getContenu() {
        return this.contenu;
    }

    public void setContenu(byte[] c) {
        this.contenu = c;
    }

    public byte[] getLock(){
        return this.lockBy;
    }

    public void lock(byte[] idClient) {
        this.lockBy = idClient;
    }

    public void unlock() {
        this.lockBy = null;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Fichier{" +
                "nom='" + nom + '\'' +
                ", contenu=" + java.util.Arrays.toString(contenu) +
                ", lockBy=" + java.util.Arrays.toString(lockBy) +
                '}';
    }
}
