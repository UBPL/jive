package edu.buffalo.cse.jive.internal.ui;

import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_OM_CONTOUR_INSTANCE;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_OM_CONTOUR_INTERFACE;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_OM_CONTOUR_METHOD;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_OM_CONTOUR_STATIC;
import static edu.buffalo.cse.jive.preferences.ImageInfo.IM_OM_CONTOUR_THREAD;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import edu.buffalo.cse.jive.model.IContourModel.IContextContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IContourModel.IMethodContour;
import edu.buffalo.cse.jive.model.IModel.IOutOfModelMethodReference;
import edu.buffalo.cse.jive.model.IModel.IThreadValue;
import edu.buffalo.cse.jive.model.IModel.IValue;
import edu.buffalo.cse.jive.model.IStaticModel.NodeKind;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.ui.IContourAttributes;
import edu.buffalo.cse.jive.ui.IMemberAttributes;

public class ContourAttributesFactory
{
  public static IContourAttributes createInstanceAttributes(final IContextContour contour)
  {
    return new InstanceContourAttributes(contour);
  }

  public static IMemberAttributes createMemberAttributes(final IContourMember member,
      final boolean isArrayMember, final long eventId)
  {
    return new MemberAttributes(member, isArrayMember, eventId);
  }

  public static IContourAttributes createMethodAttributes(final IMethodContour contour)
  {
    return new MethodContourAttributes(contour);
  }

  public static IContourAttributes createMethodAttributes(final IMethodContour contour,
      final Color backgroundColor)
  {
    return new MethodContourAttributes(contour, backgroundColor);
  }

  public static IContourAttributes createStaticAttributes(final IContextContour contour)
  {
    return new StaticContourAttributes(contour);
  }

  public static IContourAttributes createThreadAttributes(final IThreadValue thread,
      final Color backgroundColor)
  {
    return new ThreadAttributes(thread, backgroundColor);
  }

  private abstract static class ContourAttributes implements IContourAttributes
  {
    private static char VALUE_TEXT_DELIMITER = '.';
    private static char VALUE_TYPE_DELIMITER = '$';
    private static char VALUE_METHOD_DELIMITER = '#';

    protected String computeDefaultText(final String text)
    {
      // try {
      final String result = text
          .substring(text.lastIndexOf(ContourAttributes.VALUE_TEXT_DELIMITER) + 1);
      final String result2 = result.substring(result
          .lastIndexOf(ContourAttributes.VALUE_TYPE_DELIMITER) + 1);
      if (result2.contains(":"))
      {
        try
        {
          Integer.valueOf(result2.substring(0, result2.indexOf(":")));
          return result;
        }
        catch (final NumberFormatException nfe)
        {
          // fine, the name has alpha characters
        }
      }
      return result2;
      // }
      // catch (Throwable t) {
      // return "error";
      // }
    }

    protected String computeMethodText(final String text)
    {
      int ix = text.lastIndexOf(ContourAttributes.VALUE_METHOD_DELIMITER);
      String result = text;
      if (ix >= 0)
      {
        result = result.substring(ix + 1);
      }
      ix = result.lastIndexOf(ContourAttributes.VALUE_TEXT_DELIMITER);
      if (ix >= 0)
      {
        result = result.substring(ix + 1);
      }
      ix = result.lastIndexOf(ContourAttributes.VALUE_TYPE_DELIMITER);
      if (ix >= 0)
      {
        result = result.substring(ix + 1);
      }
      return result;
    }
  }

  private static class InstanceContourAttributes extends ContourAttributes
  {
    private static final Image CONTOUR_IMAGE = IM_OM_CONTOUR_INSTANCE.enabledImage();
    private final String text;
    private final String toolTipText;

    private InstanceContourAttributes(final IContextContour contour)
    {
      toolTipText = contour.signature();
      text = computeDefaultText(toolTipText);
    }

    @Override
    public Image getIcon()
    {
      return InstanceContourAttributes.CONTOUR_IMAGE;
    }

    @Override
    public Color getLabelBackgroundColor()
    {
      return IContourAttributes.BACKGROUND_COLOR_OBJECT;
    }

    @Override
    public String getText()
    {
      return text;
    }

    @Override
    public Image getToolTipIcon()
    {
      return InstanceContourAttributes.CONTOUR_IMAGE;
    }

    @Override
    public String getToolTipText()
    {
      return toolTipText;
    }
  }

  private static class MemberAttributes implements IMemberAttributes
  {
    private static int MAX_STRING = 27;
    private static char VALUE_TEXT_DELIMITER = '.';
    private static char VALUE_TYPE_DELIMITER = '$';
    private final String identifierText;
    private final String identifierToolTipText;
    private final boolean isArgument;
    private final boolean isArrayMember;
    private final boolean isField;
    // private final boolean isGarbageCollected;
    private final boolean isOutOfModel;
    private final boolean isOutOfScope;
    private final boolean isResult;
    private final boolean isRpdl;
    private final String typeText;
    private final String typeToolTipText;
    private final String valueText;
    private final String valueToolTipText;

