<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:atom="http://www.w3.org/2005/Atom"
   xmlns:media="http://search.yahoo.com/mrss/"
   xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"
   version="1.0">
  <xsl:output method="xml" version="1.0" encoding="UTF-8"
              indent="yes"
              media-type="application/rss+xml" />
  <xsl:strip-space elements="*" />

  <xsl:param name="expiryDateLimit"/>  
  
  <xsl:template match="@*|node()">
    <xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy>
  </xsl:template>
  
  <xsl:template match="rss">
    <rss xmlns:media="http://search.yahoo.com/mrss/">
      <xsl:apply-templates select="@*|node()"/>
    </rss>
  </xsl:template>

  <xsl:template match="itunes:image/@href">
    <xsl:attribute name="url">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="itunes:image">
    <media:content><xsl:apply-templates select="@*|node()" /><xsl:attribute name="type">image/thumbnail</xsl:attribute></media:content>
  </xsl:template>

  <xsl:template name="convertPubDate">
    <xsl:param name="date"/>
    <xsl:variable name="d">
      <xsl:value-of select="substring($date,6,2)"/>
    </xsl:variable>
    <xsl:variable name="mname">
      <xsl:value-of select="substring($date,9,3)"/>
    </xsl:variable>
    <xsl:variable name="m">
      <xsl:choose>
	<xsl:when test="contains($mname,'Jan')">01</xsl:when>
	<xsl:when test="contains($mname,'Feb')">02</xsl:when>
	<xsl:when test="contains($mname,'Mar')">03</xsl:when>
	<xsl:when test="contains($mname,'Apr')">04</xsl:when>
	<xsl:when test="contains($mname,'May')">05</xsl:when>
	<xsl:when test="contains($mname,'Jun')">06</xsl:when>
	<xsl:when test="contains($mname,'Jul')">07</xsl:when>
	<xsl:when test="contains($mname,'Aug')">08</xsl:when>
	<xsl:when test="contains($mname,'Sep')">09</xsl:when>
	<xsl:when test="contains($mname,'Oct')">10</xsl:when>
	<xsl:when test="contains($mname,'Nov')">11</xsl:when>
	<xsl:when test="contains($mname,'Dec')">12</xsl:when>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="y">
      <xsl:value-of select="substring($date,13,4)"/>
    </xsl:variable>    
    <xsl:variable name="hour">
      <xsl:value-of select="substring($date,18,2)"/>
    </xsl:variable>    
    <xsl:variable name="minute">
      <xsl:value-of select="substring($date,21,2)"/>
    </xsl:variable>    
    <xsl:variable name="second">
      <xsl:value-of select="substring($date,24,2)"/>
    </xsl:variable>    
    <xsl:value-of select="$y"/><xsl:value-of select="$m"/><xsl:value-of select="$d"/><xsl:value-of select="$hour"/><xsl:value-of select="$minute"/><xsl:value-of select="$second"/>
  </xsl:template>
  
  
  <!-- Sort item child elements. This is only to get the thumbnail (media:content) before the enclosure in the file, so that the thumb is downloaded first. -->
  <xsl:template match="item">
      <xsl:variable name="pubdate">
	<xsl:call-template name="convertPubDate">
	  <xsl:with-param name="date" select="pubDate" />
	</xsl:call-template>
      </xsl:variable>
      <xsl:if test="number($pubdate) &gt;= number($expiryDateLimit)">
	<xsl:copy>
	  <xsl:apply-templates select="*|@*">
	    <xsl:sort select="name()" order="descending"/>
	  </xsl:apply-templates>
	</xsl:copy>
      </xsl:if>
  </xsl:template>
  
</xsl:stylesheet>
