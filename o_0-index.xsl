<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml" xmlns:o_0="http://flabbergast.org/api" version="1.0" exclude-result-prefixes="o_0">
  <xsl:output method="html" doctype-system="about:legacy-compat" encoding="UTF-8" indent="yes"/>
  <xsl:variable name="apos">'</xsl:variable>
  <xsl:template match="/">
    <html>
      <head>
        <meta http-equiv="content-type" content="text/xml; charset=utf-8"/>
        <title>Flabbergast Documentation</title>
        <link href="https://fonts.googleapis.com/css?family=Quicksand" rel="stylesheet"/>
        <link rel="icon" href="/icon16.png" type="image/png" sizes="16x16"/>
        <link rel="icon" href="/icon32.png" type="image/png" sizes="32x32"/>
        <link rel="icon" href="/icon64.png" type="image/png" sizes="64x64"/>
        <link rel="stylesheet" type="text/css" href="o_0.css"/>
        <link rel="stylesheet" type="text/css" href="o_0-index.css"/>
      </head>
      <body>
        <h1><a href="http://flabbergast.org/">Flabbergast</a> Flabbergast Documentation</h1>
        <div id="container">
          <div id="content">
            <p>Is Flabbergast making you go o_0? If so, start with <a href="https://github.com/flabbergast-config/flabbergast/blob/master/flabbergast-manual.md">the Flabbergast Manual</a>. If you just want to know the name of that template thing that does the stuff, this the right place.</p>
            <h2>Libraries</h2>
            <table>
              <thead>
                <tr>
                  <th>URI</th>
                  <th>Description</th>
                </tr>
              </thead>
              <tbody>
                <xsl:apply-templates select="//o_0:ref_link">
                  <xsl:sort select="@o_0:name"/>
                </xsl:apply-templates>
              </tbody>
            </table>
            <h2>Manual Pages</h2>
            <table>
              <thead>
                <tr>
                  <th>Section</th>
                  <th>Name</th>
                  <th>Description</th>
                  <th>Download</th>
                </tr>
              </thead>
              <tbody>
                <xsl:apply-templates select="//o_0:man">
                  <xsl:sort select="@o_0:section" data-type="number"/>
                  <xsl:sort select="@o_0:name"/>
                </xsl:apply-templates>
              </tbody>
            </table>
          </div>
        </div>
      </body>
    </html>
  </xsl:template>
  <xsl:template match="o_0:man">
    <tr>
      <td>
        <xsl:attribute name="title">
          <xsl:choose>
            <xsl:when test="@o_0:section = 1">General commands</xsl:when>
            <xsl:when test="@o_0:section = 2">System calls</xsl:when>
            <xsl:when test="@o_0:section = 3">Library functions</xsl:when>
            <xsl:when test="@o_0:section = 4">Special files</xsl:when>
            <xsl:when test="@o_0:section = 5">File formats and conventions</xsl:when>
            <xsl:when test="@o_0:section = 6">Games and screensavers</xsl:when>
            <xsl:when test="@o_0:section = 7">Miscellanea</xsl:when>
            <xsl:when test="@o_0:section = 8">System administration commands and daemons</xsl:when>
            <xsl:otherwise>Unknown</xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
        <xsl:value-of select="@o_0:section"/>
      </td>
      <td>
        <a href="{concat(@o_0:name, '.', @o_0:section, '.html')}">
          <xsl:value-of select="@o_0:name"/>
        </a>
      </td>
      <td>
        <xsl:value-of select="@o_0:description"/>
      </td>
      <td>
        <a href="{concat(@o_0:name, '.', @o_0:section, '.pdf')}">PDF</a>
      </td>
    </tr>
  </xsl:template>
  <xsl:template match="o_0:ref_link">
    <tr>
      <td>
        <a href="{concat('doc-', translate(@o_0:name, '/', '-'), '.xml')}">lib:<xsl:value-of select="@o_0:name"/></a>
      </td>
      <td>
        <xsl:copy-of select="*|text()"/>
      </td>
    </tr>
  </xsl:template>
</xsl:stylesheet>
