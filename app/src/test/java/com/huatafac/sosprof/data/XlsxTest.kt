package com.huatafac.sosprof.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class XlsxTest {

    @Test
    fun escreverELerDevolveOMesmo() {
        val linhas = listOf(
            listOf("N.º", "Nome", "Observações"),
            listOf("1", "Ana & Rita <\"irmãs\">", "linha 1\nlinha 2"),
            listOf("2", "", "0912")
        )
        val out = ByteArrayOutputStream()
        Xlsx.escrever(out, "Alunos 7.º A", linhas, listOf(6, 30, 30))
        val lidas = Xlsx.ler(out.toByteArray().inputStream())
        // A célula vazia no meio mantém a posição
        assertEquals(linhas, lidas)
    }

    @Test
    fun colunas() {
        assertEquals("A", Xlsx.letrasColuna(0))
        assertEquals("Z", Xlsx.letrasColuna(25))
        assertEquals("AA", Xlsx.letrasColuna(26))
        assertEquals(0, Xlsx.indiceColuna("A1"))
        assertEquals(27, Xlsx.indiceColuna("AB12"))
    }

    /** Ficheiro como os que o Excel grava: textos partilhados, números e células e linhas saltadas. */
    @Test
    fun lerFicheiroComoODoExcel() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun e(nome: String, s: String) {
                zip.putNextEntry(ZipEntry(nome)); zip.write(s.toByteArray()); zip.closeEntry()
            }
            e("xl/workbook.xml", """<?xml version="1.0"?><x:workbook xmlns:x="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><x:sheets><x:sheet name="Folha" sheetId="1" r:id="rId7"/></x:sheets></x:workbook>""")
            e("xl/_rels/workbook.xml.rels", """<?xml version="1.0"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId7" Type="x" Target="/xl/worksheets/folha.xml"/></Relationships>""")
            e("xl/sharedStrings.xml", """<?xml version="1.0"?><sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><si><t>N.º</t></si><si><t>Nome</t></si><si><r><t>Bea</t></r><r><t>triz</t></r></si></sst>""")
            e("xl/worksheets/folha.xml", """<?xml version="1.0"?><x:worksheet xmlns:x="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><x:sheetData>
                <x:row r="1"><x:c r="A1" t="s"><x:v>0</x:v></x:c><x:c r="B1" t="s"><x:v>1</x:v></x:c></x:row>
                <x:row r="3"><x:c r="A3"><x:v>3.0</x:v></x:c><x:c r="C3" t="s"><x:v>2</x:v></x:c><x:c r="D3"><x:v>41703</x:v></x:c></x:row>
                </x:sheetData></x:worksheet>""")
        }
        val lidas = Xlsx.ler(out.toByteArray().inputStream())
        assertEquals(listOf(listOf("N.º", "Nome"), emptyList(), listOf("3", "", "Beatriz", "41703")), lidas)
    }

    @Test
    fun ficheiroQueNaoEExcel() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip -> zip.putNextEntry(ZipEntry("a.txt")); zip.write(1); zip.closeEntry() }
        val falhou = runCatching { Xlsx.ler(out.toByteArray().inputStream()) }.isFailure
        assertTrue(falhou)
        assertTrue(runCatching { Xlsx.ler("isto não é um zip".byteInputStream()) }.isFailure)
    }
}
