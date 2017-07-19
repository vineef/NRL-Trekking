
import java.util.ArrayList;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Pierre
 */
public class EstruturaAmostras {

    /**
     * Lista para objetos encontrados com Cascade que não usa transformação.
     */
    private ArrayList<ObjDetec> dNormal;

    /**
     * Lista para objetos encontrados com Cascade que usa "InRange" como
     * transformação.
     */
    private ArrayList<ObjDetec> dInRange;

    /**
     * Lista para objetos encontrados com Cascade que usa "HSV" como
     * transformação.
     */
    private ArrayList<ObjDetec> dHSV;

    /**
     * Lista bidimensional para salvar o objetos detectatos ao longo de varios
     * frames. A lista externa tem como index, um tempo de frame
     * ({@link #frameTime frametime}), e a interna simplesmente contém os
     * objetos que foram achados nesse tempo de frame. Objetos da lista interna
     * com a flag {@link ObjDetec#exact Exact} ligada, são considerados como
     * amostras positivas verdadeiras, e são na verdade, uma mescla de amostras
     * de diferentes cascades. Todos os elementos da lista interna tem
     * {@link ObjDetec#cascade Cascade de origem} {@link ObjDetec.CascOrigin#Indef Indefinido}.
     */
    private ArrayList<ArrayList<ObjDetec>> dFramesAll;
    
    /**
     * Valor interio que determina o index do frame atual, e que tem como limite maximo {@link #LIMFRAME FRAME LIMIT}.
     */
    private int frameTime;

    private static final double LIMSCALECASC = 0.5;
    private static final int LIMFRAME = 5;
    private static final int LIMDSTFRAME = 10;
    private static final int NUMLIMCASC = 2;

    public EstruturaAmostras() {
        this.frameTime = 0;
        dFramesAll = new ArrayList<>();
        dNormal = new ArrayList<>();
        dHSV =  new ArrayList<>();
        dInRange = new ArrayList<>();
        int i;
        for(i=0;i<LIMFRAME;i++)
            dFramesAll.add(new ArrayList<>());
    }

    /**
     * Método para adicionar os objetos encontrados em suas listas especificas.
     *
     * @param det Lista a de Retangulos da detecção.
     * @param orig Tipo do cascade usado na detecção.
     */
    public void saveObjs(MatOfRect det, ObjDetec.CascOrigin orig) {
        
        for (Rect rect : det.toArray()) {
            ObjDetec obj = new ObjDetec(rect, orig);
            switch (orig) {
                case Normal:
                    dNormal.add(obj);
                    break;
                case HSV:
                    dHSV.add(obj);
                    break;
                case InRange:
                    dInRange.add(obj);
                    break;
                case Indef:
                    System.out.print("Tipo indefinido não pode ser salvo");
            }
        }
    }

    /**
     * Inicializa ou limpa uma lista de objetos.
     *
     * @param list Lista para iniciar.
     */
    private ArrayList startArray(ArrayList list)
    {
        if (list == null)
        {
            list = new ArrayList<>();
        } else {
            list.clear();
        }
        return list;
    }


