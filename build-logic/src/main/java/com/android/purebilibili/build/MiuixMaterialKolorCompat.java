package com.android.purebilibili.build;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.android.build.api.instrumentation.InstrumentationParameters;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * MaterialKolor 6 moved DynamicScheme and its nested types from scheme to dynamiccolor.
 * The nine scheme constructors and the color-role getters used by this pinned Miuix version
 * retain their signatures after that relocation. Remap descriptors, instructions and frames
 * together, preserving Miuix's spec fallback and native MonetRoles -> Colors mapping.
 *
 * Remove this adapter when Miuix itself targets MaterialKolor 6. Do not apply it to app-tv,
 * which does not select MaterialKolor 6, or to MaterialKolor's own classes.
 */
public abstract class MiuixMaterialKolorCompat
        implements AsmClassVisitorFactory<InstrumentationParameters.None> {
    private static final String TARGET = "top.yukonga.miuix.kmp.theme.ThemeControllerKt";
    private static final String OLD = "com/materialkolor/scheme/DynamicScheme";
    private static final String NEW = "com/materialkolor/dynamiccolor/DynamicScheme";

    @Override
    public boolean isInstrumentable(ClassData classData) {
        return TARGET.equals(classData.getClassName());
    }

    @Override
    public ClassVisitor createClassVisitor(ClassContext classContext, ClassVisitor next) {
        return new ClassRemapper(next, new Remapper() {
            @Override
            public String map(String internalName) {
                if (internalName.equals(OLD) || internalName.startsWith(OLD + "$")) {
                    return NEW + internalName.substring(OLD.length());
                }
                return internalName;
            }
        });
    }
}
