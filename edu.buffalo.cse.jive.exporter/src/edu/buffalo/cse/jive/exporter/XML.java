package edu.buffalo.cse.jive.exporter;

import java.util.Map;

public final class XML
{
  public static String tagOpen(final String tagName, final Map<String, String> attributes)
  {
    final StringBuffer attrs = new StringBuffer("");
    for (final String key : attributes.keySet())
    {
      attrs.append(" ").append(key).append("=\"").append(XML.CData(attributes.get(key)))
          .append("\"");
    }
    return "<" + tagName + attrs.toString() + ">";
  }

  public static String CData(final String value)
  {
    return XML.escape(value);
  }

  public static String escape(final String value)
  {
    // ![CDATA[%s]]
    String result = "";
    for (final char ch : value.toCharArray())
    {
      if (ch == '&')
      {
        result += "&amp;";
      }
      else if (ch == '<')
      {
        result += "&lt;";
      }
      else if (ch == '>')
      {
        result += "&gt;";
      }
      else if (ch == '"')
      {
        result += "&quot;";
      }
      else
      {
        result += ch;
      }
    }
    return result;
  }

  public static String PCData(final long value)
  {
    return String.valueOf(value);
  }

  public static String tagClose(final String tagName)
  {
    return "</" + tagName + ">";
  }

  public static String tagOpen(final String tagName)
  {
    return "<" + tagName + ">";
  }
}
