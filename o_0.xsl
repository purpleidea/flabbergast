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
        <title>lib:<xsl:value-of select="o_0:lib/@o_0:name"/> – Flabbergast Documentation</title>
        <link rel="stylesheet" type="text/css" href="o_0.css"/>
        <style type="text/css" id="termcss"/>
        <script type="text/javascript" src="o_0.js"/>
        <script type="text/javascript">
          function getTerms() { return [ <xsl:for-each select="(//o_0:def/text()|//o_0:use/text())[generate-id() = generate-id(key('terms', .)[1])]"> "<xsl:value-of select="translate(., '.', '-')"/>", </xsl:for-each> ]; }
          function getLibraries() { return [ <xsl:for-each select="(//o_0:ref/text())[not(contains(., 'interop')) and generate-id() = generate-id(key('refs', .)[1])]"> "<xsl:value-of select="translate(., '/', '-')"/>", </xsl:for-each> ]; }
          hidePartials = (window.localStorage.getItem("hidePartials") || "true") === "true";
          hideExternals = (window.localStorage.getItem("hideExternals") || "false") === "true";
        </script>
      </head>
      <body onload="pageLoad();">
        <div id="searchpane">
          <div id="searcharea">
            <input type="text" id="search" onchange="searchChange();" onkeypress="searchChange();" onpaste="searchChange();" oninput="searchChange();"/>
            <span onclick="searchClear(this)">⌫</span><br/>
            <span id="hidePartials" onclick="togglePartials();" title="If an exact match is found, don't display any partial matches, even if longer.">Exact</span>
            <span id="hideExternals" onclick="toggleExternals();" title="Only display matches for this library, not libraries it uses.">Local</span>
          </div>
          <div id="terms">
            <div>
              <div id="nomatches" style="display: none">No matches.</div>
              <xsl:apply-templates mode="searchpane" select="o_0:lib//o_0:attr"/>
            </div>
          </div>
        </div>
        <h1><a href=".">⌂</a> lib:<xsl:value-of select="o_0:lib/@o_0:name"/></h1>
        <p>
          <xsl:apply-templates select="o_0:lib/o_0:description/*|o_0:lib/o_0:description/text()"/>
        </p>
        <div id="references">
          <xsl:for-each select="//o_0:ref/text()[not(contains(., 'interop')) and generate-id() = generate-id(key('refs', .)[1])]">
            <xsl:sort select="."/>
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
    <xsl:variable name="id">item-<xsl:for-each select="./ancestor::o_0:attr"><xsl:value-of select="@o_0:name"/>-</xsl:for-each><xsl:value-of select="@o_0:name"/></xsl:variable>
    <a href="{concat('#', $id)}" onclick="{concat('expandAll(', $apos, $id, $apos, ')')}"><xsl:attribute name="class"><xsl:for-each select="o_0:def[../@o_0:informative != 'true']|o_0:use|o_0:description//o_0:use"><xsl:value-of select="concat(local-name(), '_', translate(text(), '.', '-'), ' ')"/></xsl:for-each><xsl:if test="not(o_0:use|o_0:description//o_0:use)"> usenone</xsl:if><xsl:if test="@o_0:informative = 'true'"> defnone</xsl:if></xsl:attribute><xsl:for-each select="./ancestor::o_0:attr"><xsl:value-of select="concat(@o_0:name, substring(' .', 1 + count(o_0:type[text() = 'Frame']), 1))"/></xsl:for-each><xsl:value-of select="@o_0:name"/> (<xsl:value-of select="concat(@o_0:startline, ':', @o_0:startcol, '-', @o_0:endline, ':', @o_0:endcol)"/>)</a>
  </xsl:template>
  <xsl:template match="o_0:attr">
    <xsl:variable name="id">item-<xsl:for-each select="./ancestor::o_0:attr"><xsl:value-of select="@o_0:name"/>-</xsl:for-each><xsl:value-of select="@o_0:name"/></xsl:variable>
    <xsl:variable name="children" select="./o_0:attr" />
    <dt id="{$id}">
      <xsl:if test="not(.//o_0:description) and ./o_0:attr">
        <xsl:attribute name="class">hidden</xsl:attribute>
      </xsl:if>
      <xsl:choose>
        <xsl:when test="not(./o_0:attr)">
          <span class="terminal">■</span>
        </xsl:when>
        <xsl:otherwise>
          <span class="roll" onclick="showHide(this);"><span></span></span>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="@o_0:informative = 'true'">
          <span class="def" onclick="{concat('showDef(', $apos, @o_0:name, $apos, ');')}" title="Find definitions">
            <xsl:value-of select="@o_0:name"/>
          </span>
          <xsl:text> </xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <span class="use" onclick="{concat('showUse(', $apos, @o_0:name, $apos, ');')}" title="Find uses">
            <xsl:value-of select="@o_0:name"/>
          </span>
          <xsl:text> </xsl:text>
        </xsl:otherwise>
      </xsl:choose>
      <div class="info">
        <xsl:apply-templates select="o_0:type"/>
        <xsl:choose>
          <xsl:when test="/o_0:lib/@o_0:github">
            <a href="{concat(/o_0:lib/@o_0:github, '#L', @o_0:startline, '-L', @o_0:endline)}" title="View Source">
              <xsl:value-of select="concat(/o_0:lib/@o_0:name, '.o_0:', @o_0:startline, ':', @o_0:startcol, '-', @o_0:endline, ':', @o_0:endcol)"/>
            </a>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(/o_0:lib/@o_0:name, '.o_0:', @o_0:startline, ':', @o_0:startcol, '-', @o_0:endline, ':', @o_0:endcol)"/>
          </xsl:otherwise>
        </xsl:choose>
      </div>
    </dt>
    <dd>
      <xsl:apply-templates select="o_0:description/*|o_0:description/text()"/>
      <xsl:if test="o_0:use[not(text() = $children/@o_0:name)]">
        <div class="uses">Uses:
        <xsl:for-each select="o_0:use[not(text() = $children/@o_0:name)]">
          <span onclick="{concat('showDef(', $apos, translate(text(), '.', '-'), $apos, ');')}" title="Find definitions" class="deflink">
            <xsl:value-of select="text()"/></span>
            <xsl:text> </xsl:text>
        </xsl:for-each>
      </div>
      </xsl:if>
      <xsl:if test="o_0:attr">
        <dl>
          <xsl:apply-templates select="o_0:attr"/>
        </dl>
      </xsl:if>
    </dd>
  </xsl:template>
  <xsl:template match="o_0:type[text()='Bin']">
    <span class="type" title="Bin">�</span>
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
    <xsl:text> </xsl:text>
  </xsl:template>
  <xsl:template match="o_0:ref[not(contains(text(), 'interop'))]">
    <a href="{concat('doc-', translate(text(), '/', '_'), '.xml')}">lib:<xsl:value-of select="text()"/></a>
    <xsl:text> </xsl:text>
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
