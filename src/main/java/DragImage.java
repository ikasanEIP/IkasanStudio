import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class DragImage {
    public static void main(String args[]) {
        DragImage dragImage = new DragImage();
        dragImage.doStuff();
    }
    public void doStuff() {
        JFrame frame = new JFrame("Drag Image");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ImageIcon icon = new ImageIcon(this.getClass().getResource("/studio/icons/palette/ftp-consumer.png"));
//        Icon icon = new ImageIcon("computer.png");
        JLabel label = new JLabel(icon);
        ImageSelection imageSelection = new ImageSelection();
        frame.setTransferHandler(imageSelection);
        label.setTransferHandler(imageSelection);
        MouseListener listener = new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JComponent comp = (JComponent) me.getSource();
                TransferHandler handler = comp.getTransferHandler();
                handler.exportAsDrag(comp, me, TransferHandler.COPY);
            }
        };
        label.addMouseListener(listener);
        frame.add(new JScrollPane(label), BorderLayout.NORTH);

        frame.setSize(300, 150);
        frame.setVisible(true);
    }
}