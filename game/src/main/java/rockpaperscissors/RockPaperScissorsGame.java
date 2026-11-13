package rockpaperscissors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.Timer;
import java.io.*;
import javax.sound.sampled.*;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

public class RockPaperScissorsGame extends JFrame implements KeyListener {
    // Game components
    private CardLayout cardLayout;
    private JPanel mainPanel, menuPanel, gamePanel;
    private JLabel countdownLabel, resultLabel, scoreLabel;
    private JLabel player1ImageLabel, player2ImageLabel;
    private JLabel player1StatusLabel, player2StatusLabel;
    
    // Game state
    private String mode = "PvC";
    private Timer countdownTimer;
    private int countdown = 3;
    private String player1Choice = "";
    private String player2Choice = "";
    private boolean keyPressedPlayer1 = false;
    private boolean keyPressedPlayer2 = false;
    private int player1Score = 0;
    private int player2Score = 0;
    private int round = 0;
    private int player1KeyPressCount = 0;
    private int player2KeyPressCount = 0;
    private final int totalRounds = 3;
    private Clip menuClip;
    private List<String> playerHistory = new ArrayList<>();

    private URL getResource(String path) {
        URL url = getClass().getResource(path);
        if (url == null) {
            showErrorDialog("Missing Resource", "Cannot find: " + path);
            throw new RuntimeException("Missing resource: " + path);
        }
        return url;
    }

    private void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public RockPaperScissorsGame() {
        try {
            initCrossPlatformSettings();
            setupWindow();
            initAudioSystem();
            setupUI();
            playMenuMusic();
            setVisible(true);
        } catch (Exception e) {
            showErrorDialog("Startup Failed", e.getMessage());
            System.exit(1);
        }
    }

