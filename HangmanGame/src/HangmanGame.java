import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.io.*;
import java.nio.file.*;

public class HangmanGame extends JFrame {
    private static String[] WORDS;
    private enum Difficulty {
        EASY(5, 6),
        MEDIUM(7, 9),
        HARD(10, Integer.MAX_VALUE);

        private final int minLength;
        private final int maxLength;

        Difficulty(int minLength, int maxLength) {
            this.minLength = minLength;
            this.maxLength = maxLength;
        }

        public boolean isWordLengthValid(int length) {
            return length >= minLength && length <= maxLength;
        }
    }

    static {
        try {
            WORDS = Files.lines(Paths.get("wordlist.txt"))
                    .toArray(String[]::new);
        } catch (IOException e) {
            e.printStackTrace();
            WORDS = new String[0];
        }
    }

    private static final int MAX_ERRORS = 7;

    private HangmanPanel hangmanPanel;
    private JTextField wordField;
    private JTextArea messageArea;
    private JButton nextWordButton;
    private JButton giveUpButton;
    private JMenuBar menuBar;
    private JMenuItem nextWordMenuItem;
    private JMenuItem giveUpMenuItem;
    private JMenuItem exitMenuItem;
    private List<JButton> letterButtons = new ArrayList<>();
    private String currentWord;
    private char[] guessedWord;
    private int errors;

    private boolean isFirstUpdate; // 标记是否是第一次调用 updateInfoLabel

    public HangmanGame() {
        super();
        setTitle("Hangman Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        initializeComponents();
        setVisible(true);
        selectDifficultyAndStartGame();
    }

    private void initializeComponents() {
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());

        JPanel wordPanel = createWordPanel();
        hangmanPanel = new HangmanPanel();

        // 创建 JSplitPane 将 wordPanel 和 hangmanPanel 进行水平分隔
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, wordPanel, hangmanPanel);
        splitPane.setResizeWeight(0.5); // 让两部分各占一半的空间

        // 添加分隔面板到中间区域
        cp.add(splitPane, BorderLayout.CENTER);

        JPanel lettersPanel = createLettersPanel();
        cp.add(lettersPanel, BorderLayout.NORTH);

        JPanel controlPanel = createControlPanel();
        cp.add(controlPanel, BorderLayout.SOUTH);

