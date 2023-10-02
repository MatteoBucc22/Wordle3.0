import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class WordleTask implements Runnable {
    private static SecretWord secret_class;
    private static ArrayList<String> dictionary;
    private Socket socket;
    private ArrayList<User> users;
    private int port;
    private String host;
    

    public WordleTask(SecretWord secret_class, ArrayList<String> dictionary, Socket socket , ArrayList<User> users, String host, int port) {
        WordleTask.secret_class = secret_class;
        WordleTask.dictionary = dictionary;

        this.socket = socket;
        this.users = users;
        this.host = host;
        this.port = port;

        
    }
    public void run() {
       
        User user = new User(null, null);
        
        try{
            DataOutputStream dos = new DataOutputStream(this.socket.getOutputStream());
            DataInputStream dis = new DataInputStream(this.socket.getInputStream());

            while(true){

                int scelta_menu = dis.readInt();

                //srie di if per rispondere alle richieste nel momento in cui l'utente utilizza il primo menu
                if(scelta_menu==1){ //register

                    boolean registered = false;
                    while(!registered){

                        System.out.println("Register");

                        String username = dis.readUTF(); 

                        if (check(username, users) == true){ //username già esistente
                            dos.writeInt(0);
                            continue;
                        }else{ //username non esistente, si può andare avanti
                            dos.writeInt(1);
                            boolean accepted = false;
                            while(!accepted){

                                String password = dis.readUTF();
                                if(!password.equals("")){ //pasword valida 
                                    User new_user = new User(username, password); //creo nuovo utente
                                    users.add(new_user); //aggiungo utente alla lista di utenti
                                    backup_server_users(users); //salvo sul file json gli aggiornamneti
                                    accepted = true;
                                    registered = true;
                                }
                                else{ //password non valida
                                    continue;
                                }
                            }
                    
                        }
                    }

                }
                else if(scelta_menu==2){ //login
                    boolean logged = false;

                    while(!logged){
                        System.out.println("login");
                        String username = dis.readUTF();
                        if(check(username, users) == true){ //username esistente
                            
                            user = search_user(username, users); //recupero l'utente dalla lista
                            dos.writeInt(0);
                            String password = dis.readUTF();
                            if(user.password.equals(password)){ //la password è giusta

                                if(user.is_logged == true){ //utente già loggato
                                    dos.writeInt(2);
                                    continue;
                                }

                                System.out.println("utente: " + username + ", password corretta");
                                dos.writeInt(0);
                                logged = true;
                                user.setIs_logged(true);
                                boolean logout = false;
                                
                                //while per le azioni che un utente può svolgere mentre è loggato
                                while(logout == false){
                                
                                    backup_server_users(users);
                                    int action2 = dis.readInt();
                                    //secondo menu
                                    switch(action2){
                                        //play
                                        case 1:
                                            if(user.has_played){ //l'utente ha già giocato la parola corrente
                                                dos.writeInt(0);
                                            }else{ //l'utente non ha ancora giocato la parola corrente
                                                dos.writeInt(1);
                                                Long timestamp = System.currentTimeMillis();
                                                playWordle(user, dis, dos, secret_class.getSecret_word(), timestamp);
                                                backup_server_users(users); //aggiorno il file json dopo la partita
                                            }
                                            break;
                                        //statistiche
                                        case 2:
                                            seeStatistic(user, dos);
                                            break;
                                        //share
                                        case 3:
                                            if(user.has_played){ //l'utente ha giocato la parola corrente
                                                dos.writeInt(0);
                                                int attempts = 13 - dis.readInt();
                                                mess_sender(port, host, attempts, username);
                                            }else{ //l'utente non ha giocato la parola corrente
                                                dos.writeInt(1); //comunico al client che non posso inviare il messaggio perche o la parola è cambiata o ancora non ha giocato 
                                            }
                                            break;
                                        case 4: //avviene tutto lato client
                                            break;
                                        case 5: //logout
                                            user.setIs_logged(false);
                                            backup_server_users(users);
                                            logout = true;
                                            break;
                                    }
                                }
                            
                            }else{ //password errata
                                dos.writeInt(1);
                                continue;
                            }
                        
                            
                        }else{ //username non esistente
                            dos.writeInt(1);
                        }
                    }

                    
                }else if(scelta_menu == 3){
                    dis.close();
                    dos.close();
                    break;
                }
               
                
            }
        }catch(IOException e){
            
            if(user.getUsername() != null){
                user.setIs_logged(false);
                try {
                    backup_server_users(users);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            System.out.println("Client crash");
        }
    }

    //METODI UTILIZZATI DAL RUN
    //funzione per controllare se un username è registrato
    public synchronized boolean check(String username,ArrayList<User> users) {

        boolean exists = false;
        Iterator<User> iter = users.iterator();
        while(iter.hasNext()){
            User check_user = iter.next();
            if (check_user.username.equals(username)) exists = true;
        }
        return exists ;
    }

   //metodo per aggiornare il file users.json
    public synchronized void backup_server_users(ArrayList<User> users) throws IOException{
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File users_file = new File("users.json");
        FileWriter fw = new FileWriter(users_file);
        String s_json = gson.toJson(users);
        fw.write(s_json);
        fw.close();
    }

    //funzione per cercare utente 
    public synchronized User search_user(String username,ArrayList<User> users_list){
        Iterator<User> iter = users_list.iterator();
        while(iter.hasNext()){
            User user = iter.next();
            if (user.username.equals(username)){
                return user;
            }
        }
        return null;
    }


    public static void playWordle(User user, DataInputStream dis, DataOutputStream dos, String secret_word, Long timestamp ) throws IOException{ 
        boolean guessed = false;
        int attempts = 12;
        boolean timeout = false;
        while(!guessed && attempts > 0){ //finche la parola non è stat indovinata o non vengono esauriti i tentativi
            
            String try_word = dis.readUTF();
            
            if(!secret_word.equals(secret_class.getSecret_word())){ //se la parola cambia mentre si sta giocando
                dos.writeInt(3);
                user.setHas_played(false);
                timeout = true;
                break;
            }
            
            //controllo se la parola esiste
            else if(try_word.length() != 10) { //se la parola ha una lunghezza diversa da 10 lettere
                dos.writeInt(2);
                continue;
            }
            else if(check_dictionary(try_word, dictionary)){ //la parola esiste
                
                
                dos.writeInt(1);

                System.out.println(try_word);
                

                if(try_word.equals(secret_word)){ //parola indovinata
                    guessed = true;
                    update(user);
                    up_guess_distribution(user, 13 - attempts);
                    dos.writeInt(1);
                    break;

                }else{ //parola non indovinata
                    
                    dos.writeInt(0);
                    StringBuilder result = new StringBuilder();

                    //costruisco la sequenza di caratteri da restituire
                    for(int i = 0; i < 10; i++){
                        char charToGuess = secret_word.charAt(i);
                        char charGuessed = try_word.charAt(i);
                        

                        if (charToGuess == charGuessed) {
                            result.append('+'); // Carattere presente nella stessa posizione
                        } else if (secret_word.indexOf(charGuessed) != -1) {
                            result.append('?'); // Carattere presente ma in posizione diversa
                        } else {
                            result.append('X'); // Carattere non presente
                        }

                        
                    }
                    
                    
                    dos.writeUTF(result.toString());
                    attempts--;
                }

            }else{
                dos.writeInt(0); //parola non presente nel dizionario
            }
            
            
        }
        
        if(guessed == false && timeout == false ){ //partita persa
            update_lose_game(user);

        }
    }



    //metodo che legge le statistiche dell'utente e le passa al client
    public static void seeStatistic(User user, DataOutputStream dos) throws IOException{
        dos.writeUTF(user.getUsername());
        dos.writeBoolean(user.isHas_played());
        dos.writeInt(user.getGames_played());
        dos.writeInt(user.getGames_won());
        dos.writeInt(user.getWin_streak());
        dos.writeInt(user.getLongest_win_streak());
        HashMap<Integer, Integer> guess_distribution =  user.getGuess_distribution();


        for(int i = 1; i<=12; i++){
            dos.writeInt(guess_distribution.get(i));
        }
    }

    //metodo che controlla se la parola è presente nel dizionario
    public static boolean check_dictionary(String word, ArrayList<String> dictionary){
        if (dictionary.contains(word)) return true;
        return false;
    }

    //update in caso di vittoria
    public static void update(User user){
        
        user.games_played++;
        user.games_won++;
        user.setHas_played(true);;
        user.win_streak++;
        if(user.win_streak > user.longest_win_streak){
            user.longest_win_streak = user.win_streak;
        }
        

    }

    //update in caso di sconfitta
    public static void update_lose_game(User user){
        
        user.games_played++;
        user.setHas_played(true);
        user.win_streak = 0;
    }

    //update della guess distribution
    public static void up_guess_distribution(User user,int tentativo){
        int old_value = user.guess_distribution.get(tentativo);
        
        user.guess_distribution.replace(tentativo,old_value,old_value+1);
    }


    public static void mess_sender(int port, String host, int n_tentativi,String username) throws UnknownHostException {
        InetAddress ia = InetAddress.getByName(host);
        
        try (DatagramSocket ds = new DatagramSocket(0)) {
            String mess;
            //in base al numero di tentativi costruisce il messaggio
            if(n_tentativi==13){ // 13 tentativi significa che l'utente non ha indovinato la parola
                mess = username+" non ha indovinato la parola";
            }
            else{
                mess = username+" ha indovinato la parola in "+n_tentativi+" tentativi";
            }
            byte[] data = mess.getBytes("US-ASCII");
            //metto stringa in un un pacchetto e la invio 
            DatagramPacket dp = new DatagramPacket(data, data.length, ia, port);
            ds.send(dp);
            //non chiudo il datagramsocket tanto ci pensa il try 
        }
        catch(SocketException e){
            e.printStackTrace();
        }
        catch(UnsupportedEncodingException e){
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }

    }

    
}
