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
        <link rel="stylesheet" type="text/css" href="o_0.css"/>
        <link rel="stylesheet" type="text/css" href="o_0-index.css"/>
      </head>
      <body>
        <h1><a href="http://flabbergast.org/">Flabbergast</a> Flabbergast Documentation</h1>
        <div id="container">
          <div id="content">
            <p>Is Flabbergast making you go o_0? If so, start with <a href="https://github.com/flabbergast-config/flabbergast/blob/master/flabbergast-manual.md">the Flabbergast Manual</a>. If you just want to know the name of that template thing that does the stuff, this the right place.</p>
            <table>
              <thead>
                <tr>
                  <th>Version</th>
                  <th>Source</th>
                </tr>
              </thead>
              <tbody>
                <xsl:apply-templates select="//o_0:versions">
                  <xsl:sort select="@id"/>
                </xsl:apply-templates>
              </tbody>
            </table>
          </div>
        </div>
      </body>
    </html>
  </xsl:template>
  <xsl:template match="o_0:version">
    <tr>
      <td>
        <a href="{@tag}">
          <xsl:value-of select="@name"/>
        </a>
      </td>
      <td>
        <a href="{concat('https://github.com/flabbergast-config/flabbergast/tree/', @tag)}">GitHub</a>
      </td>
    </tr>
  </xsl:template>
</xsl:stylesheet>