    public MemberAttributes(final IContourMember member, final boolean isArrayMember,
        final long eventId)
    {
      assert member != null : "Cannot create a figure for a null variable instance";
      this.isArrayMember = isArrayMember;
      isField = member.schema().kind() == NodeKind.NK_FIELD;
      isResult = !isField && member.schema().modifiers().contains(NodeModifier.NM_RESULT);
      isRpdl = !isField && member.schema().modifiers().contains(NodeModifier.NM_RPDL);
      isArgument = !isField && member.schema().modifiers().contains(NodeModifier.NM_ARGUMENT);
      // isGarbageCollected = member.value().isGarbageCollected(eventId);
      isOutOfScope = !isField && !isResult && member.value().isUninitialized();
      isOutOfModel = !isResult && member.value().isOutOfModel() && !member.value().isResolved();
      identifierText = createIdentifierText(member);
      identifierToolTipText = createIdentifierToolTip(member);
      typeToolTipText = member.schema().type().node() == null ? member.schema().type().name()
          : member.schema().type().node().name();
      typeText = createText(typeToolTipText);
      valueText = createValueText(member);
      valueToolTipText = createValueToolTip(member);
    }

    @Override
    public Image getIdentifierIcon()
    {
      return null;
    }

    @Override
    public String getIdentifierText()
    {
      return identifierText;
    }

    @Override
    public String getIdentifierToolTipText()
    {
      return identifierToolTipText;
    }

    @Override
    public Image getTypeIcon()
    {
      return null;
    }

    @Override
    public String getTypeText()
    {
      return typeText;
    }

    @Override
    public Image getTypeToolTipIcon()
    {
      return null;
    }

    @Override
    public String getTypeToolTipText()
    {
      return typeToolTipText;
    }

    @Override
    public Image getValueIcon()
    {
      return null;
    }

    @Override
    public String getValueText()
    {
      return valueText;
    }

    @Override
    public Image getValueToolTipIcon()
    {
      return null;
    }

    @Override
    public String getValueToolTipText()
    {
      return valueToolTipText;
    }

    @Override
    public boolean isField()
    {
      return this.isField;
    }

    // @Override
    // public boolean isGarbageCollected()
    // {
    // return this.isGarbageCollected;
    // }
    @Override
    public boolean isOutOfModel()
    {
      return this.isOutOfModel;
    }

    @Override
    public boolean isVarArgument()
    {
      return this.isArgument;
    }

    @Override
    public boolean isVarOutOfScope()
    {
      return this.isOutOfScope;
    }

    @Override
    public boolean isVarResult()
    {
      return this.isResult;
    }

    @Override
    public boolean isVarRpdl()
    {
      return this.isRpdl;
    }

    private boolean canShortenValueText(final IContourMember member)
    {
      final IValue value = member.value();
      final String type = member.schema().type().name();
      if (value.isContourReference())
      {
        return true;
      }
      else if (value.isResolved())
      {
        if (type.equals("java.lang.Float") || type.equals("java.lang.Double"))
        {
          return false;
        }
        return member.value().toString().length() > (MemberAttributes.MAX_STRING + 3);
      }
      else if (value.isOutOfModel()
          && (value.toString().contains("" + MemberAttributes.VALUE_TEXT_DELIMITER) || value
              .toString().contains("" + MemberAttributes.VALUE_TYPE_DELIMITER)))
      {
        return true;
      }
      else if (value.isInModel())
      {
        return !type.equalsIgnoreCase("float") && !type.equalsIgnoreCase("double");
      }
      return false;
    }

    private String createIdentifierText(final IContourMember member)
    {
      if (isArrayMember)
      {
        // bypass the schema
        return member.name();
      }
      final String marker = (isField ? member.schema().visibility().marker() + " " : "");
      return marker + (isResult ? member.schema().name().toUpperCase() : member.schema().name());
    }

    private String createIdentifierToolTip(final IContourMember member)
    {
      if (isArrayMember)
      {
        return "array member";
      }
      final String modifier = isField ? member.schema().visibility().description() : "";
      return isField ? modifier + " field " + member.schema().name() : isRpdl ? "return point"
          : isResult ? "method result" : isArgument ? "argument"
              : isOutOfScope ? "out-of-scope variable" : "in-scope variable";
    }

