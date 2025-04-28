package mountain;

import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;

public class Campo
{

    private String                  texto;
    private HorizontalTextAlignEnum alignEnum;
    private int                     espacamentoLeft;
    private int                     aumentoHeight;

    public String getTexto()
    {
        return texto;
    }

    public void setTexto(String texto)
    {
        this.texto = texto;
    }

    public HorizontalTextAlignEnum getAlignEnum()
    {
        return alignEnum;
    }

    public void setAlignEnum(HorizontalTextAlignEnum alignEnum)
    {
        this.alignEnum = alignEnum;
    }

    public int getEspacamentoLeft()
    {
        return espacamentoLeft;
    }

    public void setEspacamentoLeft(int espacamentoLeft)
    {
        this.espacamentoLeft = espacamentoLeft;
    }

    public int getAumentoHeight()
    {
        return aumentoHeight;
    }

    public void setAumentoHeight(int aumentoHeight)
    {
        this.aumentoHeight = aumentoHeight;
    }

}
