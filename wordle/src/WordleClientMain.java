import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Properties;
import java.util.Scanner;

public class WordleClientMain {
    
    public static Scanner scanner = new Scanner(System.in);
    public static DataInputStream dis;
    public static DataOutputStream dos;

    public static int port;
    public static String host;
    public static String group;
    public static int port_2;
    public static final String configFile = "client.properties";

    public static void main(String[] args) throws IOException{

        readConfig(); //metodo per leggere i parametri di configurazione

        try(Socket socket = new Socket(host, port)){
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            boolean exit = false;
            
                
            while(!exit){ 
                System.out.println("---WORDLE---\n [1]REGISTER \n [2]LOGIN\n [3]EXIT");
                int action;
                try{
                    action = Integer.parseInt(scanner.nextLine());
                }catch(NumberFormatException e){
                    System.out.println("Inserire un carattere numerico per effettuare la scelta");
                    continue;
                }
                dos.writeInt(action);
                
                switch(action){
                    case 1: 
                        register(); //metodo per registrare un nuovo utente
                        continue;
                    case 2:
                        login(); //metodo per permettere ad un utente già registrato di loggarsi
                        continue;
                    case 3: //exit
                        exit = true;
                        break; 
                    default:
                        System.out.println("Comando non riconosciuto");
                        continue;
                }
            }
            dis.close();
            dos.close();
        }
    }

    public static void register() throws IOException, SocketException{

        try{

            boolean registrated = false;

            while(!registrated){ //ficnhé non si è registrati

                System.out.println("Inserisci un nuovo username:");
                String username = scanner.nextLine();
                if(username == ""){
                    System.out.println("L'username non può essere vuoto. ");
                    continue;
                }
                dos.writeUTF(username); //passa nuovo username al server

                int result = dis.readInt();
                if(result == 0){ //username gia in uso
                    System.out.println("Username già in uso.");
                    continue;
                }else{ //username valido
                    boolean accepted = false;
                    while(!accepted){ //finché la password non è accettata
                        System.out.println("Inserisci una nuova password:");
                        String password = scanner.nextLine();
                        if(password.equals("")){ //password vuota
                            System.out.println("La password non può essere vuota");
                            continue;
                        }else{ //password valida
                            dos.writeUTF(password);
                        }
                    
                        accepted = true;
                        registrated = true;
                        System.out.println("Registrato correttamente");
                        
                    }
                }
            }
        }catch(SocketException e){
            e.printStackTrace();
        }
    }

    //metodo per loggarsi
    public static void login() throws IOException, SocketException{

            boolean logged = false;
            
            while(!logged){ 
                System.out.println("Inserisci username:");
                String username = scanner.nextLine();
                dos.writeUTF(username); 
                if(dis.readInt() == 0){ //username valido
                    System.out.println("Inserisci password:");
                    String password = scanner.nextLine();
                    dos.writeUTF(password);

                    int accepted = dis.readInt();
                    
                    if(accepted == 0){ //password corretta
                        
                        Mess_Receiver mess_reciver = new Mess_Receiver(port_2, group);
                        Thread t = new Thread(mess_reciver);
                        t.start();

                        boolean logout = false;
                        int attempts = 0;
                        while(logout == false){ //finche sei loggato

                            try{
                                System.out.println("---WORDLE---\n [1]PLAY\n [2]SEE STATISTIC\n [3]SHARE\n [4]SHOW ME SHARING\n [5]LOGOUT");
                                logged = true;

                                int action2 = Integer.parseInt(scanner.nextLine());
                                dos.writeInt(action2);
                                
                                //secondo menù
                                switch(action2){
                                    case 1: //play
                                        int has_played = dis.readInt();
                                        if(has_played == 0){ //parola già giocata
                                            System.out.println("Hai già giocato oggi, torna domani");
                                        }else{ //parola non ancora giocata
                                            attempts = playWordle(scanner, dis, dos);
                                        }
                                        continue;
                                    case 2: //statistiche
                                        seeStatistics(dis);
                                        continue;
                                    case 3: //share
                                        int has_played2 = dis.readInt();
                                        if(has_played2 == 0){ //parola giocata
                                        
                                            dos.writeInt(attempts);
                                            System.out.println("La tua partita è stata condivisa correttamente");

                                        }else{ //l'utente non ha giocato l'ultima parola e non può condividere il messaggio 
                                            System.out.println("\nImpossibile condividere i dati della partita. Assicurati di aver giocato la parola corrente.");
                                        }
                                        break;
                                    case 4: //show sharing
                                        mess_reciver.print_mess();
                                        break;
                                    case 5: //logout
                                        logout = true;
                                        mess_reciver.close_connection();
                                        break;
                                }
                            }catch(SocketException e){
                                e.printStackTrace();
                            }
                        }
                    }else if(accepted == 1){ //password errata
                        System.out.println("Password non corretta");
                        continue;
                    }else{
                        System.out.println("Questo utente è già loggato su un altro host, per favore esegui il loggout per loggarti");
                        continue;
                    }
                }else{ //username non valido
                    System.out.println("Username insesistente");
                    continue;
                }
            }
            
    }

