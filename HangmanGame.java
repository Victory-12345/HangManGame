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
        EASY(5, 6), // 简单模式，单词长度为5到6
        MEDIUM(7, 9), // 中等模式，单词长度为7到9
        HARD(10, Integer.MAX_VALUE); // 困难模式，单词长度为10及以上

        private final int minLength; // 最小长度
        private final int maxLength; // 最大长度

        Difficulty(int minLength, int maxLength) {
            this.minLength = minLength;
            this.maxLength = maxLength;
        }

        // 判断单词长度是否符合当前难度
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

    private static final int MAX_ERRORS = 7; // 最大错误次数

    private HangmanPanel hangmanPanel;
    private JTextField wordField;
    private JTextArea messageArea;
    private JButton nextWordButton;
    private JButton giveUpButton;
    private JButton hintButton;
    private JMenuBar menuBar;
    private JMenuItem nextWordMenuItem;
    private JMenuItem giveUpMenuItem;
    private JMenuItem exitMenuItem;
    private List<JButton> letterButtons = new ArrayList<>();
    private String currentWord;
    private char[] guessedWord;
    private int errors;
    private int hintsUsed;
    private int DifficultyType;

    private boolean isFirstUpdate; // 标记是否是第一次调用 updateInfoLabel

    // 构造函数，初始化游戏窗口
    public HangmanGame() {
        super();
        setTitle("Hangman Game"); // 设置窗口标题
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 关闭窗口时退出应用
        setSize(800, 600); // 设置窗口大小
        initializeComponents(); // 初始化组件
        setVisible(true); // 显示窗口
        selectDifficultyAndStartGame(); // 选择难度并开始游戏
    }

    // 初始化游戏组件
    private void initializeComponents() {
        Container cp = getContentPane(); // 获取内容面板
        cp.setLayout(new BorderLayout()); // 设置布局为边界布局

        JPanel wordPanel = createWordPanel(); // 创建显示单词和消息的面板
        hangmanPanel = new HangmanPanel(); // 创建绘制绞刑台和小人的面板

        // 创建 JSplitPane，将 wordPanel 和 hangmanPanel 进行水平分隔
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, wordPanel, hangmanPanel);
        splitPane.setResizeWeight(0.5); // 设置两部分各占一半的空间

        // 将分隔面板添加到中间区域
        cp.add(splitPane, BorderLayout.CENTER);

        JPanel lettersPanel = createLettersPanel(); // 创建字母按钮面板
        cp.add(lettersPanel, BorderLayout.NORTH); // 添加到北部区域

        JPanel controlPanel = createControlPanel(); // 创建控制按钮面板
        cp.add(controlPanel, BorderLayout.SOUTH); // 添加到南部区域

        setupMenuBar(); // 设置菜单栏
    }

    // 选择难度并开始游戏
    private void selectDifficultyAndStartGame() {
        Difficulty selectedDifficulty = askForDifficulty(); // 弹出对话框让用户选择难度

        if (selectedDifficulty != null) {
            newGame(selectedDifficulty); // 根据选择的难度开始新游戏
        } else {
            JOptionPane.showMessageDialog(this, "选择了无效的难度。将以中等难度开始游戏。");
            newGame(Difficulty.MEDIUM); // 默认选择中等难度
        }
    }

    // 弹出对话框让用户选择游戏难度
    private Difficulty askForDifficulty() {
        Object[] options = { "简单", "中等", "困难" }; // 选项列表
        int choice = JOptionPane.showOptionDialog(this,
                "请选择游戏难度:", // 对话框消息
                "选择难度", // 对话框标题
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]); // 默认选中中等难度

        switch (choice) {
            case 0:
                DifficultyType = 0; // 简单难度
                return Difficulty.EASY;
            case 1:
                DifficultyType = 1; // 中等难度
                return Difficulty.MEDIUM;
            case 2:
                DifficultyType = 2; // 困难难度
                return Difficulty.HARD;
            default:
                return null;
        }
    }

    // 创建显示单词和消息的面板
    private JPanel createWordPanel() {
        JPanel wordPanel = new JPanel();
        wordPanel.setLayout(new BorderLayout()); // 设置布局为边界布局
        Color bgColor = Color.WHITE; // 统一的背景颜色

        // 设置消息区域
        messageArea = new JTextArea();
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 16)); // 设置字体
        messageArea.setEditable(false); // 不可编辑
        messageArea.setBackground(bgColor); // 设置背景颜色
        wordPanel.add(messageArea, BorderLayout.CENTER); // 添加到中间区域

        // 设置显示单词的文本字段
        wordField = new JTextField();
        wordField.setFont(new Font("Monospaced", Font.BOLD, 24)); // 设置字体
        wordField.setEditable(false); // 不可编辑
        wordField.setBackground(bgColor); // 设置背景颜色
        wordPanel.add(wordField, BorderLayout.SOUTH); // 添加到南部区域

        return wordPanel;
    }

    // 创建字母按钮面板
    private JPanel createLettersPanel() {
        JPanel lettersPanel = new JPanel();
        lettersPanel.setLayout(new GridLayout(2, 13)); // 调整网格布局以适应26个字母，分为2行
        for (char c = 'A'; c <= 'Z'; c++) {
            JButton button = new JButton(String.valueOf(c));
            button.addActionListener(e -> checkLetter(button)); // 为按钮添加监听器
            letterButtons.add(button); // 将按钮添加到列表
            lettersPanel.add(button); // 将按钮添加到面板
        }
        return lettersPanel;
    }

    // 创建控制按钮面板
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER)); // 设置布局为流式布局

        nextWordButton = new JButton("下一个单词");
        nextWordButton.addActionListener(e -> selectDifficultyAndStartGame()); // 点击按钮选择难度并开始游戏
        controlPanel.add(nextWordButton); // 添加到控制面板

        giveUpButton = new JButton("放弃猜测");
        giveUpButton.addActionListener(e -> giveUp()); // 点击按钮放弃猜测
        controlPanel.add(giveUpButton); // 添加到控制面板

        // 新增提示按钮
        hintButton = new JButton("提示");
        hintButton.addActionListener(e -> giveHint()); // 点击按钮提供提示
        controlPanel.add(hintButton); // 添加到控制面板

        getContentPane().add(controlPanel, BorderLayout.SOUTH); // 将控制面板添加到南部区域
        return controlPanel;
    }

    // 提供提示
    private void giveHint() {
        int maxHints;
        switch (DifficultyType) {
            case 0: maxHints = 2; break; // 简单模式允许最多两次提示
            case 1: maxHints = 3; break; // 中等模式允许最多三次提示
            case 2: maxHints = 4; break; // 困难模式允许最多四次提示
            default: maxHints = 0; break; // 默认情况下没有提示
        }

        // 检查是否还能提供提示
        if (hintsUsed < maxHints) {
            // 找到第一个未揭示的字母并显示
            for (int i = 0; i < guessedWord.length; i += 2) {
                if (guessedWord[i] == '_') {
                    guessedWord[i] = currentWord.charAt(i / 2); // 显示字母
                    wordField.setText(new String(guessedWord)); // 更新显示
                    messageArea.append(String.format("提示: '%c' 是单词的一个字母。\n", currentWord.charAt(i / 2)));
                    hintsUsed++; // 增加已使用的提示次数
                    if (hintsUsed >= maxHints) {
                        hintButton.setEnabled(false); // 在使用了最大次数提示后禁用提示按钮
                    }
                    return;
                }
            }
        } else {
            messageArea.append("已经用完所有提示。\n");
            hintButton.setEnabled(false); // 在使用了所有提示后禁用提示按钮
        }
    }

    // 设置菜单栏
    private void setupMenuBar() {
        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("选项"); // 创建“选项”菜单

        nextWordMenuItem = new JMenuItem("下一个单词");
        nextWordMenuItem.addActionListener(e -> selectDifficultyAndStartGame()); // 点击菜单项选择难度并开始游戏
        fileMenu.add(nextWordMenuItem); // 添加到菜单

        giveUpMenuItem = new JMenuItem("放弃猜测");
        giveUpMenuItem.addActionListener(e -> giveUp()); // 点击菜单项放弃猜测
        fileMenu.add(giveUpMenuItem); // 添加到菜单

        exitMenuItem = new JMenuItem("退出游戏");
        exitMenuItem.addActionListener(e -> System.exit(0)); // 点击菜单项退出游戏
        fileMenu.add(exitMenuItem); // 添加到菜单

        menuBar.add(fileMenu); // 将菜单添加到菜单栏
        setJMenuBar(menuBar); // 设置窗口的菜单栏
    }


    private void newGame(Difficulty difficulty) {
        Random random = new Random();

        // 根据选择的难度过滤单词列表
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

        // 选择一个符合难度的单词
        currentWord = filteredWords.get(random.nextInt(filteredWords.size()));
        guessedWord = new char[currentWord.length() * 2 - 1];
        Arrays.fill(guessedWord, '_');
        for (int i = 1; i < guessedWord.length; i += 2) {
            guessedWord[i] = ' ';
        }

        // 重置游戏状态
        errors = 0;
        hintsUsed = 0; // 重置提示次数
        wordField.setText(new String(guessedWord)); // 显示初始猜测状态
        messageArea.setText("");
        isFirstUpdate = true; // 标记为第一次更新
        updateInfoLabel(); // 更新信息标签
        hangmanPanel.reset(); // 重置绞刑台
        hangmanPanel.repaint();
        enableLetterButtons(); // 启用字母按钮
        hintButton.setEnabled(true); // 启用提示按钮
    }

    private void enableLetterButtons() {
        for (JButton button : letterButtons) {
            button.setEnabled(true);
        }
    }

    private void checkLetter(JButton button) {
        char letter = button.getText().charAt(0);
        button.setEnabled(false); // 禁用已点击的按钮
        boolean correct = false;

        // 检查字母是否存在于单词中
        for (int i = 0; i < currentWord.length(); i++) {
            if (currentWord.charAt(i) == letter) {
                guessedWord[i * 2] = letter; // 在正确位置显示字母
                correct = true;
            }
        }

        // 更新显示猜测状态
        wordField.setText(new String(guessedWord));

        if (correct) {
            messageArea.append(String.format("恭喜，'%c' 是单词的组成字母。\n", letter));
        } else {
            messageArea.append(String.format("遗憾，'%c' 不是单词的组成字母。\n", letter));
            errors++;
            hangmanPanel.setErrors(errors);
            hangmanPanel.repaint();

            // 检查是否达到最大错误次数
            if (errors >= MAX_ERRORS) {
                messageArea.append(String.format("游戏结束! 单词是: %s\n", currentWord));
                disableLetterButtons();
                return; // 游戏结束，不再更新信息
            }
        }

        // 检查是否猜中整个单词
        if (new String(guessedWord).replace(" ", "").equals(currentWord)) {
            messageArea.append("恭喜! 你猜中了单词。\n");
            disableLetterButtons();
            return; // 游戏成功，不再更新信息
        }

        // 更新当前状态信息
        updateInfoLabel();
    }

    private void giveUp() {
        // 显示完整单词
        for (int i = 0; i < currentWord.length(); i++) {
            guessedWord[i * 2] = currentWord.charAt(i);
        }
        wordField.setText(new String(guessedWord)); // 更新显示
        messageArea.append("你放弃了! 单词是: " + currentWord + "\n");
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
            isFirstUpdate = false; // 标记为非第一次更新
        } else {
            messageArea.append(String.format("请选下一个字母，剩余尝试次数为: %d次\n",
                    MAX_ERRORS - errors));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HangmanGame::new);
    }
}
