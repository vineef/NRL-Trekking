
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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;

public class CamDetectP {

    private JFrame frame;
    private VideoCapture cap;
    private JLabel label;

    private String caminhoXML;
    private CascadeClassifier faceDetector;
    private CascadeClassifier faceDetector2;
    private MatOfRect detections;
    private EstruturaAmostras amostras;

    private final int CAMERA_ID = 0;
    private String XML = "18-Linux(bom).xml";
    private String XML2 = "InRange1.xml";

    CamDetectP() {
        init();
        thread();
    }

    public static void main(String[] args) {
        new CamDetectP();
    }

    public void thread() {
        new Thread() {
            @Override
            public void run() {
                Mat mat = new Mat();
                int absoluteFaceSize = 0;
                while (frame.isEnabled())
                {
                    cap.read(mat);
                    //Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
                    //Imgproc.equalizeHist(mat, mat);
                    //Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2HSV);
                    //Imgproc.blur(mat, mat, new Size(2, 2));
                    //Imgproc.Canny(mat, mat, 100, 200, 3, false);
                    
                    //Canny--------------------------------------------------
//                    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
//                    Imgproc.blur(mat, mat, new Size(2,2));
//                    Imgproc.Canny(mat, mat, 60, 180);
//                    Imgproc.dilate(mat, mat,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)));

                    if (absoluteFaceSize == 0) {
                        int height = mat.rows();
                        if (Math.round(height * 0.2f) > 0) {
                            absoluteFaceSize = Math.round(height * 0.2f);
                        }
                    }
                    Mat img = mat.clone();
                    
                    //InRange Parte--------------------------------------------------------------------
                    //Faz o tratamento na imagem para usar o cascade preto e branco
                    Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2HSV);
                    Mat im1 = new Mat();
                    Mat im2 = new Mat();
                    Core.inRange(img, new Scalar(0,80,30), new Scalar(13,230,255), im1);
                    Core.inRange(img, new Scalar(170,80,30), new Scalar(255,230,255), im2);
                    Core.bitwise_or(im1, im2, img);
                    //----------------------------------------------------------------------------------

                    //faceDetector.detectMultiScale(mat, detections, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
                    //          new Size(absoluteFaceSize, absoluteFaceSize), new Size());
                    faceDetector.detectMultiScale(mat, detections);
                    //mat = img;
                    amostras.saveObjs(detections, ObjDetec.CascOrigin.Normal);//Amostras Normais
                    
                    
                    MatOfRect detections2 = new MatOfRect();
                    faceDetector2.detectMultiScale(img, detections2, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
                    		new Size(absoluteFaceSize, absoluteFaceSize), new Size());
                    //faceDetector2.detectMultiScale(mat, detections2);
                    
                    amostras.saveObjs(detections2, ObjDetec.CascOrigin.InRange);//Amostras de InRange
                    
                    ObjDetec o = amostras.evaluateSamples();//Encontra a amostra positiva verdadeira
                    if(o!=null)
                    {
                        Rect r = o.toRect();
                        Imgproc.rectangle(mat, new Point(r.x, r.y),
                            new Point(r.x + r.width, r.y + r.height), new Scalar(0, 255, 255), 2);
                    }
                    
                    //Imgproc.circle(mat, new Point(r.x, r.y), 30, new Scalar(0, 255, 255));
                        

                    //if(detections.toArray().length>0)
                    //    System.out.println(String.format("Encontrado %s objetos", detections.toArray().length));
                    

                    for (Rect rect : detections.toArray()) {
                        Imgproc.rectangle(mat, new Point(rect.x, rect.y), new Point(rect.x
                                + rect.width, rect.y + rect.height), new Scalar(0, 255, 0), 2);
                    }
                    
                    for (Rect rect : detections2.toArray()) {
                        Imgproc.rectangle(mat, new Point(rect.x, rect.y), new Point(rect.x
                                + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 2);
                    }
                    
                    

                    ImageIcon image = new ImageIcon(createAwtImage(mat));

                    frame.setSize(image.getIconWidth(), image.getIconHeight());
                    label.setIcon(image);
                }
            }
        }.start();
    }

    private void init() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

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

        XML = "/" + XML;
        caminhoXML = getClass().getResource(XML).getPath();

        faceDetector = new CascadeClassifier(caminhoXML.substring(1));
        detections = new MatOfRect();
        
        XML2 = "/" + XML2;
        caminhoXML = getClass().getResource(XML2).getPath();

        faceDetector2 = new CascadeClassifier(caminhoXML.substring(1));
        amostras = new EstruturaAmostras();
    }

    private static BufferedImage createAwtImage(Mat mat) {
        int type = 0;
        if (mat.channels() == 1) {
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
