package Dames;
import java.io.*;
import java.net.*;
import java.util.*;

public class DamesServer {
    private static final int PORT = 5555;
    private static List<PrintWriter> clientOutputs = new ArrayList<>();
    private static List<BufferedReader> clientInputs = new ArrayList<>();
    private static int currentPlayer = 0; // Indicateur pour le joueur actuel (0 pour le joueur 1, 1 pour le joueur 2)
    private static String[][] board = new String[8][8]; // Plateau de jeu


        public static void main(String[] args) {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Serveur démarré, en attente de connexions...");
        
                while (true) {
                    // Attente d'un client
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Un client est connecté.");
        
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        
                    // Envoyer le numéro du joueur au client
                    int playerNumber = clientOutputs.size() + 1;
                    out.println("PLAYER " + playerNumber);
        
                    clientInputs.add(in);
                    clientOutputs.add(out);
        
                    // Initialisation du plateau
                    initializeBoard();
        
                    // Lorsque deux joueurs sont connectés, on commence le jeu
                    if (clientOutputs.size() == 2) {
                        startGame();  // Début de la partie
                    }
        
                    // Lancer un thread pour chaque client
                    new ClientHandler(clientSocket, in, out).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    // Initialiser le plateau de jeu
    private static void initializeBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if ((i + j) % 2 != 0) {
                    if (i < 3) {
                        board[i][j] = "N";  // Pions noirs
                    } else if (i > 4) {
                        board[i][j] = "B";  // Pions blancs
                    } else {
                        board[i][j] = " ";  // Cases vides
                    }
                } else {
                    board[i][j] = " ";  // Cases vides
                }
            }
        }
    }

    // Vérifier si un joueur a perdu (tous ses pions éliminés)
    private static boolean checkGameEnd() {
        boolean blackPiecesExist = false;
        boolean whitePiecesExist = false;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j].equals("N") || board[i][j].equals("ND")) {
                    blackPiecesExist = true;
                }
                if (board[i][j].equals("B") || board[i][j].equals("BD")) {
                    whitePiecesExist = true;
                }
            }
        }

        return !(blackPiecesExist && whitePiecesExist);
    }

    // Démarrer le jeu et gérer les tours
    private static void startGame() {
        // Le joueur 1 commence
        clientOutputs.get(0).println("VOTRE_TOUR");  // Le premier joueur reçoit "VOTRE_TOUR"
        clientOutputs.get(1).println("EN_ATTENTE");  // Le second joueur est en attente
    }

    // Classe pour gérer chaque client
    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket clientSocket, BufferedReader in, PrintWriter out) {
            this.clientSocket = clientSocket;
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Message reçu: " + message);

                    if (message.equals("tour termine")) {
                        // Vérifier si le jeu est terminé
                        if (checkGameEnd()) {
                            // Déterminer le gagnant et le perdant
                            int winner = currentPlayer;
                            int loser = (currentPlayer + 1) % 2;

                            // Envoyer les messages de fin de jeu
                            clientOutputs.get(winner).println("GAGNE Joueur " + (winner + 1));
                            clientOutputs.get(loser).println("PERDU Joueur " + (loser + 1));

                            // Arrêter la partie
                            return;
                        }

                        // Le joueur a terminé son tour, on passe au suivant
                        currentPlayer = (currentPlayer + 1) % 2;  // Alterne entre 0 et 1 (joueur 1 et 2)
                        
                        // Envoie "VOTRE_TOUR" au joueur suivant pour indiquer que c'est son tour
                        clientOutputs.get(currentPlayer).println("VOTRE_TOUR");
                        // Envoie "EN_ATTENTE" à l'autre joueur pour indiquer qu'il doit attendre
                        clientOutputs.get((currentPlayer + 1) % 2).println("EN_ATTENTE");
                    } else if (message.matches("\\d+ \\d+ \\d+ \\d+")) {
                        // Mouvement reçu, mettre à jour le plateau
                        String[] moveParts = message.split(" ");
                        int fromX = Integer.parseInt(moveParts[0]);
                        int fromY = Integer.parseInt(moveParts[1]);
                        int toX = Integer.parseInt(moveParts[2]);
                        int toY = Integer.parseInt(moveParts[3]);

                        // Mettre à jour le plateau
                        movePiece(board, fromX, fromY, toX, toY);

                        // Relayer le mouvement à l'autre joueur
                        broadcastMove(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

   // Relayer le mouvement à l'autre joueur
    private static void broadcastMove(String move) {
        for (PrintWriter clientOut : clientOutputs) {
            clientOut.println(move);  // Diffuse le mouvement à tous les clients
        }
    }

    // Fonction pour déplacer un pion sur le plateau
    private static void movePiece(String[][] board, int fromX, int fromY, int toX, int toY) {
        String player = board[fromX][fromY];  // Récupérer le joueur (N ou B)
        String opponent = player.equals("N") ? "B" : "N";

        // Vérification des mouvements valides (y compris l'élimination)
        if (isValidMove(board, fromX, fromY, toX, toY, player)) {
            // Si c'est un saut, on élimine le pion de l'adversaire
            if (Math.abs(toX - fromX) == 2 && Math.abs(toY - fromY) == 2) {
                int middleX = (fromX + toX) / 2;
                int middleY = (fromY + toY) / 2;
                board[middleX][middleY] = " ";  // Enlever le pion adverse
            }

            // Déplacer le pion
            board[toX][toY] = board[fromX][fromY];
            board[fromX][fromY] = " ";  // Case vide après le mouvement

            // Promotion en dame si le pion atteint la ligne de l'adversaire
            if ((player.equals("N") && toX == 7) || (player.equals("B") && toX == 0)) {
                board[toX][toY] = player.equals("N") ? "ND" : "BD";  // Promotion du pion en dame
            }
        }
    }

    // Fonction pour vérifier si le mouvement est valide
    private static boolean isValidMove(String[][] board, int fromX, int fromY, int toX, int toY, String player) {
        // Vérifier que le mouvement est en diagonale
        if (Math.abs(toX - fromX) != Math.abs(toY - fromY)) {
            return false;  // Si la différence entre x et y n'est pas égale, ce n'est pas un mouvement diagonal
        }

        // Vérifier si c'est un pion normal ou une dame
        boolean isKing = board[fromX][fromY].equals("ND") || board[fromX][fromY].equals("BD");

        // Si c'est un saut (mouvement de 2 cases)
        if (Math.abs(toX - fromX) == 2 && Math.abs(toY - fromY) == 2) {
            int middleX = (fromX + toX) / 2;
            int middleY = (fromY + toY) / 2;
            String middlePiece = board[middleX][middleY];
            
            // Vérifier qu'il y a un pion adverse sur la case du milieu
            if (!middlePiece.equals(" ") && !middlePiece.equals(player)) {
                // Vérifier que la case d'arrivée est vide
                if (board[toX][toY].equals(" ")) {
                    return true;  // Mouvement valide (saut d'un pion adverse)
                }
            }
        }

        // Si c'est un déplacement simple (une seule case)
        if (Math.abs(toX - fromX) == 1 && Math.abs(toY - fromY) == 1 && board[toX][toY].equals(" ")) {
            return true;  // Mouvement simple valide
        }

        // Si c'est une dame, elle peut se déplacer en arrière
        if (isKing) {
            if (Math.abs(toX - fromX) == 1 && Math.abs(toY - fromY) == 1 && board[toX][toY].equals(" ")) {
                return true;  // Mouvement en arrière d'une dame valide
            }
        }

        return false;  // Si aucune des conditions précédentes n'est remplie, le mouvement est invalide
    }

    
}