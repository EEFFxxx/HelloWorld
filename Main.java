import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

public class Main {
    private static final int HISTOGRAM_BOTTOM_PADDING = 20;

    public static void main(String[] args) {
        String inputPath = args.length > 0 ? args[0] : "lena512.bmp";
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            System.err.println("Input file not found: " + inputFile.getAbsolutePath());
            return;
        }

        try {
            BufferedImage source = ImageIO.read(inputFile);
            if (source == null) {
                System.err.println("Unsupported image format: " + inputFile.getAbsolutePath());
                return;
            }

            BufferedImage grayscale = toGrayscale(source);
            int[] histogram = computeHistogram(grayscale);
            BufferedImage histogramImage = createHistogramImage(histogram, 512, 320);

            File outputDir = new File("output");
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
            }

            saveImage(grayscale, new File(outputDir, "grayscale.png"));
            saveImage(histogramImage, new File(outputDir, "histogram.png"));

            System.out.println("Histogram values:");
            for (int i = 0; i < histogram.length; i++) {
                System.out.printf("%3d: %d%n", i, histogram[i]);
            }

            for (int bit = 0; bit < 8; bit++) {
                BufferedImage bitPlane = extractBitPlane(grayscale, bit);
                saveImage(bitPlane, new File(outputDir, String.format("bit_plane_%d.png", bit)));
                showIfPossible("Bit Plane " + bit, bitPlane);
            }

            showIfPossible("Original (Grayscale)", grayscale);
            showIfPossible("Histogram", histogramImage);
            System.out.println("Done. Output saved to: " + outputDir.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to process image: " + e.getMessage());
        }
    }

    private static BufferedImage toGrayscale(BufferedImage source) {
        BufferedImage grayscale = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayscale.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        return grayscale;
    }

    private static int[] computeHistogram(BufferedImage image) {
        int[] histogram = new int[256];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int gray = image.getRaster().getSample(x, y, 0);
                histogram[gray]++;
            }
        }
        return histogram;
    }

    private static BufferedImage createHistogramImage(int[] histogram, int width, int height) {
        BufferedImage chart = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = chart.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        int max = 0;
        for (int value : histogram) {
            if (value > max) {
                max = value;
            }
        }
        if (max == 0) {
            max = 1;
        }

        g.setColor(Color.BLACK);
        for (int i = 0; i < 256; i++) {
            int barHeight = (int) Math.round((histogram[i] / (double) max) * (height - HISTOGRAM_BOTTOM_PADDING));
            int x = i * width / 256;
            int nextX = (i + 1) * width / 256;
            int barWidth = Math.max(1, nextX - x);
            g.fillRect(x, height - barHeight - 1, barWidth, barHeight);
        }
        g.dispose();
        return chart;
    }

    private static BufferedImage extractBitPlane(BufferedImage image, int bitIndex) {
        BufferedImage bitPlane = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        int mask = 1 << bitIndex;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int gray = image.getRaster().getSample(x, y, 0);
                int bit = ((gray & mask) != 0) ? 255 : 0;
                bitPlane.getRaster().setSample(x, y, 0, bit);
            }
        }
        return bitPlane;
    }

    private static void saveImage(BufferedImage image, File path) throws IOException {
        ImageIO.write(image, "png", path);
    }

    private static void showIfPossible(String title, BufferedImage image) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(new JLabel(new ImageIcon(image)));
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }
}
