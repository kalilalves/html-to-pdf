package mountain;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.StretchTypeEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlToPdf
{

    private static final String REGEX_MARGIN_LEFT = "<<marginLeft=(.*?)>>";
    private static final String REGEX_HEIGHT      = "<<height=(.*?)>>";

    private static final float BASE_FONTE_SIZE = 11;
    private static final int   WIDTH           = 465;
    private static final int   HEIGHT          = 20;
    private static final int   PAGE_WIDTH      = 595;
    private static final int   PAGE_HEIGHT     = 842;

    public static byte[] gerarPDF(String texto)
    {
        try
        {
            JasperDesign jasperDesign = gerarJasper(texto);

            List<Map<String, Object>> dataSourceList = Collections.singletonList(new HashMap<>());
            JRDataSource dataSource = new JRBeanCollectionDataSource(dataSourceList);

            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(), dataSource);

            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static JasperDesign gerarJasper(String texto)
    {
        JasperDesign jasperDesign = construirDesign();
        List<Campo> linhasStyled = converterHtmlParaJasperStyled(texto);
        for (Campo linha : linhasStyled)
        {
            adicionarTextField(jasperDesign, linha);
        }
        return jasperDesign;
    }

    public static List<Campo> converterHtmlParaJasperStyled(String conteudo)
    {
        conteudo = replaceHtmlParaJasperStyled(conteudo);
        String[] conteudoSplit = conteudo.split("<style");

        List<Campo> listaConteudoConvertido = new ArrayList<>();

        HorizontalTextAlignEnum alinhamentoInteracao = null;
        Campo mapInteracao = null;

        HorizontalTextAlignEnum alinhamento;
        String espacoLeft = "0";
        String height = "0";
        for (String cont : conteudoSplit)
        {
            if (cont == null || cont.trim().isEmpty())
                continue;

            cont = "<style" + cont;

            int inicioStyle = countOccurrences(cont, "<style");
            int finalStyle = countOccurrences(cont, "</style");
            int styleRestante = inicioStyle - finalStyle;

            while (styleRestante > 0)
            {
                cont += "</style>";
                styleRestante--;
            }

            while (styleRestante < 0)
            {
                cont = "<style>" + cont;
                styleRestante++;
            }

            alinhamento = extrairAlinhamento(cont);

            if (alinhamentoInteracao == null)
                alinhamentoInteracao = alinhamento;

            String espacoLeftInteracao = "0";
            Matcher matcher = Pattern.compile(REGEX_MARGIN_LEFT).matcher(cont);
            if (matcher.find())
            {
                espacoLeftInteracao = matcher.group(1);
                cont = cont.replace(matcher.group(), "");
            }

            String heightInteracao = "0";
            matcher = Pattern.compile(REGEX_HEIGHT).matcher(cont);
            if (matcher.find())
            {
                heightInteracao = matcher.group(1);
                cont = cont.replace(matcher.group(), "");
            }

            if (alinhamento != null && !alinhamentoInteracao.equals(alinhamento) ||
                espacoLeftInteracao != null && !espacoLeftInteracao.equals(espacoLeft) ||
                heightInteracao != null && !heightInteracao.equals(height))
            {
                listaConteudoConvertido.add(mapInteracao);
                mapInteracao = null;
                alinhamentoInteracao = alinhamento;
            }

            if (mapInteracao == null)
            {
                mapInteracao = new Campo();
                mapInteracao.setAlignEnum(alinhamento);
                mapInteracao.setEspacamentoLeft(Integer.parseInt(espacoLeftInteracao));
                mapInteracao.setAumentoHeight(Integer.parseInt(heightInteracao));
            }

            if (mapInteracao.getTexto() != null && !mapInteracao.getTexto().isEmpty())
                mapInteracao.setTexto(mapInteracao.getTexto() + "\n" + cont.trim());
            else
                mapInteracao.setTexto(cont.trim());
        }
        listaConteudoConvertido.add(mapInteracao);

        return listaConteudoConvertido;
    }

    private static String replaceHtmlParaJasperStyled(String conteudo)
    {

        conteudo = conteudo.replaceAll("</strong>|</em>|</u>|</s>|</pre>|</div>|</span>|</address>|</h[0-9]>", "</style>");
        conteudo = conteudo.replaceAll("<p>|<div>", "<style>");
        conteudo = conteudo.replaceAll("</p>", "</style><br/>");

        conteudo = conteudo.replace("<strong>", "<style isBold=\"true\">");
        conteudo = conteudo.replace("<u>", "<style isUnderline=\"true\">");
        conteudo = conteudo.replace("<s>", "<style isStrikeThrough=\"true\">");
        conteudo = conteudo.replace("<em>", "<style isItalic=\"true\">");
        conteudo = conteudo.replace("<address", "<style isItalic=\"true\"");
        conteudo = conteudo.replace("<pre", "<style");
        conteudo = conteudo.replace("<div", "<style");
        conteudo = conteudo.replace("<p", "<style");

        conteudo = conteudo.replace("<span style=\"color:", "<style forecolor=\"");
        conteudo = conteudo.replace("<span style=\"background-color:", "<style backcolor=\"");
        conteudo = conteudo.replace("<span style=\"display:none\"", "<style");

        conteudo = conteudo.replaceAll("style=\"margin-(.*):0cm\"", "");

        conteudo = processarIdent(conteudo);
        conteudo = processarLista(conteudo);
        conteudo = processarAnchor(conteudo);
        conteudo = processarAlign(conteudo);

        conteudo = StringEscapeUtils.unescapeHtml(conteudo);

        int tamFonteBase = 10;
        double[] tamTitulos = { 2.0, 1.5, 1.17, 1.0, 0.83, 0.67 };
        for (int i = 1; i <= 6; i++)
            conteudo = conteudo.replace("<h" + i, "<style size=\"" + (int) (tamFonteBase * tamTitulos[i - 1]) + "\"");

        return conteudo;
    }

    private static String processarIdent(String conteudo)
    {
        Pattern regexPattern = Pattern.compile("text-indent\\s*:\\s*([^;]+)");
        Matcher matcher = regexPattern.matcher(conteudo);
        while (matcher.find())
        {
            String txtLimpo = matcher.group(1);

            conteudo = conteudo.replace(matcher.group(), REGEX_HEIGHT.replace("(.*?)", converterParaPx(txtLimpo).toString()));
        }
        return conteudo;
    }

    private static String processarLista(String conteudo)
    {
        Pattern pattern = Pattern.compile("<ol>(.*?)</ol>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(conteudo);
        while (matcher.find())
        {
            String lista = matcher.group(1);
            Pattern pattern2 = Pattern.compile("<li>(.*?)</li>");
            Matcher matcher2 = pattern2.matcher(lista);
            int n = 1;
            while (matcher2.find())
            {
                String txtLimpo = matcher2.group(1);
                txtLimpo = txtLimpo.replaceAll("<li>|</li>", "");
                lista = lista.replace(matcher2.group(), "<style>" +
                                                        REGEX_MARGIN_LEFT.replace("(.*?)", "5") +
                                                        REGEX_HEIGHT.replace("(.*?)", "10") +
                                                        n +
                                                        ". " +
                                                        txtLimpo +
                                                        "</style>");
                n++;
            }
            conteudo = conteudo.replace(matcher.group(), lista);
        }

        return conteudo;
    }

    private static String processarAnchor(String conteudo)
    {
        Pattern regexPattern = Pattern.compile("<a(.*?)>(.*?)</a>");
        Matcher matcher = regexPattern.matcher(conteudo);
        while (matcher.find())
        {
            String txtLimpo = matcher.group(2);
            txtLimpo = txtLimpo.replaceAll("</[^>]*>", "");
            txtLimpo = txtLimpo.replaceAll("<[^>]*>", "");
            conteudo = conteudo.replace(matcher.group(), "<a" + matcher.group(1) + ">" + txtLimpo + "</a>");
        }
        return conteudo;
    }

    private static String processarAlign(String conteudo)
    {
        Pattern regexPattern = Pattern.compile("style\\s*=\\s*(['\"])(.*?)\\1");
        Matcher matcher = regexPattern.matcher(conteudo);
        while (matcher.find())
        {
            String txtLimpo = matcher.group(2);
            if (txtLimpo.contains("center"))
                txtLimpo = "textAlignment=\"center\"";
            else if (txtLimpo.contains("right"))
                txtLimpo = "textAlignment=\"right\"";
            else if (txtLimpo.contains("justify"))
                txtLimpo = "textAlignment=\"justify\"";
            else if (txtLimpo.contains("left"))
                txtLimpo = "textAlignment=\"left\"";
            else
                txtLimpo = "";

            conteudo = conteudo.replace(matcher.group(), txtLimpo);
        }
        return conteudo;
    }

    private static HorizontalTextAlignEnum extrairAlinhamento(String conteudo)
    {
        if (conteudo.contains("textAlignment=\"center\""))
            return HorizontalTextAlignEnum.CENTER;
        else if (conteudo.contains("textAlignment=\"right\""))
            return HorizontalTextAlignEnum.RIGHT;
        else if (conteudo.contains("textAlignment=\"justify\""))
            return HorizontalTextAlignEnum.JUSTIFIED;
        else
            return HorizontalTextAlignEnum.LEFT;
    }

    public static int countOccurrences(String haystack, String needle)
    {
        int count = 0;
        int lastIndex = 0;

        while (lastIndex != -1)
        {
            lastIndex = haystack.indexOf(needle, lastIndex);
            if (lastIndex != -1)
            {
                count++;
                lastIndex += needle.length();
            }
        }

        return count;
    }

    private static JasperDesign construirDesign()
    {
        JasperDesign jasperDesign = new JasperDesign();
        jasperDesign.setName("JasperDesign");
        jasperDesign.setPageWidth(PAGE_WIDTH);
        jasperDesign.setPageHeight(PAGE_HEIGHT);
        jasperDesign.setColumnWidth(WIDTH);
        jasperDesign.setColumnSpacing(5);
        jasperDesign.setLeftMargin(70);
        jasperDesign.setRightMargin(50);
        jasperDesign.setTopMargin(15);
        jasperDesign.setBottomMargin(0);
        jasperDesign.setLanguage("groovy");
        return jasperDesign;
    }

    private static void adicionarTextField(JasperDesign jasperDesign, Campo campo)
    {
        String texto = campo.getTexto();
        texto = texto.replace("$", "\\$");
        texto = texto.replace("\"", "\\\"");
        texto = "\"" + texto + "\"";

        JRDesignTextField textField = new JRDesignTextField();
        textField.setX(campo.getEspacamentoLeft());
        textField.setY(0);
        textField.setWidth(WIDTH);
        textField.setHeight(HEIGHT);

        textField.getParagraph().setLeftIndent(campo.getEspacamentoLeft());
        textField.getParagraph().setSpacingBefore(campo.getAumentoHeight());

        textField.setExpression(new JRDesignExpression(texto));
        textField.setHorizontalTextAlign(campo.getAlignEnum());
        textField.setStretchType(StretchTypeEnum.NO_STRETCH);
        textField.setStretchWithOverflow(true);
        textField.setPositionType(PositionTypeEnum.FLOAT);
        textField.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        textField.setFontSize(BASE_FONTE_SIZE);
        textField.setMarkup("styled");

        JRDesignBand detailBand = new JRDesignBand();
        detailBand.addElement(textField);
        detailBand.setHeight(HEIGHT);
        ((JRDesignSection) jasperDesign.getDetailSection()).addBand(detailBand);
    }

    /**
     * Converte uma quantidade CSS (px, em, rem, pt, etc) para pixels (px) inteiros
     *
     * @param valor
     *            String representando o valor com unidade (ex: "1em", "10px",
     *            "5pt")
     * @return Valor convertido para pixels como inteiro
     */
    private static Integer converterParaPx(String valor)
    {
        if (valor == null || valor.trim().isEmpty())
        {
            return 0;
        }

        valor = valor.trim().toLowerCase();
        double resultado = 0;

        // Extrair o n�mero da unidade
        Pattern pattern = Pattern.compile("([\\d.]+)([a-z%]*)");
        Matcher matcher = pattern.matcher(valor);

        if (matcher.matches())
        {
            double numero = Double.parseDouble(matcher.group(1));
            String unidade = matcher.group(2);

            switch (unidade)
            {
            case "px":
                resultado = numero;
                break;
            case "pt":
                resultado = numero * 1.33; // Aproximadamente 1pt = 1.33px
                break;
            case "em":
            case "rem":
                resultado = numero * BASE_FONTE_SIZE;
                break;
            case "cm":
                resultado = numero * 37.8; // 1cm = 37.8px (aproximadamente)
                break;
            case "mm":
                resultado = numero * 3.78; // 1mm = 3.78px (aproximadamente)
                break;
            case "in":
                resultado = numero * 96; // 1in = 96px (padr�o CSS)
                break;
            case "%":
                resultado = numero * BASE_FONTE_SIZE / 100;
                break;
            default:
                resultado = numero; // Sem unidade, assume que � pixel
            }
        }

        return (int) Math.round(resultado);
    }

}
