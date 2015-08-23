<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml" xmlns:o_0="http://flabbergast.org/api" version="1.0" exclude-result-prefixes="o_0">
  <xsl:output method="html" doctype-system="about:legacy-compat" encoding="UTF-8" indent="yes"/>
  <xsl:variable name="apos">'</xsl:variable>
  <xsl:key name="refs" match="//o_0:ref/text()" use="."/>
  <xsl:key name="terms" match="//o_0:def/text()|//o_0:use/text()" use="."/>
  <xsl:template match="/">
    <html>
      <head>
        <meta http-equiv="content-type" content="text/xml; charset=utf-8"/>
        <title>lib:<xsl:value-of select="o_0:lib/@name"/> – Flabbergast Documentation</title>
        <link rel="stylesheet" type="text/css" href="o_0.css"/>
        <style type="text/css" id="termcss"/>
        <script type="text/javascript" src="o_0.js"/>
        <script type="text/javascript">function getTerms() { return [ <xsl:for-each select="(//o_0:def/text()|//o_0:use/text())[generate-id() = generate-id(key('terms', .)[1])]"> "<xsl:value-of select="translate(., '.', '-')"/>", </xsl:for-each> ]; } </script>
      </head>
      <body onload="pageLoad();">
        <div id="searchpane">
          <div id="searcharea">
            <input type="text" id="search" onchange="searchChange();" onkeypress="searchChange();" onpaste="searchChange();" oninput="searchChange();"/>
            <span onclick="searchClear(this)">⌫</span>
          </div>
          <div id="terms">
            <div>
              <xsl:apply-templates mode="searchpane" select="o_0:lib//o_0:attr"/>
              <div id="nomatches" style="display: none">No matches.</div>
            </div>
          </div>
        </div>
        <h1><a href=".">⌂</a> lib:<xsl:value-of select="o_0:lib/@name"/></h1>
        <p><xsl:apply-templates select="o_0:lib/o_0:description/*|o_0:lib/o_0:description/text()"/></p>
        <div id="references">
          <xsl:for-each select="//o_0:ref/text()[not(contains(., 'interop')) and generate-id() = generate-id(key('refs', .)[1])]">
            <a href="{concat('doc-', translate(., '/', '-'), '.xml')}">lib:<xsl:value-of select="."/></a>
          </xsl:for-each>
        </div>
        <h2>Attributes</h2>
        <dl>
          <xsl:apply-templates select="o_0:lib/o_0:attr"/>
        </dl>
      </body>
    </html>
  </xsl:template>
  <xsl:template match="o_0:attr" mode="searchpane">
    <xsl:element name="a"><xsl:attribute name="href">#<xsl:value-of select="generate-id()"/></xsl:attribute><xsl:attribute name="class"><xsl:for-each select="o_0:def|o_0:use|o_0:description//o_0:use"><xsl:value-of select="concat(local-name(), '_', translate(text(), '.', '-'), ' ')"/></xsl:for-each><xsl:if test="not(o_0:use|o_0:description//o_0:use)">usenone</xsl:if></xsl:attribute><xsl:for-each select="ancestor::attr"><xsl:value-of select="concat(@name, substring(' .', 1 + count(o_0:type[text() = 'Frame']), 1))"/></xsl:for-each><xsl:value-of select="@name"/>
			(<xsl:value-of select="concat(@startline, ':', @startcol, '-', @endline, ':', @endcol)"/>)
		</xsl:element>
  </xsl:template>
  <xsl:template match="o_0:attr">
    <dt id="{generate-id()}">
      <xsl:if test="not(.//o_0:description | ./o_0:attr)">
        <xsl:attribute name="class">hidden</xsl:attribute>
      </xsl:if>
      <xsl:choose>
        <xsl:when test="./o_0:attr|./o_0:use and not(.//o_0:description)">
          <span class="roll" onclick="showHide(this);">▶</span>
        </xsl:when>
        <xsl:when test="./o_0:attr|./o_0:use">
          <span class="roll" onclick="showHide(this);">▼</span>
        </xsl:when>
        <xsl:otherwise>
          <span class="roll">■</span>
        </xsl:otherwise>
      </xsl:choose>
      <span class="use" onclick="{concat('showUse(', $apos, @name, $apos, ');')}" title="Find uses">
        <xsl:value-of select="@name"/>
      </span>
      <div class="info">
        <xsl:apply-templates select="o_0:type"/>
        <xsl:choose>
          <xsl:when test="/o_0:lib/@github">
            <a href="{concat(/o_0:lib/@github, '#L', @startline, '-L', @endline)}">
              <xsl:value-of select="concat(/o_0:lib/@name, '.o_0:', @startline, ':', @startcol, '-', @endline, ':', @endcol)"/>
            </a>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(/o_0:lib/@name, '.o_0:', @startline, ':', @startcol, '-', @endline, ':', @endcol)"/>
          </xsl:otherwise>
        </xsl:choose>
      </div>
    </dt>
    <dd>
      <xsl:apply-templates select="o_0:description/*|o_0:description/text()"/>
      <xsl:if test="o_0:use">
        <div class="uses">Uses:
        <xsl:for-each select="o_0:use"><span onclick="{concat('showDef(', $apos, translate(text(), '.', '-'), $apos, ');')}" title="Find definitions" class="deflink"><xsl:value-of select="text()"/></span></xsl:for-each>
      </div>
      </xsl:if>
      <xsl:if test="o_0:attr">
        <dl>
          <xsl:apply-templates select="o_0:attr"/>
        </dl>
      </xsl:if>
    </dd>
  </xsl:template>
  <xsl:template match="o_0:type[text()='Bool']">
    <span class="type" title="Bool">◑</span>
  </xsl:template>
  <xsl:template match="o_0:type[text()='Float']">
    <span class="type" title="Float">ℝ</span>
  </xsl:template>
  <xsl:template match="o_0:type[text()='Frame']">
    <span class="type" title="Frame">◰</span>
  </xsl:template>
  <xsl:template match="o_0:type[text()='Int']">
    <span class="type" title="Int">ℤ</span>
  </xsl:template>
  <xsl:template match="o_0:type[text()='Null']">
    <span class="type" title="Null">∅</span>
  </xsl:template>
  <xsl:template match="o_0:type[text()='Str']">
    <span class="type" title="Str">A</span>
  </xsl:template>
  <xsl:template match="o_0:type[text()='Template']">
    <span class="type" title="Template">⬚</span>
  </xsl:template>
  <xsl:template match="o_0:use">
    <span onclick="{concat('showDef(', $apos, text(), $apos, ');')}" title="Find definitions" class="show">
      <xsl:value-of select="text()"/>
    </span>
  </xsl:template>
  <xsl:template match="o_0:ref[not(contains(text(), 'interop'))]">
    <a href="{concat('doc-', translate(text(), '/', '_'), '.xml')}">lib:<xsl:value-of select="text()"/></a>
  </xsl:template>
  <xsl:template match="o_0:ref">
    <span class="show" title="Documentation not available.">lib:<xsl:value-of select="text()"/></span>
  </xsl:template>
  <xsl:template match="text()|*[namespace-uri() = 'http://www.w3.org/1999/xhtml']">
    <xsl:element name="{local-name()}">
      <xsl:apply-templates select="*|@*|text()"/>
    </xsl:element>
  </xsl:template>
  <xsl:template match="text()|@*">
    <xsl:copy-of select="."/>
  </xsl:template>
</xsl:stylesheet>
