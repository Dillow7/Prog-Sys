package Morpion;
import java.io.*;
import java.net.*;

public class MorpionServeur {
    private static final int PORT = 7777;
    private static final int TAILLE = 3;
    private char[][] plateau;
    private char tourActuel;
    private ServerSocket serverSocket;
    private Socket joueur1Socket;
    private Socket joueur2Socket;
    private PrintWriter joueur1Out;
    private PrintWriter joueur2Out;
    private BufferedReader joueur1In;
    private BufferedReader joueur2In;

    public MorpionServeur() {
        plateau = new char[TAILLE][TAILLE];
        initialisationPlateau();
        tourActuel = 'X';
    }

    private void initialisationPlateau() {
        for (int i = 0; i < TAILLE; i++) {
            for (int j = 0; j < TAILLE; j++) {
                plateau[i][j] = ' ';
            }
        }
    }

    public void demarrer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Serveur démarré sur le port " + PORT);
    
            // Connexion joueur 1
            joueur1Socket = serverSocket.accept();
            joueur1Out = new PrintWriter(joueur1Socket.getOutputStream(), true);
            joueur1In = new BufferedReader(new InputStreamReader(joueur1Socket.getInputStream()));
            joueur1Out.println("JOUEUR:X");
            System.out.println("Joueur 1 (X) connecté depuis " + joueur1Socket.getInetAddress());
    
            // Connexion joueur 2
            joueur2Socket = serverSocket.accept();
            joueur2Out = new PrintWriter(joueur2Socket.getOutputStream(), true);
            joueur2In = new BufferedReader(new InputStreamReader(joueur2Socket.getInputStream()));
            joueur2Out.println("JOUEUR:O");
            System.out.println("Joueur 2 (O) connecté depuis " + joueur2Socket.getInetAddress());
    
            // Commencer par le tour du joueur X
            joueur1Out.println("VOTRE_TOUR");
    
            // Gestion des tours et des coups
            gererPartie();
    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void gererPartie() throws IOException {
        while (true) {
            // Sélectionner le joueur actuel et l'autre joueur
            BufferedReader joueurActifIn = (tourActuel == 'X') ? joueur1In : joueur2In;
            PrintWriter joueurActifOut = (tourActuel == 'X') ? joueur1Out : joueur2Out;
            PrintWriter autreJoueurOut = (tourActuel == 'X') ? joueur2Out : joueur1Out;
    
            // Attendre le coup du joueur actif
            String ligneCoup = joueurActifIn.readLine();
    
            // Traiter les messages de chat
            if (ligneCoup != null && ligneCoup.startsWith("CHAT:")) {
                autreJoueurOut.println(ligneCoup);
                continue;
            }
    
            // Traiter le coup
            if (ligneCoup != null && ligneCoup.matches("\\d,\\d")) {
                String[] coordonnees = ligneCoup.split(",");
                int ligne = Integer.parseInt(coordonnees[0]);
                int colonne = Integer.parseInt(coordonnees[1]);
    
                // Vérifier si le coup est valide
                if (plateau[ligne][colonne] == ' ') {
                    // Placer le coup
                    plateau[ligne][colonne] = tourActuel;
    
                    // Afficher les coordonnées du coup dans le terminal
                    System.out.println("Coup joué: " + tourActuel + " à la position (" + ligne + ", " + colonne + ")");
    
                    // Informer tous les joueurs du coup avec le symbole
                    joueur1Out.println(ligneCoup + ":" + tourActuel);
                    joueur2Out.println(ligneCoup + ":" + tourActuel);
    
                    // Vérifier si le jeu est terminé
                    if (verifierGagnant()) {
                        terminerJeu(tourActuel);
                        break;
                    } else if (verifierMatchNul()) {
                        terminerJeu(' ');
                        break;
                    }
    
                    // Passer au joueur suivant
                    tourActuel = (tourActuel == 'X') ? 'O' : 'X';
                    
                    // Signaler le tour du prochain joueur
                    if (tourActuel == 'X') {
                        joueur1Out.println("VOTRE_TOUR");
                    } else {
                        joueur2Out.println("VOTRE_TOUR");
                    }
                }
            }
        }
    }
    

    private boolean verifierGagnant() {
        // Vérification des lignes
        for (int i = 0; i < TAILLE; i++) {
            if (plateau[i][0] != ' ' && 
                plateau[i][0] == plateau[i][1] && 
                plateau[i][1] == plateau[i][2]) {
                return true;
            }
        }

        // Vérification des colonnes
        for (int j = 0; j < TAILLE; j++) {
            if (plateau[0][j] != ' ' && 
                plateau[0][j] == plateau[1][j] && 
                plateau[1][j] == plateau[2][j]) {
                return true;
            }
        }

        // Vérification des diagonales
        if (plateau[0][0] != ' ' && 
            plateau[0][0] == plateau[1][1] && 
            plateau[1][1] == plateau[2][2]) {
            return true;
        }

        if (plateau[0][2] != ' ' && 
            plateau[0][2] == plateau[1][1] && 
            plateau[1][1] == plateau[2][0]) {
            return true;
        }

        return false;
    }

    private boolean verifierMatchNul() {
        for (int i = 0; i < TAILLE; i++) {
            for (int j = 0; j < TAILLE; j++) {
                if (plateau[i][j] == ' ') {
                    return false;
                }
            }
        }
        return true;
    }

    private void terminerJeu(char gagnant) {
        String resultat = (gagnant == ' ') ? "MATCH_NUL" : "GAGNANT:" + gagnant;
        joueur1Out.println(resultat);
        joueur2Out.println(resultat);
    }

    public static void main(String[] args) {
        new MorpionServeur().demarrer();
    }
}