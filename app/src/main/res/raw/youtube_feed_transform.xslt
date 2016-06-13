<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
              xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
              xmlns:atom="http://www.w3.org/2005/Atom"
              xmlns:media="http://search.yahoo.com/mrss/"
              version="1.0">
  <xsl:output method="xml" version="1.0" encoding="UTF-8"
              indent="no"
              media-type="application/rss+xml" />

  <xsl:template match="@*|node()">
    <xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy>
  </xsl:template>

  <xsl:template match="/atom:feed">
    <rss version="2.0">
      <channel>
        <xsl:apply-templates select="node()"/>
        <xsl:if test="not(atom:summary)">
          <description><xsl:value-of select="atom:title"/></description>
        </xsl:if>
      </channel>
    </rss>
  </xsl:template>

  <xsl:template match="atom:feed">
    <channel><xsl:apply-templates select="node()"/></channel>
  </xsl:template>

  <xsl:template match="atom:title">
    <title><xsl:apply-templates select="node()"/></title>
  </xsl:template>

  <xsl:template match="atom:link[@rel='self']">
    <xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy>
  </xsl:template>

  <xsl:template match="atom:link[1]">
    <link><xsl:value-of select="@href"/></link>
  </xsl:template>

  <xsl:template match="atom:category">
    <category><xsl:value-of select="@term"/></category>
  </xsl:template>

  <xsl:template match="atom:summary">
    <description><xsl:apply-templates select="node()"/></description>
  </xsl:template>

  <xsl:template match="atom:entry">
    <item><xsl:apply-templates select="node()"/></item>
  </xsl:template>

  <xsl:template match="atom:published">
    <pubDate><xsl:apply-templates select="node()"/></pubDate>
  </xsl:template>

  <xsl:template match="atom:entry/atom:id">
    <guid><xsl:apply-templates select="node()"/></guid>
  </xsl:template>

  <xsl:template match="atom:author[child::atom:name]">
    <author><xsl:apply-templates select="atom:name/node()" /></author>
  </xsl:template>

  <xsl:template match="atom:source"/>

  <!-- Duplicate thumbnail as MediaContent, since thumbnails are not handled correctly at this time -->
  <xsl:template match="media:thumbnail">
    <media:content><xsl:apply-templates select="@*|node()" /><xsl:attribute name="type">video/youtube</xsl:attribute></media:content>
  </xsl:template>

  <xsl:template match="media:content[@type='application/x-shockwave-flash']" />


</xsl:stylesheet>