    //metodo per leggere i parametri di configurazione usato nel main
    public static void readConfig() throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream( "./client.properties" );
        Properties prop = new Properties();
        prop.load(fis);
        port = Integer.parseInt(prop.getProperty("port"));
        host = prop.getProperty("host");
        group = prop.getProperty("group");
        port_2 = Integer.parseInt(prop.getProperty("port_2"));
        fis.close();
	}

    //metodo per giocare
    public static int playWordle(Scanner scanner, DataInputStream dis, DataOutputStream dos) throws IOException{

        boolean guessed = false;
        int attempts = 12;
        System.out.println("----------");

        while(!guessed && attempts > 0){ //finché la parola non è stata indovinata o non finiscono i tentativi
            
            String try_word = scanner.nextLine();
            dos.writeUTF(try_word);

            int accepted = dis.readInt();


            if(accepted == 1){ //parola valida

                int guess = dis.readInt();

                if(guess == 1){ //parola indovinata

                    System.out.println("Parola indovinata");
                    break;

                }else{ //parola sbagliata
                    
                    String back_word = dis.readUTF();
                    attempts--;
                    System.out.println(back_word +"\n tentativi rimanenti: " + attempts);
                    continue;

                }

            }else if(accepted == 2){ //parola di lunghezza errata
                System.out.println("Inserire una parola di 10 caratteri\nTentativi rimanenti: " + attempts);
                continue;

            }else if(accepted == 0){ //parola inesistente

                System.out.println("Parola inesistente, riprova\nTentativi rimanenti: " + attempts);
                continue;
            }
            else{ //tempo esaurito
                System.out.println("Tempo esaurito, la parola è cambiata");
                break;
            }
        }
        if(attempts == 0){ //tentativi esauriti

            System.out.println("Tentativi esauriti");

        }

        return attempts; 
    }



    //metodo che stampa in console le statistiche dell'utente
    public static void seeStatistics(DataInputStream dis) throws IOException{
        System.out.println("---STATISTICS---");

        String username = dis.readUTF();
        System.out.println("username: " + username);

        boolean has_played = dis.readBoolean();
        if(has_played == false){
            System.out.println("Parola del giorno non giocata");
        }else{
            System.out.println("Parola del giorno giocata");
        }

        int games_played = dis.readInt();
        System.out.println("Partite giocate: " + games_played);

        int games_won = dis.readInt();
        System.out.println("Partite vinte: " + games_won);

        double percentage = 0;
        String formatted = "";
        if(games_played > 0){
            percentage = (double) games_won/games_played * 100;
            formatted = String.format("%.2f", percentage);
        }
        System.out.println("Percentuale vittorie: " + formatted + "%");


        int win_streak = dis.readInt();
        System.out.println("Serie di vittorie attuale: " + win_streak);


        int longest_win_streak = dis.readInt();
        System.out.println("Serie di vittorie più lunga: " + longest_win_streak);


        for(int i = 1; i <= 12; i++){
            int attempts = dis.readInt();
            System.out.println("parole indoviate in " + i + " tenatativi: " + attempts );
        }

        System.out.println("----------------");
        
    }




}
