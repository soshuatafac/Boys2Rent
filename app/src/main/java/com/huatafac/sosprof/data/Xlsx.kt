package com.huatafac.sosprof.data

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Leitura e escrita de folhas Excel (.xlsx) simples, só com texto, sem bibliotecas externas.
 * Um .xlsx é um ZIP com ficheiros XML; aqui escreve-se uma folha e lê-se a primeira folha do livro.
 */
object Xlsx {

    const val MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    /** Limites de leitura, para um ficheiro estranho não esgotar a memória do telemóvel. */
    private const val MAX_BYTES_ENTRADA = 20L * 1024 * 1024
    private const val MAX_LINHAS = 5000
    private const val MAX_COLUNAS = 100

    private const val NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

    // ---------------------------------------------------------------- escrita

    /**
     * Escreve um livro com uma folha. A primeira linha fica a negrito e fixa no topo.
     * Todas as células (também as vazias das colunas) ficam em formato de texto, para o Excel
     * não transformar datas, números de telemóvel ou NIF (zeros à esquerda, notação científica).
     * [larguras] em caracteres, por coluna.
     */
    fun escrever(out: OutputStream, nomeFolha: String, linhas: List<List<String>>, larguras: List<Int> = emptyList()) {
        val colunas = maxOf(linhas.maxOfOrNull { it.size } ?: 0, larguras.size, 1)
        val folha = nomeFolha.replace(Regex("""[\[\]:*?/\\]"""), " ").trim().ifBlank { "Folha1" }.take(31)
        ZipOutputStream(out).use { zip ->
            fun entrada(nome: String, conteudo: String) {
                zip.putNextEntry(ZipEntry(nome))
                zip.write(conteudo.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entrada("[Content_Types].xml", CONTENT_TYPES)
            entrada("_rels/.rels", RELS)
            entrada(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    """<workbook xmlns="$NS_MAIN" xmlns:r="$NS_REL"><sheets>""" +
                    """<sheet name="${escapar(folha)}" sheetId="1" r:id="rId1"/></sheets></workbook>"""
            )
            entrada("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            entrada("xl/styles.xml", STYLES)
            entrada("xl/worksheets/sheet1.xml", folhaXml(linhas, colunas, larguras))
        }
    }

    private fun folhaXml(linhas: List<List<String>>, colunas: Int, larguras: List<Int>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="$NS_MAIN">""")
        append("""<sheetViews><sheetView workbookViewId="0">""")
        append("""<pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>""")
        append("""</sheetView></sheetViews>""")
        append("<cols>")
        for (c in 0 until colunas) {
            val largura = larguras.getOrNull(c) ?: 18
            append("""<col min="${c + 1}" max="${c + 1}" width="$largura" style="1" customWidth="1"/>""")
        }
        append("</cols><sheetData>")
        linhas.forEachIndexed { i, linha ->
            val r = i + 1
            append("""<row r="$r">""")
            linha.forEachIndexed { c, valor ->
                // Estilo 2 = cabeçalho (negrito, texto); 1 = texto
                val estilo = if (i == 0) 2 else 1
                append("""<c r="${letrasColuna(c)}$r" t="inlineStr" s="$estilo"><is><t xml:space="preserve">""")
                append(escapar(valor))
                append("</t></is></c>")
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    /** 0 → A, 25 → Z, 26 → AA. */
    internal fun letrasColuna(indice: Int): String {
        var n = indice + 1
        val sb = StringBuilder()
        while (n > 0) {
            val resto = (n - 1) % 26
            sb.append('A' + resto)
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    /** "A" → 0, "AA" → 26 (só as letras do início da referência, ex.: "AB12"). */
    internal fun indiceColuna(referencia: String): Int? {
        val letras = referencia.takeWhile { it.isLetter() }.uppercase()
        if (letras.isEmpty()) return null
        return letras.fold(0) { acc, ch -> acc * 26 + (ch - 'A' + 1) } - 1
    }

    private fun escapar(texto: String): String = buildString {
        for (ch in texto) {
            when {
                ch == '&' -> append("&amp;")
                ch == '<' -> append("&lt;")
                ch == '>' -> append("&gt;")
                ch == '"' -> append("&quot;")
                // Caracteres de controlo não são válidos em XML 1.0 (exceto tab e mudança de linha)
                ch < ' ' && ch != '\t' && ch != '\n' && ch != '\r' -> Unit
                else -> append(ch)
            }
        }
    }

    // ---------------------------------------------------------------- leitura

    /**
     * Lê a primeira folha do livro. Devolve as linhas pela ordem da folha (índice 0 = linha 1),
     * com linhas vazias onde a folha salta linhas; cada linha tem os valores como texto.
     * Números inteiros vêm sem casas decimais ("3" e não "3.0").
     * Lança exceção se o ficheiro não for um .xlsx válido.
     */
    fun ler(input: InputStream): List<List<String>> {
        val ficheiros = lerZip(input)
        val partilhadas = ficheiros["xl/sharedStrings.xml"]?.let { textosPartilhados(xml(it)) } ?: emptyList()
        val caminho = caminhoPrimeiraFolha(ficheiros)
            ?: throw IllegalArgumentException("O ficheiro não tem nenhuma folha")
        val folha = xml(ficheiros.getValue(caminho))

        val linhas = sortedMapOf<Int, List<String>>()
        var proximaLinha = 1
        for (row in filhos(folha.documentElement, "sheetData").flatMap { filhos(it, "row") }) {
            val numero = row.getAttribute("r").toIntOrNull() ?: proximaLinha
            proximaLinha = numero + 1
            if (numero > MAX_LINHAS) break
            val celulas = sortedMapOf<Int, String>()
            var proximaColuna = 0
            for (c in filhos(row, "c")) {
                val coluna = indiceColuna(c.getAttribute("r")) ?: proximaColuna
                proximaColuna = coluna + 1
                if (coluna >= MAX_COLUNAS) continue
                celulas[coluna] = valorCelula(c, partilhadas)
            }
            val ultima = celulas.keys.maxOrNull() ?: -1
            linhas[numero] = (0..ultima).map { celulas[it].orEmpty() }
        }
        val total = linhas.keys.maxOrNull() ?: 0
        return (1..total).map { linhas[it] ?: emptyList() }
    }

    private fun lerZip(input: InputStream): Map<String, ByteArray> {
        val ficheiros = mutableMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(input).use { zip ->
            while (true) {
                val entrada = zip.nextEntry ?: break
                if (entrada.isDirectory) continue
                val nome = entrada.name.removePrefix("/")
                // Só interessam os XML do livro; o resto (imagens, etc.) é ignorado
                if (!nome.endsWith(".xml") && !nome.endsWith(".rels")) continue
                val buffer = ByteArrayOutputStream()
                val bloco = ByteArray(8192)
                while (true) {
                    val n = zip.read(bloco)
                    if (n < 0) break
                    total += n
                    if (total > MAX_BYTES_ENTRADA) throw IllegalArgumentException("O ficheiro é demasiado grande")
                    buffer.write(bloco, 0, n)
                }
                ficheiros[nome] = buffer.toByteArray()
            }
        }
        if ("xl/workbook.xml" !in ficheiros && ficheiros.keys.none { it.startsWith("xl/worksheets/") }) {
            throw IllegalArgumentException("Não é um ficheiro Excel (.xlsx)")
        }
        return ficheiros
    }

    /** Caminho da primeira folha, pelo workbook.xml e as suas relações; senão, a primeira em xl/worksheets/. */
    private fun caminhoPrimeiraFolha(ficheiros: Map<String, ByteArray>): String? {
        try {
            val workbook = xml(ficheiros.getValue("xl/workbook.xml"))
            val sheet = filhos(workbook.documentElement, "sheets").flatMap { filhos(it, "sheet") }.firstOrNull()
            val id = sheet?.getAttributeNS(NS_REL, "id")?.ifBlank { null } ?: sheet?.getAttribute("r:id")
            val rels = xml(ficheiros.getValue("xl/_rels/workbook.xml.rels"))
            val alvo = filhos(rels.documentElement, "Relationship")
                .firstOrNull { it.getAttribute("Id") == id }?.getAttribute("Target")
            if (!alvo.isNullOrBlank()) {
                val caminho = if (alvo.startsWith("/")) alvo.removePrefix("/") else "xl/$alvo"
                if (caminho in ficheiros) return caminho
            }
        } catch (_: Exception) {
        }
        return ficheiros.keys.filter { it.startsWith("xl/worksheets/") && it.endsWith(".xml") }.minOrNull()
    }

    private fun textosPartilhados(doc: Document): List<String> =
        filhos(doc.documentElement, "si").map { textoDe(it) }

    /** Junta o texto de todos os <t> dentro do elemento (texto simples ou com formatação em partes). */
    private fun textoDe(elemento: Element): String {
        val partes = elemento.getElementsByTagNameNS("*", "t")
        return (0 until partes.length).joinToString("") { partes.item(it).textContent.orEmpty() }
    }

    private fun valorCelula(c: Element, partilhadas: List<String>): String {
        val v = filhos(c, "v").firstOrNull()?.textContent.orEmpty()
        return when (c.getAttribute("t")) {
            "s" -> v.trim().toIntOrNull()?.let { partilhadas.getOrNull(it) }.orEmpty()
            "inlineStr" -> filhos(c, "is").firstOrNull()?.let { textoDe(it) }.orEmpty()
            "str" -> v
            "b" -> if (v.trim() == "1") "VERDADEIRO" else "FALSO"
            "e" -> ""
            else -> numeroComoTexto(v)
        }
    }

    /** "3.0" → "3", "1.5E3" → "1500", "0.25" → "0.25". */
    private fun numeroComoTexto(v: String): String {
        val limpo = v.trim()
        if (limpo.isEmpty()) return ""
        return try {
            BigDecimal(limpo).stripTrailingZeros().toPlainString()
        } catch (_: NumberFormatException) {
            limpo
        }
    }

    private fun filhos(pai: Element, nomeLocal: String): List<Element> {
        val lista = mutableListOf<Element>()
        var no = pai.firstChild
        while (no != null) {
            if (no is Element && (no.localName ?: no.nodeName.substringAfter(':')) == nomeLocal) lista += no
            no = no.nextSibling
        }
        return lista
    }

    private fun xml(bytes: ByteArray): Document {
        val fabrica = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            // Ficheiros vindos de fora: sem DTD nem entidades externas
            for (f in listOf(
                "http://apache.org/xml/features/disallow-doctype-decl" to true,
                "http://xml.org/sax/features/external-general-entities" to false,
                "http://xml.org/sax/features/external-parameter-entities" to false
            )) {
                try {
                    setFeature(f.first, f.second)
                } catch (_: Exception) {
                }
            }
        }
        return fabrica.newDocumentBuilder().parse(bytes.inputStream())
    }

    private const val CONTENT_TYPES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
            """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
            """<Default Extension="xml" ContentType="application/xml"/>""" +
            """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
            """<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
            """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
            """</Types>"""

    private const val RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
            """</Relationships>"""

    private const val WORKBOOK_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
            """<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
            """</Relationships>"""

    // Estilos: 0 = normal; 1 = texto (@); 2 = texto a negrito (cabeçalho)
    private const val STYLES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<styleSheet xmlns="$NS_MAIN">""" +
            """<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font>""" +
            """<font><b/><sz val="11"/><name val="Calibri"/></font></fonts>""" +
            """<fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>""" +
            """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
            """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
            """<cellXfs count="3">""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
            """<xf numFmtId="49" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>""" +
            """<xf numFmtId="49" fontId="1" fillId="0" borderId="0" xfId="0" applyNumberFormat="1" applyFont="1"/>""" +
            """</cellXfs>""" +
            """<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""" +
            """</styleSheet>"""
}
