
//per compilare javac -cp "./../libs/gson-2.10.jar" ./*.java -d ./../bin
//per seguire java -cp .:./../libs/gson-2.10.jar WordleServerMain



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.reflect.*;
import java.lang.reflect.*;

public class WordleServerMain {

    public static int port; //porta di connessione con il client
    public static int timer_word;
    public static String host;
    public static int port_2; 
    public static final String configFile = "server.properties";
    public static SecretWord secret_class = new SecretWord();
    public static void main(String[] args) throws IOException {

        readConfig(); //metodo per leggere i parametri di configurazione da server.properties

        System.out.println("Server aperto sulla porta " + port);

        ArrayList<User> users = new ArrayList<User>(); //arraylist per contenere gli utenti
        File usersFile = new File("./users.json");
        if(usersFile.exists()){
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader(usersFile));
            Type list_user_type = new TypeToken<ArrayList<User>>() {}.getType();
            users = gson.fromJson(reader,list_user_type);
        }else{
            usersFile.createNewFile();
        }

        File f = new File("./words.txt");
        ArrayList<String> dictionary = create_dictionary(f); //arraylist per contenere tutte le parole del gioco
        

        Timer timer = new Timer();
        long period = timer_word;
        timer.scheduleAtFixedRate(new ChoseWord(dictionary, users, secret_class), 0, period); //funzione che permette di eseguire ChoseWord ogni tot periodo di tempo
        
        for (User user : users) {
            user.setIs_logged(false);
        }

        //threadpool per gestire le connessioni
        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket server = new ServerSocket(port)){
            while(true){  
                Socket socket = server.accept();
                System.out.println("Client connected");
                //per ogni client connesso crea un WordleTask che si occupa delle sue richieste
                pool.execute(new WordleTask(secret_class, dictionary, socket, users, host, port_2)); 
                // Tutta la comunicazione è gestita dalla classe WordleTask
            }
            
    
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    //metodo che permette di leggere i parametri di configurazioen
    public static void readConfig() throws FileNotFoundException, IOException {
		FileInputStream fis = new FileInputStream( "./server.properties" );
        Properties prop = new Properties();
        prop.load(fis);
        port = Integer.parseInt(prop.getProperty("port"));
        timer_word = Integer.parseInt(prop.getProperty("timer"));
        host = prop.getProperty("host");
        port_2 = Integer.parseInt(prop.getProperty("port_2"));
        fis.close();
	}

    //metodo per creare l'arraylist di parole dato un file contenente quest'ultime
    public static ArrayList<String> create_dictionary(File f) throws FileNotFoundException{
        Scanner s = new Scanner(f);
        ArrayList<String> dictionary = new ArrayList<String>();
        while (s.hasNext()){
            dictionary.add(s.next());
        }
        s.close();
        return dictionary;
    }

    //metodo per restituire una parola prersa casualmente dall'arraylist contenente tutte le parole
    public static String chose_word(ArrayList<String> dictionary){
        String[] words = dictionary.toArray(new String[0]);
        Random rand = new Random();
        return words[rand.nextInt(words.length)];
    }

}
