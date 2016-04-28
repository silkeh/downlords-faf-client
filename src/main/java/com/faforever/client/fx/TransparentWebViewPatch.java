/**
 * @name HTMLMonkey
 * @version 1.0
 * @author Riccardo Balbo
 * @license Copyright (c) 2015 Riccardo Balbo < riccardo @ forkforge . net >
 * <p>
 * This software is provided 'as-is', without any express or implied warranty. In no event will the authors be held
 * liable for any damages arising from the use of this software.
 * <p>
 * Permission is granted to anyone to use this software for any purpose, including commercial applications, and to alter
 * it and redistribute it freely, subject to the following restrictions:
 * <p>
 * 1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software.
 * If you use this software in a product, an acknowledgment in the product documentation would be appreciated but is not
 * required.
 * <p>
 * 2. Altered source versions must be plainly marked as such, and must not be misrepresented as being the original
 * software.
 * <p>
 * 3. This notice may not be removed or altered from any source distribution.
 * <p>
 * Modified by Michel Jung
 */

package com.faforever.client.fx;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class TransparentWebViewPatch implements ClassFileTransformer {

  private static ClassPool CLASS_POOL = ClassPool.getDefault();
  // This patch aims to make the WebView capable to correctly render
  // transparent pages.

  @Override
  public byte[] transform(ClassLoader loader, String class_name, Class<?> class_being_redefined, ProtectionDomain protection_domain, byte[] byte_code) throws IllegalClassFormatException {
    if (class_name.equals("com/sun/webkit/WebPage")) {
      System.out.println("> Patching " + class_name + " ...");
      try {
        CtClass ct_class = CLASS_POOL.makeClass(new ByteArrayInputStream(byte_code));

        // First thing to do is edit the fireLoadEvent in order to force
        // the page to set its own
        // background to transparent black (0x00000000 argb) each time
        // it changes
        CtMethod fireLoadEvent_method = ct_class.getDeclaredMethod("fireLoadEvent");
        fireLoadEvent_method.insertBefore("{\n" + "    "
            + "setBackgroundColor(0);\n"
            + "}");

        // NOT WORKING WITH JAVA 1.8.0_66
        // Then, we edit the the paint2GC method in order to call
        // clearRect every time right before it
        // start to draw a clip (clips are parts of the rendered frame).
        // We need this because when the webpage is rendered with the
        // backbuffer enabled,
        // every clip is just drawn over the old rendered frame without
        // care about the alpha channel.
        // NOTE: there is a system property
        // com.sun.webkit.pagebackbuffer that could do the trick without
        // this entire patch, but i didn't succeed in getting anything
        // useful out of it...
        // CtMethod
        // paint2GC_method=ct_class.getDeclaredMethod("paint2GC");
        // paint2GC_method.insertAt(696,
        // "{\n" +
        // " com.sun.webkit.graphics.WCRectangle clip=$1.getClip(); \n"
        // +
        // "
        // $1.clearRect(clip.getX(),clip.getY(),clip.getWidth(),clip.getHeight());\n"
        // +
        // "}"
        // );

        // Then we replace the scroll method body in order to force the
        // repaint of the entire frame
        // when the page is scrolled
        CtMethod scroll_method = ct_class.getDeclaredMethod("scroll");
        scroll_method.setBody(
            "{\n" + "   "
                + "addDirtyRect(new com.sun.webkit.graphics.WCRectangle(0f,0f,(float)width,(float)height));\n"
                + "}"
        );
        byte_code = ct_class.toBytecode();
        ct_class.detach();
      } catch (Exception e) {
        System.out.println("/!\\ " + class_name + " patching failed :(");
        e.printStackTrace();
        return byte_code;
      }
      System.out.println("> " + class_name + " patching succeeded!");
    } else if (class_name.equals("com/sun/javafx/webkit/prism/WCGraphicsPrismContext")) {
      System.out.println("> Patching " + class_name + " ...");
      try {
        CtClass ct_class = CLASS_POOL.makeClass(new ByteArrayInputStream(byte_code));

        // Then, we edit the the WCGraphicsPrismContext.setClip method
        // in order to call clearRect over the area of the clip.
        CtClass signature[] = new CtClass[]{CLASS_POOL.get("com.sun.webkit.graphics.WCRectangle")};
        CtMethod setClip_method = ct_class.getDeclaredMethod("setClip", signature);
        setClip_method.insertBefore(
            "{" + "  "
                + " $0.clearRect($1.getX(),$1.getY(),$1.getWidth(),$1.getHeight());"
                + "}");
        byte_code = ct_class.toBytecode();
        ct_class.detach();
      } catch (Exception e) {
        System.out.println("/!\\ " + class_name + " patching failed :(");
        e.printStackTrace();
        return byte_code;
      }
      System.out.println("> " + class_name + " patching succeeded!");
    }

    return byte_code;
  }

  public static void premain(String agentArguments, Instrumentation instrumentation) {
    instrumentation.addTransformer(new TransparentWebViewPatch());
  }
}
