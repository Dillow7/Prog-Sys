package Puissance4;
import java.io.*;
import java.net.*;

public class Puissance4Server {
    private static final int PORT = 12345;
    private static final int ROWS = 6;
    private static final int COLS = 7;
    private static int[][] board = new int[ROWS][COLS];
    private static int currentPlayer = 1;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Serveur démarré sur le port " + PORT);

        // Attendre la connexion de deux joueurs
        Socket player1 = serverSocket.accept();
        Socket player2 = serverSocket.accept();
        System.out.println("Joueurs connectés.");

        BufferedReader in1 = new BufferedReader(new InputStreamReader(player1.getInputStream()));
        PrintWriter out1 = new PrintWriter(player1.getOutputStream(), true);
        BufferedReader in2 = new BufferedReader(new InputStreamReader(player2.getInputStream()));
        PrintWriter out2 = new PrintWriter(player2.getOutputStream(), true);

        // Configuration initiale du jeu
        out1.println("START:1"); // Joueur 1 (rouge)
        out2.println("START:2"); // Joueur 2 (jaune)

        // Indiquer le début du tour de joueur 1
        out1.println("TURN:1");
        out2.println("TURN:2");

        boolean isPlayer1Turn = true;

        while (true) {
            Socket currentPlayerSocket = isPlayer1Turn ? player1 : player2;
            PrintWriter currentOut = isPlayer1Turn ? out1 : out2;
            BufferedReader currentIn = isPlayer1Turn ? in1 : in2;

            try {
                // Lire le mouvement du joueur
                String moveStr = currentIn.readLine();
                System.out.println("Mouvement reçu : " + moveStr + " (Joueur " + (isPlayer1Turn ? "1" : "2") + ")");
                
                int column = Integer.parseInt(moveStr);

                // Valider et traiter le mouvement
                boolean moveValid = processMove(column, isPlayer1Turn ? 1 : 2);
                
                if (moveValid) {
                    // Envoyer l'état du plateau aux deux joueurs
                    String boardState = getBoardState();
                    out1.println("BOARD:" + boardState);
                    out2.println("BOARD:" + boardState);

                    // Vérifier la victoire ou le match nul
                    if (checkWin(isPlayer1Turn ? 1 : 2)) {
                        out1.println("END:WIN:" + (isPlayer1Turn ? 1 : 2));
                        out2.println("END:WIN:" + (isPlayer1Turn ? 1 : 2));
                        break;
                    }
                    
                    if (isBoardFull()) {
                        out1.println("END:DRAW");
                        out2.println("END:DRAW");
                        break;
                    }

                    // Changer de tour
                    isPlayer1Turn = !isPlayer1Turn;

                    // Informer les joueurs du changement de tour
                    out1.println("TURN:" + (isPlayer1Turn ? "1" : "2"));
                    out2.println("TURN:" + (isPlayer1Turn ? "1" : "2"));
                } else {
                    // Mouvement invalide
                    System.out.println("Mouvement invalide pour le joueur " + (isPlayer1Turn ? "1" : "2"));
                    currentOut.println("INVALID:Move");
                }
            } catch (IOException | NumberFormatException e) {
                System.out.println("Erreur : " + e.getMessage());
                break;
            }
        }

        serverSocket.close();
    }

    private static boolean processMove(int column, int player) {
        // Vérifier si la colonne est valide
        if (column < 0 || column >= COLS) {
            System.out.println("Colonne invalide : " + column);
            return false;
        }

        // Trouver la première ligne vide dans la colonne depuis le bas
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][column] == 0) {
                board[row][column] = player;
                System.out.println("Mouvement valide : Joueur " + player + " colonne " + column + " ligne " + row);
                return true;
            }
        }
        System.out.println("Colonne pleine : " + column);
        return false; // Colonne est pleine
    }

    private static boolean checkWin(int player) {
        // Vérifier horizontalement
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col <= COLS - 4; col++) {
                if (board[row][col] == player &&
                    board[row][col+1] == player &&
                    board[row][col+2] == player &&
                    board[row][col+3] == player) {
                    return true;
                }
            }
        }

        // Vérifier verticalement
        for (int row = 0; row <= ROWS - 4; row++) {
            for (int col = 0; col < COLS; col++) {
                if (board[row][col] == player &&
                    board[row+1][col] == player &&
                    board[row+2][col] == player &&
                    board[row+3][col] == player) {
                    return true;
                }
            }
        }

        // Vérifier diagonale (haut-gauche vers bas-droite)
        for (int row = 0; row <= ROWS - 4; row++) {
            for (int col = 0; col <= COLS - 4; col++) {
                if (board[row][col] == player &&
                    board[row+1][col+1] == player &&
                    board[row+2][col+2] == player &&
                    board[row+3][col+3] == player) {
                    return true;
                }
            }
        }

        // Vérifier diagonale (bas-gauche vers haut-droite)
        for (int row = 3; row < ROWS; row++) {
            for (int col = 0; col <= COLS - 4; col++) {
                if (board[row][col] == player &&
                    board[row-1][col+1] == player &&
                    board[row-2][col+2] == player &&
                    board[row-3][col+3] == player) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isBoardFull() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (board[row][col] == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String getBoardState() {
        StringBuilder boardState = new StringBuilder();
        for (int[] row : board) {
            for (int cell : row) {
                boardState.append(cell).append(" ");
            }
            boardState.append("\n");
        }
        return boardState.toString();
    }
}
