
import java.util.ArrayList;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;

/**
 * Classe usada para salvar e processar dados resultados de uma detecção do
 * {@linkplain org.opencv OpenCV} usando
 * {@linkplain org.opencv.objdetect.CascadeClassifier Cascade Classifier}. No
 * processamento são usados dados da detecção de 3 Cascade Classifiers
 * diferentes. Os resultados das detecções são comparados afim de reduzir os
 * erros (Falsos Positivos). Os cascades se diferenciam por possuir diferentes
 * tratamentos de imagem no seu treinamento, como a mudança de espaço de cores
 * para
 * {@linkplain org.opencv.imgproc.Imgproc#cvtColor(org.opencv.core.Mat, org.opencv.core.Mat, int) HSV}
 * e a transformação binária usando valores limiares com
 * {@linkplain org.opencv.core.Core#inRange(org.opencv.core.Mat, org.opencv.core.Scalar, org.opencv.core.Scalar, org.opencv.core.Mat) InRange()}.
 *
 * @author Pierre Alvim de Paula
 */
public class EstruturaAmostras
{

    /**
     * Lista para objetos encontrados com Cascade que não usa transformação.
     *
     * @see ObjDetec.CascOrigin#Normal
     */
    private ArrayList<ObjDetec> dNormal;
    /**
     * Lista para objetos encontrados com Cascade que usa "InRange" como
     * transformação.
     *
     * @see ObjDetec.CascOrigin#InRange
     */
    private ArrayList<ObjDetec> dInRange;

    /**
     * Lista para objetos encontrados com Cascade que usa "HSV" como
     * transformação.
     *
     * @see ObjDetec.CascOrigin#HSV
     */
    private ArrayList<ObjDetec> dHSV;

    /**
     * ()Lista bidimensional para salvar o objetos detectatos ao longo de varios
     * frames. A lista externa tem como index, um tempo de frame
     * ({@linkplain #frameTime frametime}), e a interna simplesmente contém os
     * objetos que foram achados nesse tempo de frame. Objetos da lista interna
     * com a flag {@linkplain ObjDetec#exact Exact} ligada, são considerados
     * como amostras positivas verdadeiras, e são na verdade, uma mescla de
     * amostras de diferentes cascades. Todos os elementos da lista interna tem
     * {@linkplain ObjDetec#cascade Cascade de origem} {@linkplain ObjDetec.CascOrigin#Indef Indefinido}.
     */
    private ArrayList<ArrayList<ObjDetec>> dFramesTruePositive;

    /**
     * Valor interio que determina o index do frame atual, e que tem como limite
     * maximo {@link #NUMFRAMES FRAME LIMIT}.
     */
    private int frameTime;
    /**
     * (SCALE of CONTAINER on TEST) Escala geral usada na função
     * {@linkplain ObjDetec#isCenterInside(ObjDetec, double) isCenterInside()}
     * para encontrar objetos proximos. É usado em
     * {@linkplain #relateObjByCasc() relateObjsByCasc}.
     */
    private static final double SCALECONTAINER = 0.5;
    /**
     * (NUMBER of FRAMES) O numero total de frames que tem informação salva das
     * amostras verdadeiras positivas. É indice máximo da lista
     * {@linkplain #dFramesTruePositive FramesTruePositive}.
     *
     * @see #frameTime
     * @see #frameAvance()
     * @see #dFramesTruePositive
     */
    private int NUMFRAMES = 3;
    
    /**
     * (LIMIT of DISTANCE for objects in diferent FRAMES) Limite de distância
     * entre objetos.(Não usado)
     */
    private static final int LIMDSTFRAME = 10;

    /**
     * (NUMBER LIMIT of CASCADE found) Numero limite de cascades diferentes onde
     * foram encontrado objetos similares para um amostra ser considerada
     * verdadeira positiva.
     *
     * @see #relateObjByCasc(int) relateObjByCasc
     */
    private static final int NUMLIMCASC = 2;

    /**
     * Construtor padrão que inicializa as listas e inicia o tempo em 0.
     */
    public EstruturaAmostras()
    {
        this.frameTime = 0;
        dFramesTruePositive = new ArrayList<>();
        dNormal = new ArrayList<>();
        dHSV = new ArrayList<>();
        dInRange = new ArrayList<>();
        int i;
        for (i = 0; i < NUMFRAMES; i++)
            dFramesTruePositive.add(new ArrayList<>());
    }
    