    private void initCrossPlatformSettings() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                System.setProperty("apple.awt.application.appearance", "system");
                System.setProperty("apple.laf.useScreenMenuBar", "true");
            }
        } catch (Exception e) {
            System.err.println("Look and feel error: " + e.getMessage());
        }
    }

    private void setupWindow() {
        setTitle("Rock Paper Scissors Game");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setFocusable(true);
        addKeyListener(this);
    }

    private void initAudioSystem() {
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            System.setProperty("javax.sound.sampled.Clip", "com.sun.media.sound.DirectAudioDeviceProvider");
            System.setProperty("javax.sound.sampled.Port", "com.sun.media.sound.PortMixerProvider");
        }
    }

    private void setupUI() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        setupMenuPanel();
        setupGamePanel();
        mainPanel.add(menuPanel, "menu");
        mainPanel.add(gamePanel, "game");
        add(mainPanel);
        cardLayout.show(mainPanel, "menu");
    }

    private void playSound(String path) {
        try {
            URL soundURL = getResource("/" + path);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            
            if (menuClip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                FloatControl menuGain = (FloatControl) menuClip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(menuGain.getValue());
            }
            
            clip.start();
        } catch (Exception e) {
            System.err.println("Failed to play: " + path + " - " + e.getMessage());
        }
    }

    private void playMenuMusic() {
        try {
            URL soundURL = getResource("/audio/menu_music.wav");
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
            menuClip = AudioSystem.getClip();
            menuClip.open(audioStream);
            menuClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            showErrorDialog("Music Error", "Failed to load menu music\n" + e.getMessage());
        }
    }

    private void playCountdownSound() { playSound("audio/countdown.wav"); }
    private void playShowSound() { playSound("audio/show.wav"); }
    private void playSelectSound() { playSound("audio/select.wav"); }
    private void playCheatingSound() { playSound("audio/cheating.wav"); }
    private void playHoverSound() { playSound("audio/hover.wav"); }

    private void setupMenuPanel() {
        menuPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                try {
                    Image bg = new ImageIcon(getResource("/icons/menu_bg.jpg")).getImage();
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                } catch (Exception e) {
                    showErrorDialog("Image Error", "Failed to load menu background");
                }
            }
        };
        menuPanel.setLayout(new GridBagLayout());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        
        JLabel title = new JLabel("Select Game Mode", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton pvpButton = createMenuButton("Player vs Player", e -> startGame("PvP"));
        JButton pvcButton = createMenuButton("Player vs Computer", e -> startGame("PvC"));
        
        JPanel volumePanel = createVolumePanel();

        buttonPanel.add(title);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        buttonPanel.add(pvpButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        buttonPanel.add(pvcButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        buttonPanel.add(volumePanel);

        menuPanel.add(buttonPanel);
    }

    private JButton createMenuButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.addActionListener(action);
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                playHoverSound();
                button.setBackground(new Color(120, 80, 40));
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(new Color(101, 67, 33));
            }
        });
        button.setBackground(new Color(101, 67, 33));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.PLAIN, 16));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(250, 60));
        button.setMinimumSize(new Dimension(250, 60));
        button.setPreferredSize(new Dimension(250, 60));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        return button;
    }

    private JPanel createVolumePanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
        
        ImageIcon soundIcon = new ImageIcon(getResource("/icons/sound-icon.png"));
        soundIcon = new ImageIcon(soundIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
        JLabel soundLabel = new JLabel(soundIcon);
        
        JSlider volumeSlider = new JSlider(0, 100, 100);
        volumeSlider.setPreferredSize(new Dimension(150, 30));
        volumeSlider.setMaximumSize(new Dimension(150, 30));
        volumeSlider.setOpaque(false);
        volumeSlider.setForeground(Color.WHITE);
        volumeSlider.setBackground(new Color(65, 105, 225, 100));
        volumeSlider.setUI(new javax.swing.plaf.basic.BasicSliderUI(volumeSlider) {
            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.WHITE);
                g2d.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
                g2d.dispose();
            }
            
            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(65, 105, 225, 100));
                g2d.fillRoundRect(trackRect.x, trackRect.y + trackRect.height/2 - 2, 
                                 trackRect.width, 4, 2, 2);
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(trackRect.x, trackRect.y + trackRect.height/2 - 2, 
                                 thumbRect.x + thumbRect.width/2, 4, 2, 2);
                g2d.dispose();
            }
        });
        
        volumeSlider.addChangeListener(e -> {
            int volume = volumeSlider.getValue();
            try {
                if (menuClip != null && menuClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) menuClip.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = (float) (Math.log(volume / 100.0) / Math.log(10.0) * 20.0);
                    gainControl.setValue(dB);
                }
            } catch (Exception ex) {
                System.err.println("Volume control error: " + ex.getMessage());
            }
        });
        
        panel.add(soundLabel);
        panel.add(Box.createRigidArea(new Dimension(10, 0)));
        panel.add(volumeSlider);
        return panel;
    }

    private void setupGamePanel() {
        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                try {
                    Image bg = new ImageIcon(getResource("/icons/game_bg.jpg")).getImage();
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                    g2d.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                    g2d.dispose();
                } catch (Exception e) {
                    showErrorDialog("Image Error", "Failed to load game background");
                }
            }
        };
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));
        gamePanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        countdownLabel = new JLabel("Get Ready", SwingConstants.CENTER);
        countdownLabel.setFont(new Font("Verdana", Font.BOLD, 32));
        countdownLabel.setBackground(new Color(101, 67, 33));
        countdownLabel.setForeground(Color.WHITE);
        countdownLabel.setBorder(BorderFactory.createEmptyBorder(20, 60, 20, 60));
        countdownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        resultLabel = new JLabel("", SwingConstants.CENTER);
        resultLabel.setFont(new Font("Verdana", Font.PLAIN, 20));
        resultLabel.setForeground(new Color(50, 50, 50));
        resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        scoreLabel = new JLabel("0 - 0", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Verdana", Font.BOLD, 28));
        scoreLabel.setForeground(new Color(101, 67, 33));
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        player1ImageLabel = createGestureLabel("P1");
        player2ImageLabel = createGestureLabel("P2");

        JPanel gesturePanel = new JPanel(new GridLayout(1, 2, 20, 0));
        gesturePanel.setOpaque(false);
        gesturePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gesturePanel.add(player1ImageLabel);
        gesturePanel.add(player2ImageLabel);

        player1StatusLabel = createPlayerStatusLabel("/icons/player1_waiting.png", 450, 150);
        player2StatusLabel = createPlayerStatusLabel("/icons/player2_waiting.png", 450, 150);

        JPanel statusPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        statusPanel.setOpaque(false);
        statusPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusPanel.add(player1StatusLabel);
        statusPanel.add(player2StatusLabel);

        gamePanel.add(countdownLabel);
        gamePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        gamePanel.add(resultLabel);
        gamePanel.add(scoreLabel);
        gamePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        gamePanel.add(gesturePanel);
        gamePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        gamePanel.add(statusPanel);
    }

    private JLabel createGestureLabel(String watermarkText) {
        JLabel label = new JLabel() {
            @Override
            public void setIcon(Icon icon) {
                if (icon != null) {
                    // Scale down the gesture icons to fit better
                    ImageIcon imageIcon = (ImageIcon) icon;
                    Image img = imageIcon.getImage();
                    Image scaledImg = img.getScaledInstance(120, 120, Image.SCALE_SMOOTH);
                    super.setIcon(new ImageIcon(scaledImg));
                } else {
                    super.setIcon(null);
                }
            }
        };
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(180, 180));
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(101, 67, 33), 4, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        label.setLayout(new BorderLayout());
        
        JLabel watermark = new JLabel(watermarkText, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                if (countdown == 3) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
                    super.paintComponent(g2d);
                }
                g2d.dispose();
            }
        };
        watermark.setFont(new Font("Arial", Font.BOLD, 80));
        watermark.setForeground(new Color(0, 0, 0, 50));
        label.add(watermark, BorderLayout.CENTER);
        
        return label;
    }

    private JLabel createPlayerStatusLabel(String imagePath, int width, int height) {
        try {
            ImageIcon icon = new ImageIcon(getResource(imagePath));
            Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new JLabel(new ImageIcon(scaledImage)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    float alpha = (this == player1StatusLabel && keyPressedPlayer1) || 
                                (this == player2StatusLabel && (mode.equals("PvC") || keyPressedPlayer2)) 
                                ? 0.4f : 1.0f;
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    super.paintComponent(g2d);
                    g2d.dispose();
                }
            };
        } catch (Exception e) {
            showErrorDialog("Image Error", "Failed to load player status image");
            return new JLabel();
        }
    }

    private void startGame(String selectedMode) {
        this.mode = selectedMode;
        player1ImageLabel.setIcon(null);
        player2ImageLabel.setIcon(null);
        cardLayout.show(mainPanel, "game");
        resetGame();
        startCountdown();
    }

    private void resetGame() {
        player1KeyPressCount = 0;
        player2KeyPressCount = 0;
        player1Choice = "";
        player2Choice = "";
        keyPressedPlayer1 = false;
        keyPressedPlayer2 = false;
        countdown = 3;
        round = 0;
        player1Score = 0;
        player2Score = 0;
        countdownLabel.setText("3");
        resultLabel.setText("");
        scoreLabel.setText("0 - 0");
        playerHistory.clear();
    }

    private void startCountdown() {
        countdownTimer = new Timer(1000, e -> {
            if (countdown > 0) {
                playCountdownSound();
                countdownLabel.setText(String.valueOf(countdown));
                countdown--;
            } else {
                countdownTimer.stop();
                processRound();
            }
        });
        countdownTimer.start();
    }

    private void processRound() {
        boolean player1Cheated = player1KeyPressCount > 1;
        boolean player2Cheated = player2KeyPressCount > 1;
        String cheatMessage = null;

        if (mode.equals("PvP")) {
            if (player1Cheated && !player2Cheated) {
                player2Score++;
                cheatMessage = "Player 1 cheated! Player 2 wins the round.";
            } else if (player2Cheated && !player1Cheated) {
                player1Score++;
                cheatMessage = "Player 2 cheated! Player 1 wins the round.";
            } else if (player1Cheated && player2Cheated) {
                cheatMessage = "Both players cheated! Round invalid.";
            }
        }

        round++;
        
        if (cheatMessage != null) {
            playCheatingSound();
            resultLabel.setText(cheatMessage);
            if (!(player1Cheated && player2Cheated)) updateScore();
        } else if (mode.equals("PvP")) {
            if (keyPressedPlayer1 && keyPressedPlayer2) {
                showGestures(player1Choice, player2Choice);
                resultLabel.setText(getWinner(player1Choice, player2Choice));
                updateScore();
            } else {
                resultLabel.setText("Invalid round. Players didn't choose.");
            }
        } else {
            if (keyPressedPlayer1) {
                String computerChoice = smartComputerChoice();
                showGestures(player1Choice, computerChoice);
                resultLabel.setText(getWinner(player1Choice, computerChoice));
                playerHistory.add(player1Choice);
                if (playerHistory.size() > 5) {
                    playerHistory.remove(0);
                }
                updateScore();
            } else {
                resultLabel.setText("You didn't choose in time!");
            }
        }

        if (player1Score > totalRounds / 2 || player2Score > totalRounds / 2 || round >= totalRounds) {
            showMatchResult();
        } else {
            new Timer(2000, e -> {
                ((Timer)e.getSource()).stop();
                resetRound();
            }).start();
        }
    }

    private void showMatchResult() {
        String finalResult = player1Score > player2Score ? "Player 1 Wins the Match!" :
                          player2Score > player1Score ? (mode.equals("PvP") ? "Player 2 Wins the Match!" : "Computer Wins the Match!") :
                          "The Match is a Tie!";
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel label = new JLabel(finalResult, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 20));
        label.setForeground(new Color(101, 67, 33));
        
        panel.add(label, BorderLayout.CENTER);
        
        JOptionPane.showOptionDialog(
            this,
            panel,
            "Game Over",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            new Object[]{"OK"},
            "OK"
        );
        
        cardLayout.show(mainPanel, "menu");
    }

    private void resetRound() {
        player1Choice = "";
        player2Choice = "";
        keyPressedPlayer1 = false;
        keyPressedPlayer2 = false;
        player1KeyPressCount = 0;
        player2KeyPressCount = 0;
        countdown = 3;
        countdownLabel.setText("3");
        resultLabel.setText("");
        player1ImageLabel.setIcon(null);
        player2ImageLabel.setIcon(null);
        startCountdown();
    }

    private void showGestures(String p1, String p2) {
        playShowSound();
        try {
            player1ImageLabel.setIcon(new ImageIcon(getResource("/icons/" + getGestureFileName(p1))));
            player2ImageLabel.setIcon(new ImageIcon(getResource("/icons/" + getGestureFileName(p2))));
        } catch (Exception e) {
            showErrorDialog("Image Error", "Failed to load gesture images");
        }
    }

    private String getWinner(String p1, String p2) {
        if (p1.equals(p2)) return "It's a tie!";
        if ((p1.equals("🪨") && p2.equals("✂️")) ||
            (p1.equals("📄") && p2.equals("🪨")) ||
            (p1.equals("✂️") && p2.equals("📄"))) {
            player1Score++;
            return "Player 1 wins!";
        } else {
            player2Score++;
            return mode.equals("PvP") ? "Player 2 wins!" : "Computer wins!";
        }
    }

    private String smartComputerChoice() {
        if (playerHistory.isEmpty()) {
            return switch (new Random().nextInt(3)) {
                case 0 -> "🪨";
                case 1 -> "📄";
                case 2 -> "✂️";
                default -> "🪨";
            };
        }
        String last = playerHistory.get(playerHistory.size() - 1);
        return switch (last) {
            case "🪨" -> "📄";
            case "📄" -> "✂️";
            case "✂️" -> "🪨";
            default -> "🪨";
        };
    }

    private String getGestureFileName(String emoji) {
        return switch (emoji) {
            case "🪨" -> "rock.png";
            case "📄" -> "paper.png";
            case "✂️" -> "scissors.png";
            default -> "";
        };
    }

    private void updateScore() {
        scoreLabel.setText(player1Score + " - " + player2Score);
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (countdown > 0) {
            char key = e.getKeyChar();
            if (mode.equals("PvC")) {
                if (!keyPressedPlayer1) {
                    playSelectSound();
                    player1Choice = mapKeyToChoice(key);
                    if (!player1Choice.isEmpty()) {
                        keyPressedPlayer1 = true;
                        player1StatusLabel.repaint();
                    }
                }
            } else {
                switch (key) {
                    case 'a': 
                        player1KeyPressCount++; 
                        if (!keyPressedPlayer1) { 
                            playSelectSound(); 
                            player1Choice = "🪨"; 
                            keyPressedPlayer1 = true; 
                            player1StatusLabel.repaint(); 
                        } 
                        break;
                    case 's': 
                        player1KeyPressCount++; 
                        if (!keyPressedPlayer1) { 
                            playSelectSound(); 
                            player1Choice = "📄"; 
                            keyPressedPlayer1 = true; 
                            player1StatusLabel.repaint(); 
                        } 
                        break;
                    case 'd': 
                        player1KeyPressCount++; 
                        if (!keyPressedPlayer1) { 
                            playSelectSound(); 
                            player1Choice = "✂️"; 
                            keyPressedPlayer1 = true; 
                            player1StatusLabel.repaint(); 
                        } 
                        break;
                    case 'j': 
                        player2KeyPressCount++; 
                        if (!keyPressedPlayer2) { 
                            playSelectSound(); 
                            player2Choice = "🪨"; 
                            keyPressedPlayer2 = true; 
                            player2StatusLabel.repaint(); 
                        } 
                        break;
                    case 'k': 
                        player2KeyPressCount++; 
                        if (!keyPressedPlayer2) { 
                            playSelectSound(); 
                            player2Choice = "📄"; 
                            keyPressedPlayer2 = true; 
                            player2StatusLabel.repaint(); 
                        } 
                        break;
                    case 'l': 
                        player2KeyPressCount++; 
                        if (!keyPressedPlayer2) { 
                            playSelectSound(); 
                            player2Choice = "✂️"; 
                            keyPressedPlayer2 = true; 
                            player2StatusLabel.repaint(); 
                        } 
                        break;
                }
            }
        }
    }

    private String mapKeyToChoice(char key) {
        return switch (key) {
            case 'a' -> "🪨";
            case 's' -> "📄";
            case 'd' -> "✂️";
            default -> "";
        };
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        System.setProperty("sun.awt.noerasebackground", "true");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        
        SwingUtilities.invokeLater(() -> {
            try {
                new RockPaperScissorsGame();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, 
                    "Failed to start: " + e.getMessage(),
                    "Critical Error", 
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}