    /**
     * Encontra elementos que estejam em mais de uma lista de cascades usando
     * como semelhança o teste {@link ObjDetec#isCenterInside(ObjDetec, double)
     * isCenterInside}. Depois de encontrados, os elementos semelhates são
     * transformados em um só pela média dos seus valores, usando
     * {@link #mergeMean(java.util.ArrayList) mergeMean}, e salvo com a flag
     * {@link ObjDetec#exact Exact} ligada. Os outros elementos também são
     * salvos, mas sem junção e com a flag desligada. A checagem é feita com os
     * elementos adicionados por
     * {@link #saveObjs(org.opencv.core.MatOfRect, ObjDetec.CascOrigin) saveObjs}.
     * @param time Em qual tempo de frame será seram salvos.
     * @see #saveObjs(org.opencv.core.MatOfRect, ObjDetec.CascOrigin)
     *
     */
    private void relateObjByCasc(int time) {
        
        dFramesAll.get(time).clear();
        for (ObjDetec obj1 : dNormal) {
            ArrayList<ObjDetec> aux = new ArrayList<>();
            for (ObjDetec obj2 : dHSV) {
                if (obj1.isCenterInside(obj2, LIMSCALECASC)) {
                    if (!aux.contains(obj2)) {
                        aux.add(obj2);
                    }
                    if (!aux.contains(obj1)) {
                        aux.add(obj1);
                    }
                }
            }
            for (ObjDetec obj2 : dInRange) {
                if (obj1.isCenterInside(obj2, LIMSCALECASC)) {
                    if (!aux.contains(obj2)) {
                        aux.add(obj2);
                    }
                    if (!aux.contains(obj1)) {
                        aux.add(obj1);
                    }
                }
            }
            if (!aux.isEmpty()) {
                ObjDetec ob = mergeMean(aux);  //criar um objeto da media dos detectados

                if (aux.size() >= 2) // If it is found in 2 cascades set as "Exact"
                {
                    ob.setExact(true);
                }

                dFramesAll.get(time).add(ob);
            }
            
            if (!dFramesAll.get(time).contains(obj1))//Adiciona de qualquer maneira o objeto sem a flag Exact
            {  
                dFramesAll.get(time).add(obj1);
            }
        }
        
        for (ObjDetec obj1 : dHSV) {

            ArrayList<ObjDetec> aux = new ArrayList<>();
            
            for (ObjDetec obj2 : dInRange) { // só busca no InRange pois a busca no normal ja foi feita
                if (obj1.isCenterInside(obj2, LIMSCALECASC)) {
                    if (!aux.contains(obj2)) {
                        aux.add(obj2);
                    }
                    if (!aux.contains(obj1)) {
                        aux.add(obj1);
                    }
                }
            }
            if (!aux.isEmpty()) {
                ObjDetec ob = mergeMean(aux);  //criar um objeto da media dos detectados

                if (aux.size() >= NUMLIMCASC) // If it is found in 2 cascades set as "Exact"
                {
                    ob.setExact(true);
                }

                dFramesAll.get(time).add(ob);
            }
            
            if (!dFramesAll.get(time).contains(obj1))//Adiciona de qualquer maneira o objeto sem a flag Exact
            {  
                dFramesAll.get(time).add(obj1);
            }
        }
        
        for (ObjDetec obj1 : dInRange)// não realiza mais busacas pois ja foi comparado do Normal e No HSV
        { 
            
            if (!dFramesAll.get(time).contains(obj1))//Adiciona de qualquer maneira o objeto sem a flag Exact
            {  
                dFramesAll.get(time).add(obj1);
            }
        }
        dNormal.clear();
        dHSV.clear();
        dInRange.clear();//limpa as listas caso ainda reste algo.
        
    }

    /**
     * Mesmo que {@link #findConeByCasc(int) findConeByCasc(int time)}, poém o
     * tempo é assumido como o valor atual de {@link #frameTime frametime}.
     *
     * @see #findConeByCasc(int) (especificar tempo)
     */
    private void relateObjByCasc() {

        relateObjByCasc(frameTime);
    }

    /**
     * Mescla varios objetos detectados em um único usando a média dos seus
     * valores.
     *
     * @param list Lista de objetos a mesclar.
     * @return Retorna o Objeto mesclado
     */
    private ObjDetec mergeMean(ArrayList<ObjDetec> list) {
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
    }
    
    
    private void frameAvance()
    {
        frameTime++;
        if(frameTime >= LIMFRAME);
            frameTime=0;
    }
    
    
    public ObjDetec evaluateSamples()
    {
        relateObjByCasc();
        frameAvance();//Avanca frame
        if(dFramesAll.get(frameTime).size()!=0)System.out.println(": " + dFramesAll.get(frameTime).size());
        for(ObjDetec aux : dFramesAll.get(frameTime))
        {
            if(aux.isExact())
                return aux;
        }
        return null;
        
    }

}