    public EstruturaAmostras(int numFrames)
    {
    	this.NUMFRAMES = numFrames;
        this.frameTime = 0;
        dFramesTruePositive = new ArrayList<>();
        dNormal = new ArrayList<>();
        dHSV = new ArrayList<>();
        dInRange = new ArrayList<>();
        int i;
        for (i = 0; i < NUMFRAMES; i++)
            dFramesTruePositive.add(new ArrayList<>());
    }

    /**
     * (SAVE OBJECTS into its list) Método para adicionar os objetos encontrados
     * em suas listas especificas.
     *
     * @param det Matriz de Retangulos da detecção.
     * @param orig Tipo do cascade usado na detecção.
     * @throws Error Se a origem do tipo de detecção for
     * {@linkplain ObjDetec.CascOrigin#Indef indefinida}.
     * @see Rect OpenCV Rect
     * @see ObjDetec.CascOrigin CascOrigin
     * @see #dNormal dNormal
     * @see #dHSV dHSV
     * @see #dInRange dInRange
     */
    public void saveObjs(MatOfRect det, ObjDetec.CascOrigin orig)
    {
        for (Rect rect : det.toArray()) {
            ObjDetec obj = new ObjDetec(rect, orig);
            switch (orig) {
                case Normal:
                    dNormal.clear();
                    obj.setCascade(orig);
                    dNormal.add(obj);
                    break;
                case HSV:
                    dHSV.clear();
                    obj.setCascade(orig);
                    dHSV.add(obj);
                    break;
                case InRange:
                    dInRange.clear();
                    obj.setCascade(orig);
                    dInRange.add(obj);
                    break;
                case Indef:
                    throw new Error("Não foi possivel salvar, pois o tipo de cascade é indefinido.");
            }
        }
    }

    /**
     * (START ARRAYLIST) Inicializa ou limpa uma lista de
     * {@linkplain ObjDetec objetos}.
     *
     * @param list Lista para iniciar.
     */
    private ArrayList startArray(ArrayList list)
    {
        if (list == null)
            list = new ArrayList<>();
        else
            list.clear();

        return list;
    }

    /**
     * (RELATE all OBEJCTS BY similarity among CASCADES lists) Encontra
     * elementos que estejam em mais de uma lista de cascades usando como
     * semelhança o teste {@linkplain ObjDetec#isCenterInside(ObjDetec, double)
     * isCenterInside}. Depois de encontrados, os elementos semelhates são
     * transformados em um só pela média dos seus valores, usando
     * {@linkplain #mergeObjsAvarage(java.util.ArrayList) mergeObjsAvarage}, e
     * salvo com a flag {@linkplain ObjDetec#exact Exact} ligada. Os outros
     * elementos também são salvos, mas sem junção e com a flag desligada. A
     * checagem é feita com os elementos adicionados por
     * {@linkplain #saveObjs(org.opencv.core.MatOfRect, ObjDetec.CascOrigin) saveObjs}.
     *
     * @param time Em qual tempo de frame será seram salvos.
     * @see #saveObjs(org.opencv.core.MatOfRect, ObjDetec.CascOrigin)
     */
    private void relateObjByCasc(int time)
    {

        dFramesTruePositive.get(time).clear();
        ArrayList<ObjDetec> all = new ArrayList<>();
        all.addAll(dNormal);
        all.addAll(dHSV);
        all.addAll(dInRange);

        for (int i = 0; i < all.size(); i++) {
            ObjDetec obj1 = all.get(i);
            all.remove(obj1); // Evitar que o objeto seja checado mais de uma vez, ou cheque ele msm
            ArrayList<ObjDetec> aux = new ArrayList<>(); // lista para slavar amostras que são parecidas
            for (int j = 0; j < all.size(); j++) {
                ObjDetec obj2 = all.get(j);

                if (obj1.isCenterInside(obj2, SCALECONTAINER) && obj1.getCascade() != obj2.getCascade()) //se está no centro e é de cascade diferente
                {
                    if (!aux.contains(obj2))
                        aux.add(obj2);
                    if (!aux.contains(obj1))
                        aux.add(obj1);
                }
            }
            if (!aux.isEmpty()) {
                ObjDetec ob = mergeObjsAvarage(aux);  //criar um objeto da media dos detectados
                if (aux.size() >= 2) // If it is found in 2 cascades set as "Exact"
                    ob.setExact(true);

                dFramesTruePositive.get(time).add(ob);
            }
        }
        //Escolhe entre os possiveis verdadeiros positivos, o mais proximos das amostras anteriores.
        if (!dFramesTruePositive.get(time).isEmpty()) {
            ObjDetec ob = findBestPosSample(dFramesTruePositive.get(time));
            dFramesTruePositive.get(time).clear();
            dFramesTruePositive.get(time).add(ob);// mantém a lista somente com 1 verdadeiro Positivo 
        }

    }

