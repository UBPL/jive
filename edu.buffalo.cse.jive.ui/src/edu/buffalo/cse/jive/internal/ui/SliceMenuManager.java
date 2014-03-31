package edu.buffalo.cse.jive.internal.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;

import edu.buffalo.cse.jive.debug.model.IJiveDebugTarget;
import edu.buffalo.cse.jive.launch.JiveLaunchPlugin;
import edu.buffalo.cse.jive.model.IContourModel.IContour;
import edu.buffalo.cse.jive.model.IContourModel.IContourMember;
import edu.buffalo.cse.jive.model.IStaticModel.NodeModifier;
import edu.buffalo.cse.jive.ui.ISliceMenuManager;

public class SliceMenuManager implements ISliceMenuManager
{
  @Override
  public void createSliceMenu(final IContour contour, final IMenuManager manager)
  {
    contour.model().readLock();
    try
    {
      final IJiveDebugTarget target = JiveLaunchPlugin.getDefault().getLaunchManager()
          .activeTarget();
      // create the menu entry if there is no active slice and the temporal model is in replay mode
      if (contour.model().sliceView().activeSlice() == null
          && (target.isSuspended() || target.isTerminated())
          && (contour.model().temporalState().canRollback() || contour.model().temporalState()
              .canReplayCommit()))
      {
        IMenuManager sliceMenu = null;
        for (final IContourMember member : contour.members())
        {
          // no RPDL variable
          if (member.schema().modifiers().contains(NodeModifier.NM_RPDL))
          {
            continue;
          }
          // no uninitialized arguments (local variables may get in/out of scope) or result
          if (member.value() == contour.model().valueFactory().createUninitializedValue()
              && member.schema().modifiers().contains(NodeModifier.NM_RESULT))
          {
            continue;
          }
          if (sliceMenu == null)
          {
            sliceMenu = createSubmenu("Slice w.r.t.", manager);
          }
          sliceMenu.add(new SliceEventAction(member));
        }
      }
    }
    finally
    {
      contour.model().readUnlock();
    }
  }

  private IMenuManager createSubmenu(final String text, final IMenuManager manager)
  {
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    final IMenuManager jumpToMenu = new MenuManager(text);
    manager.add(jumpToMenu);
    return jumpToMenu;
  }

  private final class SliceEventAction extends Action
  {
    private final IContourMember member;

    SliceEventAction(final IContourMember member)
    {
      this.member = member;
      final String value;
      if (member.value().isContourReference())
      {
        final String signature = member.value().toString();
        value = String.format(
            "%s == %s",
            member.schema().name(),
            signature.indexOf(".") == -1 ? signature
                : signature.substring(signature.indexOf(".") + 1));
      }
      else
      {
        value = String.format("%s == %s", member.schema().name(),
            member.value().isResolved() ? member.value().value() : member.value().toString());
      }
      if (value.length() > 37)
      {
        setText(value.substring(0, 37) + "...");
      }
      else
      {
        setText(value);
      }
    }

    @Override
    public void run()
    {
      member.schema().model().sliceView().computeSlice(member);
    }
  }
}
