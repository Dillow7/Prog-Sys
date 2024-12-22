package Accueil;

import javax.swing.*;

import Dames.DamesClient;
import Dames.DamesServer;
import Morpion.MorpionClient;
import Morpion.MorpionServeur;
import Puissance4.Puissance4Client;
import Puissance4.Puissance4Server;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.net.ServerSocket;

public class AccueilFrame extends JFrame {
    private static final Color COULEUR_FOND = new Color(245, 245, 250);
    private static final Color COULEUR_BOUTON = new Color(70, 130, 180);
    private static final Color COULEUR_BOUTON_HOVER = new Color(100, 149, 237);
    private static final Font POLICE_TITRE = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font POLICE_BOUTON = new Font("Segoe UI", Font.BOLD, 16);

    private boolean morpionServeurLance = false;
    private boolean puissance4ServeurLance = false;
    private boolean damesServeurLance = false;

    public AccueilFrame() {
        setTitle("Plateforme de Jeux");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setBackground(COULEUR_FOND);

        JPanel panelPrincipal = new JPanel(new BorderLayout(20, 20));
        panelPrincipal.setBackground(COULEUR_FOND);
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel titre = new JLabel("Choisissez votre jeu", SwingConstants.CENTER);
        titre.setFont(POLICE_TITRE);
        titre.setForeground(new Color(50, 50, 50));
        panelPrincipal.add(titre, BorderLayout.NORTH);

        JPanel panelBoutons = new JPanel(new GridLayout(3, 1, 0, 20));
        panelBoutons.setBackground(COULEUR_FOND);
        panelBoutons.setBorder(BorderFactory.createEmptyBorder(30, 0, 30, 0));

        JButton btnMorpion = creerBoutonJeu("Morpion", "Jeu de Morpion en réseau", "/icons/morpion.png");
        JButton btnPuissance4 = creerBoutonJeu("Puissance 4", "Jeu de Puissance 4 en réseau", "/icons/puissance4.png");
        JButton btnDames = creerBoutonJeu("Dames", "Jeu de Dames en réseau", "/icons/dames.png");

        btnMorpion.addActionListener(e -> gererJeu("Morpion"));
        btnPuissance4.addActionListener(e -> gererJeu("Puissance4"));
        btnDames.addActionListener(e -> gererJeu("Dames"));

        panelBoutons.add(btnMorpion);
        panelBoutons.add(btnPuissance4);
        panelBoutons.add(btnDames);

        panelPrincipal.add(panelBoutons, BorderLayout.CENTER);
        add(panelPrincipal);

        setResizable(false);
    }

    private JButton creerBoutonJeu(String texte, String tooltip, String iconPath) {
        JButton bouton = new JButton(texte) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(COULEUR_BOUTON.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(COULEUR_BOUTON_HOVER);
                } else {
                    g2.setColor(COULEUR_BOUTON);
                }

                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));

                g2.setColor(Color.WHITE);
                g2.setFont(POLICE_BOUTON);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };

        bouton.setPreferredSize(new Dimension(300, 60));
        bouton.setFocusPainted(false);
        bouton.setBorderPainted(false);
        bouton.setContentAreaFilled(false);
        bouton.setToolTipText(tooltip);
        bouton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return bouton;
    }

    private void gererJeu(String jeu) {
        switch (jeu) {
            case "Morpion":
                if (!morpionServeurLance) {
                    lancerServeur(() -> {
                        MorpionServeur.main(new String[]{});
                        morpionServeurLance = true;
                    });
                } else {
                    lancerClient(() -> new MorpionClient().setVisible(true));
                }
                break;
            case "Puissance4":
                if (!puissance4ServeurLance) {
                    lancerServeur(() -> {
                        try {
                            Puissance4Server.main(new String[]{});
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        puissance4ServeurLance = true;
                    });
                } else {
                    lancerClient(() -> new Puissance4Client().setVisible(true));
                }
                break;
            case "Dames":
                if (!damesServeurLance) {
                    lancerServeur(() -> {
                        DamesServer.main(new String[]{});
                        damesServeurLance = true;
                    });
                } else {
                    lancerClient(() -> new DamesClient().setVisible(true));
                }
                break;
        }
    }

    private void lancerServeur(Runnable serveur) {
        new Thread(() -> {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                serveur.run();
                JOptionPane.showMessageDialog(this, "Le serveur est démarré avec succès !");
            } catch (Exception ex) {
                ex.printStackTrace();
                afficherErreur("Erreur lors du démarrage du serveur");
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }).start();
    }

    private void lancerClient(Runnable client) {
        new Thread(() -> {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                client.run();
            } catch (Exception ex) {
                ex.printStackTrace();
                afficherErreur("Erreur lors du lancement du client");
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }).start();
    }

    private void afficherErreur(String message) {
        SwingUtilities.invokeLater(() -> 
            JOptionPane.showMessageDialog(this, 
                message, 
                "Erreur", 
                JOptionPane.ERROR_MESSAGE)
        );
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new AccueilFrame().setVisible(true);
        });
    }
}