    /**
     * Mesmo que {@linkplain #findConeByCasc(int) findConeByCasc(int time)},
     * porém o tempo é assumido como o valor atual de
     * {@linkplain #frameTime frametime}.
     *
     * @see #findConeByCasc(int) (especificar tempo)
     */
    private void relateObjByCasc()
    {

        relateObjByCasc(frameTime);
    }

    /**
     * (FIND the BEST TRUE POSITIVE SAMPLE) Encontra dentro de uma lista, o
     * objeto que mais se aproxima em
     * {@linkplain ObjDetec#distanceTo(ObjDetec) distância} da média dos objetos
     * salvos em {@linkplain #dFramesTruePositive dFramesTruePositive}.
     *
     * @param list Lista dos objetos a checar.
     * @return Objeto que mais se aproxima da média dos salvos em
     * {@linkplain #dFramesTruePositive dFramesTruePositive}.
     */
    private ObjDetec findBestPosSample(ArrayList<ObjDetec> list)
    {
        if (list.isEmpty())
            return null;

        boolean t = false;

        for (ArrayList l : dFramesTruePositive)
            if (!l.isEmpty())
                t = true;

        if (t)
            return list.get(0);

        ObjDetec campeao = null;
        ObjDetec media = averageTruePositives();

        for (ObjDetec ob : list)
            if (!ob.isExact())
                if (campeao == null)
                    campeao = ob;
                else if (ob.distanceTo(media) < campeao.distanceTo(media))
                    campeao = ob;

        return campeao;
    }

    /**
     * (object from the AVAGE of the TRUE POSITIVES) Gera um objeto que é a
     * média de todas as positivas salvas em
     * {@linkplain #dFramesTruePositive dFramesTruePositive} usando a função
     * {@linkplain #mergeObjsAvarage(java.util.ArrayList) mergeObjsAvarage()}.
     *
     * @return O objeto gerado da média.
     */
    private ObjDetec averageTruePositives()
    {
        ArrayList<ObjDetec> aux = new ArrayList<>();

        for (ArrayList<ObjDetec> l : dFramesTruePositive)
            if (!l.isEmpty())
                if (l.get(0).isExact())
                    aux.add(l.get(0));

        //System.out.println("aux:" + aux.size());
        return mergeObjsAvarage(aux);
    }

    /**
     * (MERGE OBJECTS into an AVARAGE) Mescla varios objetos detectados em um
     * único usando a média dos seus valores.
     *
     * @param list Lista de objetos a mesclar.
     * @return Retorna o Objeto mesclado
     */
    private ObjDetec mergeObjsAvarage(ArrayList<ObjDetec> list)
    {

        if (list.isEmpty())//se está vazio n é possivel criar uma media

            return null;

        if (list.size() == 1)// se só existe um objeto a média é o próprio objeto

            return list.get(0);

        int h = 0, w = 0, x = 0, y = 0;
        for (ObjDetec aux : list) {
            h += aux.getHeight();
            w += aux.getWidth();
            x += aux.getCenter().x;
            y += aux.getCenter().y;
        }
        h /= list.size();
        w /= list.size();
        x /= list.size();
        y /= list.size();
        return new ObjDetec(new ObjDetec.Point(x, y), h, w, ObjDetec.CascOrigin.Indef);
        //O objeto é sempre de cascade indefinido
    }

    /**
     * Incrementa o {@link #frameTime tempo de frame} obedecendo o limite de
     * {@link #NUMFRAMES NUMFRAMES}.
     */
    private void frameAvance()
    {
        frameTime++;
        if (frameTime >= NUMFRAMES)
            frameTime = 0;
    }

    /**
     * (EVALUATE SAMPLES)Processa as amostras salvas dos cascades, encontrando a
     * melhor.
     *
     * @return
     */
    public ObjDetec evaluateSamples()
    {
        relateObjByCasc();
        frameAvance();//Avanca frame

        if (!dFramesTruePositive.get(frameTime).isEmpty())
            //System.out.println(": " + dFramesTruePositive.get(frameTime).size());
            return averageTruePositives(); //Retorna a média dos valores salvos
        return null;
    }
}
