<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml" xmlns:o_0="http://flabbergast.org/api" version="1.0" exclude-result-prefixes="o_0">
  <xsl:output method="html" doctype-system="about:legacy-compat" encoding="UTF-8" indent="yes"/>
  <xsl:param name="knownterms"/>
  <xsl:key name="terms" match="//o_0:def/text()|//o_0:use/text()" use="."/>
  <xsl:template match="/">
    <xsl:for-each select="o_0:lib//o_0:attr[contains($knownterms, concat('[', @name, ']'))]">
    <xsl:variable name="id">item-<xsl:for-each select="./ancestor::o_0:attr"><xsl:value-of select="@name"/>-</xsl:for-each><xsl:value-of select="@name"/></xsl:variable>
    <a href="{concat('doc-', translate(/o_0:lib/@name, '/', '-') ,'.xml#', $id)}"><xsl:attribute name="class">external <xsl:for-each select="o_0:def[../@informative != 'true']|o_0:use|o_0:description//o_0:use"><xsl:value-of select="concat(local-name(), '_', translate(text(), '.', '-'), ' ')"/></xsl:for-each><xsl:if test="not(o_0:use|o_0:description//o_0:use)"> usenone</xsl:if><xsl:if test="@informative = 'true'"> defnone</xsl:if></xsl:attribute>🔗lib:<xsl:value-of select="/o_0:lib/@name"/> <xsl:for-each select="./ancestor::o_0:attr"><xsl:value-of select="concat(@name, substring(' .', 1 + count(o_0:type[text() = 'Frame']), 1))"/></xsl:for-each><xsl:value-of select="@name"/> (<xsl:value-of select="concat(@startline, ':', @startcol, '-', @endline, ':', @endcol)"/>)</a>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
