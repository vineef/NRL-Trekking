
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;

public class CamDetect{

    private JFrame frame;
    private VideoCapture cap;
    private JLabel label;

    private CascadeClassifier normalCascade;
    private CascadeClassifier hsvCascade;
    private CascadeClassifier inRangeCascade;
    
    private EstruturaAmostras amostras;

    private final int CAMERA_ID = 0;
    private String XML_normal = "18N.xml";
    private String XML_hsv = "21H.xml";
    private String XML_inRange = "20In.xml";
    
    private final int IMG_SCALE = 1;
    
    CamDetect() {
        init();
        thread();
    }

    public static void main(String[] args) {
        new CamDetect();
    }

    public void thread() {
        new Thread() {
            @Override
            public void run() {
                Mat normal = new Mat();
                Mat inRange = new Mat();
                Mat hsv = new Mat();
                while (frame.isEnabled()) {
                    cap.read(normal);
                    
                    Imgproc.resize(normal, normal, new Size(normal.cols()*IMG_SCALE,normal.rows()*IMG_SCALE));           
                    
                    inRange = normal.clone();
                    hsv = normal.clone();                    
                    
                    //Faz o tratamento na imagem para usar o cascade com InRange e HSV
                    Imgproc.cvtColor(normal, hsv, Imgproc.COLOR_BGR2HSV); //HSV
                    //InRange Parte ----------------------------------------------------------
                    Mat im1 = new Mat();
                    Mat im2 = new Mat();
                    Core.inRange(hsv, new Scalar(0,80,30), new Scalar(13,230,255), im1);
                    Core.inRange(hsv, new Scalar(170,80,30), new Scalar(255,230,255), im2);
                    Core.bitwise_or(im1, im2, inRange);
                    //------------------------------------------------------------------------
                    
                    MatOfRect detectionsNormal = new MatOfRect();
                    MatOfRect detectionsHSV = new MatOfRect();
                    MatOfRect detectionsInRange = new MatOfRect();
                    
                    normalCascade.detectMultiScale(normal, detectionsNormal);
                    hsvCascade.detectMultiScale(hsv, detectionsHSV);                    
                    inRangeCascade.detectMultiScale(inRange, detectionsInRange);
                    
                    amostras.saveObjs(detectionsNormal, ObjDetec.CascOrigin.Normal); //Salva as amostras Normais
                    amostras.saveObjs(detectionsInRange, ObjDetec.CascOrigin.InRange); //Salva as amostras InRange
                    amostras.saveObjs(detectionsHSV, ObjDetec.CascOrigin.HSV); //Salva as amostras HSV
                    
                    ObjDetec o = amostras.evaluateSamples(); //Encontra a amostra positiva verdadeira
                    if(o!=null){
                        Rect r = o.toRect();
                        Imgproc.rectangle(normal, new Point(r.x, r.y),
                            new Point(r.x + r.width, r.y + r.height), new Scalar(0, 255, 255), 2);
                    }
                    
                    /*
                    for (Rect rect : detectionsNormal.toArray()) {
                        Imgproc.rectangle(normal, new Point(rect.x, rect.y), new Point(rect.x
                                + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 2);
                    }
                    for (Rect rect : detectionsHSV.toArray()) {
                        Imgproc.rectangle(normal, new Point(rect.x, rect.y), new Point(rect.x
                                + rect.width, rect.y + rect.height), new Scalar(0, 255, 0), 2);
                    }
                    
                    for (Rect rect : detectionsInRange.toArray()) {
                        Imgproc.rectangle(normal, new Point(rect.x, rect.y), new Point(rect.x
                                + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 2);
                    }
                    */
					
                    
                    ImageIcon image = new ImageIcon(createAwtImage(normal)); //Converte Mat para ImageIcon
                    frame.setSize(image.getIconWidth(), image.getIconHeight()); //Configura o tamanho da janela como tamanho da imagem
                    label.setIcon(image); //Mostra a imagem
                }
            }
        }.start();
    }
    
    private void init() {
    	/* Configurando ClassPath manualmente para executar o programa pelo .jar
    	try {

            System.setProperty("java.library.path", "lib");

            Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(null, null);

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }*/
    	
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        //System.loadLibrary("opencv_ffmpeg320_64"); //Biblioteca para leitura de video

        frame = new JFrame("NRL - Nucleo Robotica Leopoldina");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setResizable(true);
        frame.setLocationRelativeTo(null);

        label = new JLabel("");
        frame.getContentPane().add(label, BorderLayout.CENTER);

        frame.validate();
        frame.setVisible(true);

        cap = new VideoCapture();
        cap.open(CAMERA_ID);
        //cap.open("http://192.168.0.103:8080/video"); 

        normalCascade = new CascadeClassifier(getClass().getResource("/" + XML_normal).getPath().substring(1));
        hsvCascade = new CascadeClassifier(getClass().getResource("/" + XML_hsv).getPath().substring(1));
        inRangeCascade = new CascadeClassifier(getClass().getResource("/" + XML_inRange).getPath().substring(1));

        amostras = new EstruturaAmostras();
    }

    private static BufferedImage createAwtImage(Mat mat) { //Método para transformar Mat em ImageIcon
        int type = 0;
        if (mat.channels() == 1){
            type = BufferedImage.TYPE_BYTE_GRAY;
        } else if (mat.channels() == 3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        } else {
            return null;
        }

        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        WritableRaster raster = image.getRaster();
        DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
        byte[] data = dataBuffer.getData();
        mat.get(0, 0, data);

        return image;
    }
}
