

//classe per poter condividere la secret word alle altre classi che la richiedono
public class SecretWord {
    private String secret_word;
   

    public SecretWord() {
        this.secret_word = "";
    }

    public String getSecret_word() {
        return secret_word;
    }


    public void setSecret_word(String secret_word) {
        this.secret_word = secret_word;
    }




}
