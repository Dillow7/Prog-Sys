package Dames;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DamesClient extends JFrame {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 5555;
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static JFrame frame;
    private static JPanel panel;
    private static CasePanel[][] casePanels;
    private static String[][] board;
    private static int selectedX = -1, selectedY = -1;
    private static boolean isMyTurn = false;
    private static int playerNumber = -1; // 1 pour joueur 1 (blanc), 2 pour joueur 2 (noir)

    public static void main(String[] args) {
        // Définir automatiquement le numéro du joueur à 1, il sera mis à jour lors de la connexion
        playerNumber = 1;
        
        SwingUtilities.invokeLater(() -> {
            new DamesClient();
        });
    }
    private static boolean isMyPiece(String piece) {
        if (playerNumber == 1) {
            return piece.startsWith("B"); // Joueur 1 contrôle les pions blancs
        } else {
            return piece.startsWith("N"); // Joueur 2 contrôle les pions noirs
        }
    }
    
    private static void connecterAuServeur() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    
            // Lire le numéro du joueur envoyé par le serveur
            String firstMessage = in.readLine();
            if (firstMessage.startsWith("PLAYER")) {
                playerNumber = Integer.parseInt(firstMessage.split(" ")[1]);
                System.out.println("Je suis le joueur " + playerNumber);
            }
    
            // Thread pour recevoir les messages
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        traiterMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void traiterMessage(String message) {
        if (message != null) {
            System.out.println("Message du serveur : " + message);
            
            if (message.equals("VOTRE_TOUR")) {
                isMyTurn = true;
                SwingUtilities.invokeLater(() -> {
                    frame.setTitle("Jeu de Dames - Joueur " + playerNumber + " - C'est votre tour!");
                });
            } else if (message.equals("EN_ATTENTE")) {
                isMyTurn = false;
                SwingUtilities.invokeLater(() -> {
                    frame.setTitle("Jeu de Dames - Joueur " + playerNumber + " - En attente...");
                });
            } else if (message.startsWith("GAGNE")) {
                String winner = message.substring(6);
                JOptionPane.showMessageDialog(frame, 
                    "Félicitations ! " + winner + " a gagné la partie!", 
                    "Partie Terminée", 
                    JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            } else if (message.startsWith("PERDU")) {
                String loser = message.substring(6);
                JOptionPane.showMessageDialog(frame, 
                    "Dommage ! " + loser + " a perdu la partie.", 
                    "Partie Terminée", 
                    JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            } else if (message.matches("\\d+ \\d+ \\d+ \\d+")) {
                String[] moveParts = message.split(" ");
                int fromX = Integer.parseInt(moveParts[0]);
                int fromY = Integer.parseInt(moveParts[1]);
                int toX = Integer.parseInt(moveParts[2]);
                int toY = Integer.parseInt(moveParts[3]);

                updateBoardWithMove(fromX, fromY, toX, toY);
            }
        }

        SwingUtilities.invokeLater(() -> {
            updateUI();
        });
    }

    public DamesClient() {
        frame = new JFrame("Jeu de Dames - Joueur " + playerNumber);
        frame.setLayout(new BorderLayout());
        panel = new JPanel();
        panel.setLayout(new GridLayout(8, 8));
        casePanels = new CasePanel[8][8];
        board = new String[8][8];

        connecterAuServeur();
        initializeBoard();

        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static void initializeBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                casePanels[i][j] = new CasePanel(i, j);
                if ((i + j) % 2 == 0) {
                    casePanels[i][j].setBackground(new Color(255, 235, 185));
                } else {
                    casePanels[i][j].setBackground(new Color(139, 69, 19));
                    if (i < 3) {
                        board[i][j] = "N";
                    } else if (i > 4) {
                        board[i][j] = "B";
                    } else {
                        board[i][j] = " ";
                    }
                }

                final int x = i;
                final int y = j;

                casePanels[i][j].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (isMyTurn) {
                            if (selectedX == -1 && selectedY == -1) {
                                // Vérifier si la pièce appartient au joueur actuel
                                if (!board[x][y].equals(" ") && isMyPiece(board[x][y])) {
                                    selectedX = x;
                                    selectedY = y;
                                    System.out.println("Sélectionnée : " + selectedX + ", " + selectedY);
                                    casePanels[x][y].setBackground(Color.YELLOW); // Highlight sélection
                                }
                            } else {
                                // Déplacer la pièce
                                if (isValidMove(selectedX, selectedY, x, y, board)) {
                                    movePiece(selectedX, selectedY, x, y);
                                    // Réinitialiser la couleur de la case précédemment sélectionnée
                                    if ((selectedX + selectedY) % 2 == 0) {
                                        casePanels[selectedX][selectedY].setBackground(new Color(255, 235, 185));
                                    } else {
                                        casePanels[selectedX][selectedY].setBackground(new Color(139, 69, 19));
                                    }
                                }
                                selectedX = -1;
                                selectedY = -1;
                            }
                            updateUI();
                        } else {
                            System.out.println("Ce n'est pas votre tour.");
                        }
                    }
                });

                panel.add(casePanels[i][j]);
            }
        }
        updateUI();
    }

    private static void updateBoardWithMove(int fromX, int fromY, int toX, int toY) {
        String piece = board[fromX][fromY];
        board[fromX][fromY] = " ";
        board[toX][toY] = piece;

        // Promotion en dame
        if (piece.equals("N") && toX == 7) {
            board[toX][toY] = "ND";
        } else if (piece.equals("B") && toX == 0) {
            board[toX][toY] = "BD";
        }

        // Capture
        if (Math.abs(toX - fromX) == 2 && Math.abs(toY - fromY) == 2) {
            int middleX = (fromX + toX) / 2;
            int middleY = (fromY + toY) / 2;
            board[middleX][middleY] = " ";
        }
    }

    private static void movePiece(int fromX, int fromY, int toX, int toY) {
        String move = fromX + " " + fromY + " " + toX + " " + toY;
        out.println(move);
        out.println("tour termine");
        isMyTurn = false;
    }

    private static void updateUI() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                casePanels[i][j].repaint();
            }
        }
    }

    private static boolean isValidMove(int fromX, int fromY, int toX, int toY, String[][] board) {
        String piece = board[fromX][fromY];
        boolean isBlackPiece = piece.equals("N");
        boolean isWhitePiece = piece.equals("B");
        boolean isBlackKing = piece.equals("ND");
        boolean isWhiteKing = piece.equals("BD");
    
        // Vérifier si le mouvement est diagonal
        if (Math.abs(toX - fromX) != Math.abs(toY - fromY)) {
            return false;
        }
    
        // Pour les pions normaux (non-dames), vérifier la direction du mouvement
        if (!isBlackKing && !isWhiteKing) {
            if (isBlackPiece && toX < fromX) {
                return false;  // Les pions noirs ne peuvent avancer que vers le bas
            }
            if (isWhitePiece && toX > fromX) {
                return false;  // Les pions blancs ne peuvent avancer que vers le haut
            }
        }
    
        // Si le mouvement est d'une seule case
        if (Math.abs(toX - fromX) == 1 && Math.abs(toY - fromY) == 1) {
            return board[toX][toY].equals(" ");
        }
    
        // Si le mouvement est un saut (deux cases)
        if (Math.abs(toX - fromX) == 2 && Math.abs(toY - fromY) == 2) {
            int midX = (fromX + toX) / 2;
            int midY = (fromY + toY) / 2;
    
            if (board[toX][toY].equals(" ")) {
                String middlePiece = board[midX][midY];
                if (isWhitePiece || isWhiteKing) {
                    return middlePiece.equals("N") || middlePiece.equals("ND");
                } else {
                    return middlePiece.equals("B") || middlePiece.equals("BD");
                }
            }
        }
    
        return false;
    }

    static class CasePanel extends JPanel {
        private int x, y;
    
        public CasePanel(int x, int y) {
            this.x = x;
            this.y = y;
            setPreferredSize(new Dimension(70, 70));
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            int width = getWidth();
            int height = getHeight();
            int pieceSize = Math.min(width, height) - 20;
            int xOffset = (width - pieceSize) / 2;
            int yOffset = (height - pieceSize) / 2;
        
            if (board[x][y] != null && !board[x][y].equals(" ")) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
                // "B" est toujours blanc, "N" est toujours noir
                if (board[x][y].startsWith("B")) {
                    g2d.setColor(Color.WHITE);
                } else {
                    g2d.setColor(Color.BLACK);
                }
                
                g2d.fillOval(xOffset, yOffset, pieceSize, pieceSize);
                
                // Dessiner le contour
                g2d.setColor(Color.GRAY);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(xOffset, yOffset, pieceSize, pieceSize);
        
                // Dessiner la couronne pour les dames
                if (board[x][y].equals("ND") || board[x][y].equals("BD")) {
                    g2d.setColor(Color.RED);
                    int crownHeight = pieceSize / 3;
                    int[] xPoints = {
                        width/2, 
                        width/2 - pieceSize/4, 
                        width/2 + pieceSize/4
                    };
                    int[] yPoints = {
                        yOffset + pieceSize/4,
                        yOffset + pieceSize/4 + crownHeight,
                        yOffset + pieceSize/4 + crownHeight
                    };
                    g2d.fillPolygon(xPoints, yPoints, 3);
                }
            }
        }
    }
}