    private String createShortText(final String text)
    {
      if (text.startsWith("\"") && text.endsWith("\""))
      {
        try
        {
          return text.substring(0, MemberAttributes.MAX_STRING) + "...\"";
        }
        catch (final Exception e)
        {
          e.printStackTrace();
        }
      }
      return createText(text);
    }

    private String createText(final String text)
    {
      String result = text.substring(text.lastIndexOf(MemberAttributes.VALUE_TEXT_DELIMITER) + 1);
      result = result.substring(result.lastIndexOf(MemberAttributes.VALUE_TYPE_DELIMITER) + 1);
      return result;
    }

    private String createValueText(final IContourMember member)
    {
      if (isRpdl && member.value().isOutOfModelMethodReference()
          && ((IOutOfModelMethodReference) member.value()).method() != null)
      {
        return createText(((IOutOfModelMethodReference) member.value()).method().signature());
      }
      else if (canShortenValueText(member))
      {
        return createShortText(member.value().toString());
      }
      return member.value().toString();
    }

    private String createValueToolTip(final IContourMember member)
    {
      if (isRpdl)
      {
        return (isOutOfModel ? "out-of-model RPDL: " : "RPDL: ") + member.value().toString();
      }
      String result = "";
      if (isOutOfModel)
      {
        result += "out-of-model: ";
      }
      // else if (isGarbageCollected)
      // {
      // result += "garbage collected: ";
      // }
      return result + member.value().toString();
    }
  }

  private static class MethodContourAttributes extends ContourAttributes
  {
    private static final Image CONTOUR_IMAGE = IM_OM_CONTOUR_METHOD.enabledImage();
    private final Color backgroundColor;
    private final String text;
    private final String toolTipText;

    private MethodContourAttributes(final IMethodContour contour)
    {
      this(contour, IContourAttributes.BACKGROUND_COLOR_JIVE_VARS);
    }

    private MethodContourAttributes(final IMethodContour contour, final Color backgroundColor)
    {
      this.toolTipText = contour.signature();
      this.text = computeMethodText(toolTipText);
      this.backgroundColor = backgroundColor;
    }

    @Override
    public Image getIcon()
    {
      return MethodContourAttributes.CONTOUR_IMAGE;
    }

    @Override
    public Color getLabelBackgroundColor()
    {
      return backgroundColor;
    }

    @Override
    public String getText()
    {
      return text;
    }

    @Override
    public Image getToolTipIcon()
    {
      return MethodContourAttributes.CONTOUR_IMAGE;
    }

    @Override
    public String getToolTipText()
    {
      return toolTipText;
    }
  }

  private static class StaticContourAttributes extends ContourAttributes
  {
    private static final Image CLASS_IMAGE = IM_OM_CONTOUR_STATIC.enabledImage();
    private static final Image INTERFACE_IMAGE = IM_OM_CONTOUR_INTERFACE.enabledImage();
    private final boolean isInterface;
    private final String text;
    private final String toolTipText;

    private StaticContourAttributes(final IContextContour contour)
    {
      isInterface = contour.schema().kind() == NodeKind.NK_INTERFACE;
      toolTipText = contour.signature();
      text = computeDefaultText(toolTipText);
    }

    @Override
    public Image getIcon()
    {
      return isInterface ? StaticContourAttributes.INTERFACE_IMAGE
          : StaticContourAttributes.CLASS_IMAGE;
    }

    @Override
    public Color getLabelBackgroundColor()
    {
      return isInterface ? IContourAttributes.BACKGROUND_COLOR_INTERFACE
          : IContourAttributes.BACKGROUND_COLOR_CLASS;
    }

    @Override
    public String getText()
    {
      return text;
    }

    @Override
    public Image getToolTipIcon()
    {
      return isInterface ? StaticContourAttributes.INTERFACE_IMAGE
          : StaticContourAttributes.CLASS_IMAGE;
    }

    @Override
    public String getToolTipText()
    {
      return toolTipText;
    }
  }

  private static final class ThreadAttributes extends ContourAttributes
  {
    private final Color backgroundColor;
    private final IThreadValue thread;

    private ThreadAttributes(final IThreadValue thread, final Color backgroundColor)
    {
      this.backgroundColor = backgroundColor;
      this.thread = thread;
    }

    @Override
    public Image getIcon()
    {
      return IM_OM_CONTOUR_THREAD.enabledImage();
    }

    @Override
    public Color getLabelBackgroundColor()
    {
      return backgroundColor;
      // return IContourAttributes.BACKGROUND_COLOR_LIGHT_ORANGE;
    }

    @Override
    public String getText()
    {
      return thread.toString();
    }

    @Override
    public Image getToolTipIcon()
    {
      return IM_OM_CONTOUR_THREAD.enabledImage();
    }

    @Override
    public String getToolTipText()
    {
      return thread.toString();
    }
  }
}