        setupMenuBar();
    }
    private void selectDifficultyAndStartGame() {
        Difficulty selectedDifficulty = askForDifficulty();

        if (selectedDifficulty != null) {
            newGame(selectedDifficulty);
        } else {
            JOptionPane.showMessageDialog(this, "选择了无效的难度。将以中等难度开始游戏。");
            newGame(Difficulty.MEDIUM); // Default to medium difficulty if selection is invalid
        }
    }
    private Difficulty askForDifficulty() {
        Object[] options = { "简单", "中等", "困难" };
        int choice = JOptionPane.showOptionDialog(this,
                "请选择游戏难度:",
                "选择难度",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);

        switch (choice) {
            case 0:
                return Difficulty.EASY;
            case 1:
                return Difficulty.MEDIUM;
            case 2:
                return Difficulty.HARD;
            default:
                return null; // Invalid choice
        }
    }

    private JPanel createWordPanel() {
        JPanel wordPanel = new JPanel();
        wordPanel.setLayout(new BorderLayout());
        Color bgColor = Color.WHITE; // 统一的背景颜色

        // 设置 messageArea
        messageArea = new JTextArea();
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        messageArea.setEditable(false);
        messageArea.setBackground(bgColor); // 设置背景颜色
        wordPanel.add(messageArea, BorderLayout.CENTER);

        // 设置 wordField
        wordField = new JTextField();
        wordField.setFont(new Font("Monospaced", Font.BOLD, 24));
        wordField.setEditable(false);
        wordField.setBackground(bgColor); // 设置背景颜色
        wordPanel.add(wordField, BorderLayout.SOUTH);

        return wordPanel;
    }

    private JPanel createLettersPanel() {
        JPanel lettersPanel = new JPanel();
        lettersPanel.setLayout(new GridLayout(2, 13)); // Adjusted grid layout for 26 letters in 2 rows
        for (char c = 'A'; c <= 'Z'; c++) {
            JButton button = new JButton(String.valueOf(c));
            button.addActionListener(e -> checkLetter(button));
            letterButtons.add(button);
            lettersPanel.add(button);
        }
        return lettersPanel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        nextWordButton = new JButton("下一个单词");
        nextWordButton.addActionListener(e -> selectDifficultyAndStartGame()); // Use selectDifficultyAndStartGame() instead of newGame()
        controlPanel.add(nextWordButton);

        giveUpButton = new JButton("放弃猜测");
        giveUpButton.addActionListener(e -> giveUp());
        controlPanel.add(giveUpButton);

        getContentPane().add(controlPanel, BorderLayout.SOUTH);
        return controlPanel;
    }

    private void setupMenuBar() {
        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("选项");

        nextWordMenuItem = new JMenuItem("下一个单词");
        nextWordMenuItem.addActionListener(e -> selectDifficultyAndStartGame()); // Use selectDifficultyAndStartGame() instead of newGame()
        fileMenu.add(nextWordMenuItem);

        giveUpMenuItem = new JMenuItem("放弃猜测");
        giveUpMenuItem.addActionListener(e -> giveUp());
        fileMenu.add(giveUpMenuItem);

        exitMenuItem = new JMenuItem("退出游戏");
        exitMenuItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void newGame(Difficulty difficulty) {
        Random random = new Random();

        // Filter words based on selected difficulty
        List<String> filteredWords = new ArrayList<>();
        for (String word : WORDS) {
            int length = word.length();
            if (difficulty.isWordLengthValid(length)) {
                filteredWords.add(word);
            }
        }

        if (filteredWords.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有符合选择难度的单词。");
            return;
        }

        currentWord = filteredWords.get(random.nextInt(filteredWords.size()));
        guessedWord = new char[currentWord.length() * 2 - 1];
        Arrays.fill(guessedWord, '_');
        for (int i = 1; i < guessedWord.length; i += 2) {
            guessedWord[i] = ' ';
        }
        errors = 0;
        wordField.setText(new String(guessedWord));
        messageArea.setText("");
        isFirstUpdate = true;
        updateInfoLabel();
        hangmanPanel.reset();
        hangmanPanel.repaint();
        enableLetterButtons();
    }

    private void enableLetterButtons() {
        for (JButton button : letterButtons) {
            button.setEnabled(true);
        }
    }


    private void checkLetter(JButton button) {
        char letter = button.getText().charAt(0);
        button.setEnabled(false);
        boolean correct = false;

        // 遍历 currentWord，检查 letter 是否匹配
        for (int i = 0; i < currentWord.length(); i++) {
            if (currentWord.charAt(i) == letter) {
                guessedWord[i * 2] = letter; // 在正确的下划线位置显示猜中的字母
                correct = true;
            }
        }

        // 更新 wordField 以显示当前猜测状态
        wordField.setText(new String(guessedWord)); // 使用 new String 确保正确显示

        if (correct) {
            messageArea.append(String.format("恭喜，'%c' 是单词的组成字母。 ", letter));
        } else {
            messageArea.append(String.format("遗憾，'%c' 不是单词的组成字母。 ", letter));
            errors++;
            hangmanPanel.setErrors(errors);
            hangmanPanel.repaint();

            if (errors >= MAX_ERRORS) {
                messageArea.append(String.format("游戏结束! 单词是: %s\n", currentWord));
                disableLetterButtons();
            }
        }

        // 检查是否赢得游戏
        if (new String(guessedWord).replace(" ", "").equals(currentWord)) {
            messageArea.append("恭喜! 你猜中了单词.\n");
            disableLetterButtons();
        }

        // 更新当前状态信息
        updateInfoLabel();
    }




    private void giveUp() {
        for (int i = 0; i < currentWord.length(); i++) {
            guessedWord[i * 2] = currentWord.charAt(i); // 显示完整单词
        }
        wordField.setText(new String(guessedWord)); // 使用 new String
        messageArea.setText("你放弃了! 单词是: " + currentWord);
        disableLetterButtons();
    }



    private void disableLetterButtons() {
        for (JButton button : letterButtons) {
            button.setEnabled(false);
        }
    }

    private void updateInfoLabel() {
        if (isFirstUpdate) {
            messageArea.append(String.format("单词长度为: %d，还可以猜测的次数为: %d次\n",
                    currentWord.length(), MAX_ERRORS - errors));
            isFirstUpdate = false; // 之后的调用将不会输出首次信息
        } else {
            messageArea.append(String.format("请选下一个字母，剩余尝试次数为: %d次\n",
                    MAX_ERRORS - errors));
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(HangmanGame::new);
    }
}
