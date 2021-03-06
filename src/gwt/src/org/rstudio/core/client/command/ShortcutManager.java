/*
 * ShortcutManager.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.command;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.events.NativeKeyDownEvent;
import org.rstudio.core.client.events.NativeKeyDownHandler;

import java.util.HashMap;

public class ShortcutManager implements NativePreviewHandler,
                                        NativeKeyDownHandler
{
   public interface Handle
   {
      void close();
   }

   public static final ShortcutManager INSTANCE = new ShortcutManager();

   private ShortcutManager()
   {
      Event.addNativePreviewHandler(this);
   }

   public boolean isEnabled()
   {
      return disableCount_ == 0;
   }

   public Handle disable()
   {
      disableCount_++;
      return new Handle()
      {
         private boolean closed_ = false;

         @Override
         public void close()
         {
            if (!closed_)
               disableCount_--;
            closed_ = true;
         }
      };
   }

   public void register(int modifiers, int keyCode, AppCommand command)
   {
      if (!BrowseCap.hasMetaKey() && (modifiers & KeyboardShortcut.META) != 0)
         return;
      
      KeyboardShortcut shortcut = new KeyboardShortcut(modifiers, keyCode);
      commands_.put(shortcut, command);
      command.setShortcut(shortcut);
   }

   public void onKeyDown(NativeKeyDownEvent evt)
   {
      if (evt.isCanceled())
         return;

      if (handleKeyDown(evt.getEvent()))
         evt.cancel();
   }

   public void onPreviewNativeEvent(NativePreviewEvent event)
   {
      if (event.isCanceled())
         return;

      if (event.getTypeInt() == Event.ONKEYDOWN)
      {
         if (handleKeyDown(event.getNativeEvent()))
            event.cancel();
      }
   }

   private boolean handleKeyDown(NativeEvent e)
   {
      int modifiers = KeyboardShortcut.getModifierValue(e);

      KeyboardShortcut shortcut = new KeyboardShortcut(modifiers,
                                                       e.getKeyCode());
      AppCommand command = commands_.get(shortcut);
      if (command != null)
      {
         boolean enabled = isEnabled() && command.isEnabled();
         
         // some commands want their keyboard shortcut to pass through 
         // to the browser when they are disabled (e.g. Cmd+W)
         if (!enabled && !command.preventShortcutWhenDisabled())
            return false;
         
         e.preventDefault();

         if (enabled)
            command.execute();
      }

      return command != null;
   }

   private int disableCount_ = 0;
   private final HashMap<KeyboardShortcut, AppCommand> commands_
                                  = new HashMap<KeyboardShortcut, AppCommand>();

}
