
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
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
     @see CascOrigin Cascade de Origem (enumeração) */
    private CascOrigin cascade;
    /** Se o objeto é considerado como correto, ou melhor, um Positivo Verdadeiro.*/
    private boolean exact;
    
    /**
     * Tipos de Cascade de origem do objeto detectado
     */
    public enum CascOrigin
    {
        /**Cascade Indefinido.*/
        Indef(-1),
        /** Cascade sem transformação.*/
        Normal(1),
        /** Cascade que usa a transaformação "InRange" na detecção.*/
        InRange(2),
        /** Cascade que usa a transaformação "HSV" na detecção.*/
        HSV(3);
        
        public final int id;
        
        private CascOrigin(int id){this.id=id;}
        
    }

    public ObjDetec() {
    }

    public ObjDetec(Point center, int height, int width, CascOrigin cascade) {
        this.center = center;
        this.height = height;
        this.width = width;
        this.cascade = cascade;
        this.exact = false;
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
        this.center = new Point((r.x + width/2), (r.y + height/2));
        cascade = CascOrigin.Indef;
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    } 

    public CascOrigin getCascade() {
        return cascade;
    }

    public void setCascade(CascOrigin cascade) {
        this.cascade = cascade;
    }

    public boolean isExact() {
        return exact;
    }

    public void setExact(boolean exact) {
        this.exact = exact;
    }
   
    /**
     * Método para verificar se um Objeto detectado está com seu centro contido dentro de outro.
     * @param ob Objeto que possivelmente CONTÉM o centro do que usa o método.(Container)
     * @param scale Escala para ampliar ou reduzir o objeto passado.(Scale of Container)
     * @return Retorna verdadeiro se o centro do objeto que usa o método, está dentro do objeto passado depois de escalado.
     */
    public boolean isCenterInside (ObjDetec ob ,double scale)
    {
        double h = ob.height*scale/2;
        double w = ob.width*scale/2;
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
        int w = (ob.width*scale - this.width);
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
     * Método para calcular o perimetro do objeto detectado.
     * @return Retorn 2*(altura+largura).
     */
    public double perimeter()
    {
        return 2*(this.height + this.width);//Pondera a diferenca das dimensões com a soma das dimensões
    }
    
    /**
     * Método que retorna a area do objeto detectado.
    * @return Retorna altura*largura.
     */
    public double area()
    {
      return width*height;
    }
    
    /**
     * Método de comparação entre objetos detectados pela relação de suas dimensões.
     * @param ob Objeto a ser comparado.
     * @return Retorn 1 - (diferença/soma). Quanto mais proximo de 1 mais parecidas são as dimensões.
     */
    public double compareDimen(ObjDetec ob)
    {
        double dif = abs(ob.width - this.width) + abs(ob.height - this.height);
        
        //Pondera a diferenca das dimensões com a soma dos perimetros
        return 1 - 2*dif/(ob.perimeter() + this.perimeter());
    }
    
    /**
     * Método de comparação entre objetos detectados pela relação de suas areas.
     * @param ob Objeto a ser comparado.
     * @return Retorna 1 - (diferença/soma). Quanto mais proximo de 1 mais parecidas são as areas.
     */
    public double compareArea(ObjDetec ob)
    {
        return 1 - ( abs(ob.area() - this.area())/(ob.area() + this.area()) );
    }
    
    
    public Rect toRect()
    {
        return new Rect((int)center.x-width/2,(int)center.y-height/2,width,height);
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
    
    static public class Point
    {
        int x;
        int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
    }
}
