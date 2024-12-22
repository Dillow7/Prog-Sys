package Puissance4;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Puissance4Client extends JFrame {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private static final int ROWS = 6;
    private static final int COLS = 7;

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    private static JButton[][] buttons = new JButton[ROWS][COLS];
    private static JButton[] columnButtons = new JButton[COLS];
    private static JLabel statusLabel;

    private static int playerNumber;
    private static boolean isMyTurn = false;

    private static final Color PLAYER1_COLOR = Color.RED;
    private static final Color PLAYER2_COLOR = Color.YELLOW;

    public Puissance4Client() {
        setTitle("Puissance 4");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panneau du plateau de jeu
        JPanel gamePanel = new JPanel(new GridLayout(ROWS, COLS));
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                JButton button = new JButton();
                button.setBackground(Color.WHITE);
                button.setPreferredSize(new Dimension(80, 80));
                button.setEnabled(false);  // Les cases sont initialement désactivées
                buttons[row][col] = button;
                gamePanel.add(button);
            }
        }

        // Panneau de sélection de colonne
        JPanel columnPanel = new JPanel(new GridLayout(1, COLS));
        for (int col = 0; col < COLS; col++) {
            final int column = col;
            JButton colButton = new JButton("↓");
            colButton.setFont(new Font("Arial", Font.BOLD, 24));
            colButton.setForeground(Color.BLUE);
            colButton.addActionListener(e -> {
                if (isMyTurn) {
                    out.println(column);
                    isMyTurn = false;
                    enableColumnButtons(false); // Désactiver les boutons de colonnes
                    statusLabel.setText("Attente du tour de l'adversaire...");
                } else {
                    System.out.println("Ce n'est pas votre tour !");
                }
            });
            columnButtons[col] = colButton;
            columnPanel.add(colButton);
        }

        // Étiquette de statut
        statusLabel = new JLabel("Attente de la connexion...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));

        // Ajouter les composants à la fenêtre
        add(gamePanel, BorderLayout.CENTER);
        add(columnPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);

        setVisible(true);

        connectToServer();
    }

    private static void enableColumnButtons(boolean enable) {
        for (JButton button : columnButtons) {
            button.setEnabled(enable);
        }
    }

    public static void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Lire les messages du serveur
                while (true) {
                    String message = in.readLine();
                    if (message == null) {
                        statusLabel.setText("Erreur de communication avec le serveur.");
                        break;
                    }
                    handleServerMessage(message);
                }

            } catch (IOException e) {
                statusLabel.setText("Connexion perdue avec le serveur !");
                enableColumnButtons(false);
                e.printStackTrace();
            }
        }).start();
    }

    private static void handleServerMessage(String message) {
        System.out.println("Message du serveur : " + message); // Log du message reçu

        if (message.startsWith("START:")) {
            playerNumber = Integer.parseInt(message.split(":")[1]);
            statusLabel.setText("Vous êtes le joueur " + playerNumber);
        } else if (message.startsWith("BOARD:")) {
            String boardState = message.split(":")[1];
            updateBoard(boardState);
        } else if (message.startsWith("TURN:")) {
            String turnMessage = message.split(":")[1];

            if (turnMessage.equals(String.valueOf(playerNumber))) {
                // C'est le tour du joueur
                isMyTurn = true;
                statusLabel.setText("Votre tour !");
                enableColumnButtons(true); // Activer les boutons de colonnes
            } else {
                // Ce n'est pas le tour du joueur
                isMyTurn = false;
                statusLabel.setText("Tour de l'adversaire...");
                enableColumnButtons(false); // Désactiver les boutons de colonnes
            }
        } else if (message.startsWith("INVALID:")) {
            statusLabel.setText("Mouvement invalide, réessayez !");
        } else if (message.startsWith("END:")) {
            handleEndGame(message);
        }
    }

    private static void updateBoard(String boardState) {
        // Mettre à jour le plateau en fonction de l'état du jeu reçu du serveur
        String[] rows = boardState.split("\n");
        for (int row = 0; row < rows.length; row++) {
            String[] cells = rows[row].split(" ");
            for (int col = 0; col < cells.length; col++) {
                int value = Integer.parseInt(cells[col].trim());
                if (value == 1) {
                    buttons[row][col].setBackground(PLAYER1_COLOR);
                } else if (value == 2) {
                    buttons[row][col].setBackground(PLAYER2_COLOR);
                } else {
                    buttons[row][col].setBackground(Color.WHITE);
                }
            }
        }
    }

    private static void handleEndGame(String message) {
        String[] parts = message.split(":");
        if (parts[1].equals("WIN")) {
            statusLabel.setText("Le joueur " + parts[2] + " a gagné !");
        } else if (parts[1].equals("DRAW")) {
            statusLabel.setText("Match nul !");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Puissance4Client::new);
    }
}
