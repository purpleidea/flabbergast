<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml" xmlns:o_0="http://flabbergast.org/api" version="1.0" exclude-result-prefixes="o_0">
  <xsl:output method="html" doctype-system="about:legacy-compat" encoding="UTF-8" indent="yes"/>
  <xsl:param name="knownterms"/>
  <xsl:key name="terms" match="//o_0:def/text()|//o_0:use/text()" use="."/>
  <xsl:template match="/">
    <xsl:for-each select="o_0:lib//o_0:attr[contains($knownterms, concat('[', @o_0:name, ']'))]">
      <xsl:variable name="id">item-<xsl:for-each select="./ancestor::o_0:attr"><xsl:value-of select="@o_0:name"/>-</xsl:for-each><xsl:value-of select="@o_0:name"/></xsl:variable>
      <xsl:variable name="uses" select="(o_0:use|o_0:description//o_0:use)[contains($knownterms, concat('[', text(), ']'))]"/>
      <xsl:variable name="defs" select="o_0:def[../@o_0:informative != 'true' and contains($knownterms, concat('[', text(), ']'))]"/>
      <a href="{concat('doc-', translate(/o_0:lib/@o_0:name, '/', '-') ,'.xml#', $id)}"><xsl:attribute name="class">external <xsl:for-each select="$uses|$defs"><xsl:value-of select="concat(local-name(), '_', translate(text(), '.', '-'), ' ')"/></xsl:for-each>
        <xsl:if test="not($uses)"> usenone</xsl:if>
        <xsl:if test="not($defs)"> defnone</xsl:if>
      </xsl:attribute>🔗lib:<xsl:value-of select="/o_0:lib/o_0:name"/> <xsl:for-each select="./ancestor::o_0:attr"><xsl:value-of select="concat(@o_0:name, substring(' .', 1 + count(o_0:type[text() = 'Frame']), 1))"/></xsl:for-each><xsl:value-of select="@o_0:name"/> (<xsl:value-of select="concat(@o_0:startline, ':', @o_0:startcol, '-', @o_0:endline, ':', @o_0:endcol)"/>)</a>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
