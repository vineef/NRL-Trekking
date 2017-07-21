
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.lang.reflect.Field;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;

public class CamDetectPierre{

    private JFrame frame;
    private VideoCapture cap;
    private JLabel label;

    private String caminhoXML;
    private CascadeClassifier normalCascade;
    private CascadeClassifier hsvCascade;
    private CascadeClassifier inRangeCascade;
    
    private EstruturaAmostras amostras;

    private final int CAMERA_ID = 1;
    private String XML_normal = "18N.xml";
    private String XML_hsv = "19H.xml";
    private String XML_inRange = "20In.xml";

    CamDetectPierre() {
        init();
        thread();
    }

    public static void main(String[] args) {
        new CamDetectPierre();
    }

    public void thread() {
        new Thread() {
            @Override
            public void run() {
                Mat normal = new Mat();
                Mat inRange = new Mat();
                Mat hsv = new Mat();
                int absoluteFaceSize = 0;
                while (frame.isEnabled()) {
                    cap.read(normal);
                    //System.out.println("FPS: " + cap.get(Videoio.CAP_PROP_FPS));
                    
                    Imgproc.resize(normal, normal, new Size(normal.cols()*1,normal.rows()*1));
                    
                    //Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
                    //Imgproc.equalizeHist(mat, mat);
                    //Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2HSV);
                    //Imgproc.blur(mat, mat, new Size(2, 2));
                    //Imgproc.Canny(mat, mat, 100, 200, 3, false);
                    
                    //Canny--------------------------------------------------
//                    Imgproc.cvtColor(normal, normal, Imgproc.COLOR_BGR2GRAY);
//                    Imgproc.blur(normal, normal, new Size(2,2));
//                    Imgproc.Canny(normal, normal, 60, 180);
//                    Imgproc.dilate(normal, normal,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)));
					
                    if (absoluteFaceSize == 0) {
                        int height = normal.rows();
                        if (Math.round(height * 0.2f) > 0) {
                            absoluteFaceSize = Math.round(height * 0.2f);
                        }
                    }                    
                    
                    inRange = normal.clone();
                    hsv = normal.clone();                    
                    
                    //InRange Parte--------------------------------------------------------------------
                    //Faz o tratamento na imagem para usar o cascade preto e branco
                    Imgproc.cvtColor(normal, hsv, Imgproc.COLOR_BGR2HSV);
                    Mat im1 = new Mat();
                    Mat im2 = new Mat();
                    Core.inRange(hsv, new Scalar(0,80,30), new Scalar(13,230,255), im1);
                    Core.inRange(hsv, new Scalar(170,80,30), new Scalar(255,230,255), im2);
                    Core.bitwise_or(im1, im2, inRange);

                    //----------------------------------------------------------------------------------
                    
                    MatOfRect detectionsNormal = new MatOfRect();
                    MatOfRect detectionsHSV = new MatOfRect();
                    MatOfRect detectionsInRange = new MatOfRect();
                    
                    normalCascade.detectMultiScale(normal, detectionsNormal);
                    hsvCascade.detectMultiScale(hsv, detectionsHSV);                    
                    //inRangeCascade.detectMultiScale(inRange, detectionsInRange, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
                    //		new Size(absoluteFaceSize, absoluteFaceSize), new Size());
                    inRangeCascade.detectMultiScale(inRange, detectionsInRange);
                    //normal = img;
                    
                    amostras.saveObjs(detectionsNormal, ObjDetec.CascOrigin.Normal);//Amostras de InRange
                    amostras.saveObjs(detectionsInRange, ObjDetec.CascOrigin.InRange);//Amostras de InRange
                    amostras.saveObjs(detectionsHSV, ObjDetec.CascOrigin.HSV);//Amostras de InRange
                    
                    ObjDetec o = amostras.evaluateSamples();//Encontra a amostra positiva verdadeira
                    if(o!=null)
                    {
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
					
                    
                    ImageIcon image = new ImageIcon(createAwtImage(normal));

                    frame.setSize(image.getIconWidth(), image.getIconHeight());
                    label.setIcon(image);
                }
            }
        }.start();
    }
    
    private void init() {
    	
    	try {

            System.setProperty("java.library.path", "lib");

            Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(null, null);

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    	
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        //System.loadLibrary("opencv_ffmpeg320_64");

        frame = new JFrame("NRL - Nucleo Robotica Leopoldina PIERRE");
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

        XML_normal = "/" + XML_normal;
        caminhoXML = getClass().getResource(XML_normal).getPath();
        normalCascade = new CascadeClassifier(caminhoXML.substring(1));
        
        XML_hsv = "/" + XML_hsv;
        caminhoXML = getClass().getResource(XML_hsv).getPath();
        hsvCascade = new CascadeClassifier(caminhoXML.substring(1));
        
        XML_inRange = "/" + XML_inRange;
        caminhoXML = getClass().getResource(XML_inRange).getPath();
        inRangeCascade = new CascadeClassifier(caminhoXML.substring(1));
        E
        amostras = new EstruturaAmostras();
    }

    private static BufferedImage createAwtImage(Mat mat) {
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
