import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.TimerTask;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ChoseWord extends TimerTask {
    private SecretWord secret_word;
    private ArrayList<String> dictionary; 
    private ArrayList<User> users;
    


    public ChoseWord(ArrayList<String> dictionary, ArrayList<User> users, SecretWord secret_word) {
        this.dictionary = dictionary;
        this.users = users;
        this.secret_word = secret_word;
        
        
    }

    public void run(){
        
        chose_word(dictionary); //scelta della parola dal dizionario
        now_can_play_again(users); //la parola è cambiata, tutti gli utenti possono rigiocare
        try {
            backup_server_users(users); //aggiorno il file json
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    
    //metodo per scegliere la parola in modo randomico dal dizionario
    public void chose_word(ArrayList<String> dictionary){ 
        String[] words = dictionary.toArray(new String[0]);
        Random rand = new Random();
        String selectedWord = words[rand.nextInt(words.length)];
        secret_word.setSecret_word(selectedWord);
        System.out.println(secret_word.getSecret_word());
    }

    //metodo per permettere a tutti gli utenti di giocare la nuova parola
    public void now_can_play_again(ArrayList<User> users_list){
        Iterator<User> iter = users_list.iterator();
        while(iter.hasNext()){
            User user = iter.next();
            user.setHas_played(false);
        }
    }

    //metodo per aggiornare il file json
    public synchronized void backup_server_users(ArrayList<User> users_list) throws IOException{
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File users_file = new File("users.json");
        FileWriter fw = new FileWriter(users_file);
        String s_json = gson.toJson(users_list);
        fw.write(s_json);
        fw.close();
    }
}
