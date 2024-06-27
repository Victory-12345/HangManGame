import javax.swing.*;
import java.awt.*;

public class HangmanPanel extends JPanel {
    private int errors;

    public HangmanPanel() {
        setPreferredSize(new Dimension(300, 400)); // 为该组件向其父容器建议一个首选大小
        setBackground(Color.WHITE); // 设置背景颜色为白色
    }

    public void setErrors(int errors) {
        this.errors = errors;
    } // 设置错误次数

    public void reset() {
        errors = 0;
    } // 重置错误次数为0 开始下一轮游戏

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // 确保在自定义绘制前，面板被适当地清除

        // 设置抗锯齿
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 获取面板的宽度和高度
        int width = getWidth();
        int height = getHeight();

        // 计算中心位置
        int centerX = width / 2;
        int centerY = height / 2;

        // 绞刑台尺寸和位置参数
        int baseY = centerY + 150;
        int poleX = centerX - 50;
        int beamY = centerY - 150;
        int ropeX = centerX + 50;

        // 设置颜色
        g2d.setColor(Color.RED); // 支架设置为红色
        g2d.setStroke(new BasicStroke(5)); // 设置粗细较大的线条

        // 绘制绞刑台
        g2d.drawLine(poleX - 100, baseY, poleX + 100, baseY); // 底部横线
        g2d.drawLine(poleX, baseY, poleX, beamY); // 垂直支柱
        g2d.drawLine(poleX, beamY, ropeX, beamY);  // 顶部横梁
        g2d.drawLine(ropeX, beamY, ropeX, beamY + 50); // 挂钩

        // 设置颜色为蓝色，绘制小人
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(2)); // 设置粗细较小的线条
        // 绘制小人部分（相对于绞刑台挂钩的位置）
        if (errors > 0) { // 头部
            g2d.drawOval(ropeX - 20, beamY + 50, 40, 40);
        }
        if (errors > 1) { // 身体
            g2d.drawLine(ropeX, beamY + 90, ropeX, beamY + 170);
        }
        if (errors > 2) { // 左臂
            g2d.drawLine(ropeX, beamY + 110, ropeX - 30, beamY + 130);
        }
        if (errors > 3) { // 右臂
            g2d.drawLine(ropeX, beamY + 110, ropeX + 30, beamY + 130);
        }
        if (errors > 4) { // 左腿
            g2d.drawLine(ropeX, beamY + 170, ropeX - 30, beamY + 210);
        }
        if (errors > 5) { // 右腿
            g2d.drawLine(ropeX, beamY + 170, ropeX + 30, beamY + 210);
        }
        if (errors > 6) { // 眼睛和嘴
            // 左眼
            g2d.drawLine(ropeX - 10, beamY + 60, ropeX - 5, beamY + 65);
            g2d.drawLine(ropeX - 5, beamY + 60, ropeX - 10, beamY + 65);
            // 右眼
            g2d.drawLine(ropeX + 5, beamY + 60, ropeX + 10, beamY + 65);
            g2d.drawLine(ropeX + 10, beamY + 60, ropeX + 5, beamY + 65);
            // 嘴巴
            g2d.drawArc(ropeX - 5, beamY + 80, 10, 5, 0, 180); // 半圆形嘴巴
        }
    }
}
