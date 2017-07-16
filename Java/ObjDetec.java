
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import org.opencv.core.Point;
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
public class ObjDetec {
    /** Centro da figura detectada. */
    private Point center;
    
    /** Altura da figura detectada. */
    private int height;
    /** Largura da figura detectada. */
    private int width;
    /** Tipo do Cascade usado na detecção.
     Tipos: CASC_NORMAL, CASC_INRANGE, CASC_HSV */
    private CascOrigin cascade;
    /** Se o objeto é considerado como correto, ou melhor, um Positivo Verdadeiro.*/
    private boolean exact;
    
    
    public enum CascOrigin
    {
        Indef,Normal,InRange,HSV
    }
    /** Objeto detectado por um cascade sem nenhuma transfomação.
     * {@value}*/
    static final int CASC_NORMAL = 1;
    /**  Objeto detectado por um cascade depois da transformação InRange,
     * que separa partes laranja avermelhadas da imagem.
     * {@value} */
    static final int CASC_INRANGE = 2;
    /** Objeto detectado por um cascade depois da transformação para HSV.
     * {@value}*/
    static final int CASC_HSV = 3;
    /** Objeto detectado por um cascade indefinido (padrão).
     * {@value}*/
    static final int CASC_INDEF = -1;

    public ObjDetec() {
    }

    public ObjDetec(Point center, int height, int width, CascOrigin cascade, boolean exact) {
        this.center = center;
        this.height = height;
        this.width = width;
        this.cascade = cascade;
        this.exact = exact;
    }
    
    /**
     * Construtor usando o retangulo detectado e o Cascade usado.
     * @param r Retângulo do quadro detectado pelo OpenCV.
     * @param cascadeType Qual o tipo do Cascade usado para detectar o Retangulo.
     * @see CascOrigin
     */

    public ObjDetec(Rect r, CascOrigin cascadeType) {
        this.height = r.height;
        this.width = r.width;
        this.center = new Point(r.x, r.y);
        cascade = CascOrigin.Indef;
    }
    
    /**
     * Método para verificar se um Objeto detectado está com seu centro contido dentro de outro.
     * @param ob Objeto que possivelmente CONTÉM o centro do que usa o método.(Container)
     * @param scale Escala para ampliar ou reduzir o objeto passado.(Scale of Container)
     * @return Retorna verdadeiro se o centro do objeto que usa o método, está dentro do objeto passado depois de escalado.
     */
    public boolean isCenterInside (ObjDetec ob ,int scale)
    {
        int h = ob.height*scale/2;
        int w = ob.width*scale/2;
        return (abs(this.center.x - ob.center.x) < w) && (abs(this.center.y - ob.center.y) < h);
    }
    
    /**
     * Método para verificar se um Objeto detectado está com seu centro contido dentro de outro.
     * @param ob Objeto que possivelmente CONTÉM o centro do que usa o método.(Container)
     * @return Retorna verdadeiro se o centro do objeto que usa o método, está dentro do objeto passado.
     */
    public boolean isCenterInside (ObjDetec ob)
    {
        return this.isCenterInside(ob,1);
    }
    
    /**
     * Método para verificar se um Objeto detectado está completamente contido dentro de outro.
     * @param ob Objeto que possivelmente CONTÉM o que usa o método.(Container)
     * @param scale Escala para ampliar ou reduzir o objeto passado.(Scale of Container)
     * @return Retorna VERDADEIRO se o objeto que usa o método, está completamente dentro do objeto passado depois de escalado,
     * e FALSO se o Objeto que usa: está totalmente fora; a tem alguma dimensão maior que o passado; está parciamente fora.
     */
    public boolean isInside (ObjDetec ob ,int scale)
    {
        int h = (ob.height*scale - this.height);
        int w = (ob.width*scale - this.height);
        return (abs(this.center.x - ob.center.x) < w) && (abs(this.center.y - ob.center.y) < h);
    }
    
    /**
     * Método para verificar se um Objeto detectado está completamente contido dentro de outro.
     * @param ob Objeto que possivelmente CONTÉM o que usa o método.(Container)
     * @return Retorna VERDADEIRO se o objeto que usa o método, está completamente dentro do objeto passado depois de escalado,
     * e FALSO se o Objeto que usa: está totalmente fora; a tem alguma dimensão maior que o passado; está parciamente fora.
     */
    public boolean isInside (ObjDetec ob)
    {
        return isInside(ob,1);
    }
    
    /**
     * Método para calcular a distancia entre dois objetos detectados.
     * @param ob Objeto comparado para distância.
     * @return Retorna a distância euclidiana entre os dois objetos (o Objeto que usao o método e o argumento).
     */
    public double distanceTo (ObjDetec ob)
    {
        return (sqrt( sqr(this.center.x) + sqr(ob.center.y)));
    }
    
    /**
     * Método rapido para elevar um numero double ao quadrado.
     * @param a Valor a ser operado.
     * @return Retorn a².
     */
    static public double sqr(double a)
    {
            return a*a;
    }
}
