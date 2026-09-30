package com.crazylimits.shared_resources.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Checks every vanilla mixin against the Minecraft classes of the version and loader being built,
 * the way Mixin would when applying them: target classes, shadows, accessors, target methods and injection points.
 * <p>
 * Mixin targets are plain strings, so a renamed method or changed call site compiles fine and only fails
 * once the game starts. This catches that at build time, on every target.
 */
class MixinTargetsTest {
    private static final String MAIN_CONFIG = "shared-resources.mixins.json";
    private static final String COMPAT_CONFIG = "shared-resources.compat.mixins.json";

    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String PSEUDO = "Lorg/spongepowered/asm/mixin/Pseudo;";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String ACCESSOR = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String COMPAT_MIXIN = "Lcom/crazylimits/shared_resources/compat/CompatMixin;";
    private static final Set<String> INJECTORS = Set.of(
            "Lorg/spongepowered/asm/mixin/injection/Inject;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyArg;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;",
            "Lorg/spongepowered/asm/mixin/injection/Redirect;",
            "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;",
            "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;",
            "Lcom/llamalad7/mixinextras/injector/WrapWithCondition;",
            "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;"
    );

    @TestFactory
    Stream<DynamicTest> vanillaMixinsMatchMinecraft() {
        return mixinClasses(MAIN_CONFIG).stream()
                .map(name -> DynamicTest.dynamicTest(simpleName(name), () -> {
                    List<String> problems = verifyMixin(name);
                    if (!problems.isEmpty()) {
                        fail(name + " would not apply:\n  " + String.join("\n  ", problems));
                    }
                }));
    }

    @TestFactory
    Stream<DynamicTest> compatMixinsAreGuarded() {
        return mixinClasses(COMPAT_CONFIG).stream()
                .map(name -> DynamicTest.dynamicTest(simpleName(name), () -> {
                    ClassNode mixin = readClass(internal(name));
                    assertNotNull(mixin, "Mixin class is missing");
                    // Compat targets live in other mods, so they must not break the game when those mods are absent
                    assertNotNull(annotation(mixin.invisibleAnnotations, PSEUDO), "Compat mixins must be @Pseudo");
                    AnnotationNode compat = annotation(mixin.visibleAnnotations, COMPAT_MIXIN);
                    assertNotNull(compat, "Compat mixins must declare their mods with @CompatMixin");
                    List<?> mods = (List<?>) value(compat, "value");
                    assertTrue(mods != null && !mods.isEmpty(), "@CompatMixin must name at least one mod");
                }));
    }

    @Test
    void configsListEveryMixin() throws IOException, URISyntaxException {
        for (String config : List.of(MAIN_CONFIG, COMPAT_CONFIG)) {
            JsonObject json = readConfig(config);
            String pkg = json.get("package").getAsString();
            Set<String> listed = Set.copyOf(mixinClasses(config));

            URL dir = getClass().getClassLoader().getResource(pkg.replace('.', '/'));
            assertNotNull(dir, "Mixin package " + pkg + " not found");
            if (!dir.getProtocol().equals("file")) continue; // Only checkable against a classes directory
            Path root = Paths.get(dir.toURI());

            List<String> unlisted;
            try (Stream<Path> files = Files.walk(root)) {
                unlisted = files
                        .filter(file -> file.toString().endsWith(".class") && !file.getFileName().toString().contains("$"))
                        .map(file -> pkg + "." + root.relativize(file).toString()
                                .replace(".class", "").replace(java.io.File.separatorChar, '.'))
                        .filter(name -> isMixin(name) && !listed.contains(name))
                        .collect(Collectors.toList());
            }
            assertEquals(Collections.emptyList(), unlisted, "Mixins missing from " + config);
        }
    }

    @Test
    void configsAreFilledIn() throws IOException {
        for (String config : List.of(MAIN_CONFIG, COMPAT_CONFIG)) {
            JsonObject json = readConfig(config);
            String level = json.get("compatibilityLevel").getAsString();
            assertTrue(level.matches("JAVA_\\d+"), config + " has an unexpanded compatibilityLevel: " + level);
            if (com.crazylimits.shared_resources.platform.Platform.LOADER.equals("forge")) {
                // Forge ships an older Mixin that refuses to start with anything newer
                int java = Integer.parseInt(level.substring("JAVA_".length()));
                assertTrue(java <= 21, config + " asks for " + level + ", Forge's Mixin only knows up to JAVA_21");
            }
            if (json.has("plugin")) {
                assertNotNull(readClass(internal(json.get("plugin").getAsString())), config + " plugin is missing");
            }
        }
    }

    // --- Verification ---

    private static List<String> verifyMixin(String name) {
        List<String> problems = new ArrayList<>();
        ClassNode mixin = readClass(internal(name));
        if (mixin == null) {
            problems.add("mixin class is missing");
            return problems;
        }
        AnnotationNode mixinAnnotation = annotation(mixin.visibleAnnotations, mixin.invisibleAnnotations, MIXIN);
        if (mixinAnnotation == null) {
            problems.add("not annotated with @Mixin");
            return problems;
        }

        for (String targetName : targets(mixinAnnotation)) {
            ClassNode target = readClass(targetName);
            if (target == null) {
                problems.add("target class " + targetName + " does not exist");
                continue;
            }
            verifyMembers(mixin, target, problems);
            for (MethodNode method : mixin.methods) {
                for (AnnotationNode injector : annotations(method)) {
                    if (injector.desc.equals("Lorg/spongepowered/asm/mixin/injection/ModifyArgs;")) {
                        problems.add("@ModifyArgs " + method.name + " crashes on Forge, use one @ModifyArg per argument");
                    }
                    if (INJECTORS.contains(injector.desc)) {
                        verifyInjector(method.name, injector, target, problems);
                    }
                }
            }
        }
        return problems;
    }

    private static void verifyMembers(ClassNode mixin, ClassNode target, List<String> problems) {
        for (FieldNode field : mixin.fields) {
            if (annotation(field.visibleAnnotations, field.invisibleAnnotations, SHADOW) != null) {
                if (findField(target, field.name, field.desc) == null) {
                    problems.add("@Shadow field " + field.name + " " + field.desc + " not found in " + target.name);
                }
            }
        }
        for (MethodNode method : mixin.methods) {
            if (annotation(method.visibleAnnotations, method.invisibleAnnotations, SHADOW) != null) {
                if (findMethod(target, method.name, method.desc) == null) {
                    problems.add("@Shadow method " + method.name + method.desc + " not found in " + target.name);
                }
            }
            AnnotationNode accessor = annotation(method.visibleAnnotations, method.invisibleAnnotations, ACCESSOR);
            if (accessor != null) {
                String fieldName = (String) value(accessor, "value");
                if (fieldName == null || fieldName.isEmpty()) {
                    problems.add("@Accessor " + method.name + " should name its field");
                } else if (findField(target, fieldName, Type.getReturnType(method.desc).getDescriptor()) == null) {
                    problems.add("@Accessor field " + fieldName + " not found in " + target.name);
                }
            }
        }
    }

    private static void verifyInjector(String handler, AnnotationNode injector, ClassNode target, List<String> problems) {
        String label = "@" + simpleName(Type.getType(injector.desc).getClassName()) + " " + handler;
        @SuppressWarnings("unchecked")
        List<String> selectors = (List<String>) value(injector, "method");
        if (selectors == null || selectors.isEmpty()) {
            problems.add(label + " has no target method");
            return;
        }

        List<MethodNode> methods = new ArrayList<>();
        for (String selector : selectors) {
            List<MethodNode> matched = selectMethods(target, selector);
            if (matched.isEmpty()) {
                problems.add(label + ": no method " + selector + " in " + target.name);
            }
            methods.addAll(matched);
        }
        if (methods.isEmpty()) return;

        for (AnnotationNode at : ats(value(injector, "at"))) {
            verifyAt(label, at, methods, problems);
        }
        Object slice = value(injector, "slice");
        if (slice instanceof AnnotationNode) {
            for (String bound : List.of("from", "to")) {
                Object at = value((AnnotationNode) slice, bound);
                if (at instanceof AnnotationNode) verifyAt(label + " slice." + bound, (AnnotationNode) at, methods, problems);
            }
        }
    }

    private static void verifyAt(String label, AnnotationNode at, List<MethodNode> methods, List<String> problems) {
        String point = (String) value(at, "value");
        String target = (String) value(at, "target");
        Integer ordinal = (Integer) value(at, "ordinal");
        int count = 0;
        for (MethodNode method : methods) {
            count += countMatches(point, target, stringArgs(at), method);
        }
        if (count < 0) return; // Not an injection point we can check

        int needed = ordinal != null && ordinal >= 0 ? ordinal + 1 : 1;
        if (count < needed) {
            String what = target != null ? target : String.join(",", stringArgs(at));
            problems.add(label + ": @At(" + point + ") " + what + " matched " + count + " time(s), needs " + needed);
        }
    }

    /**
     * How often an injection point matches in a method, or -1 when it can't be checked statically.
     */
    private static int countMatches(String point, String target, List<String> args, MethodNode method) {
        switch (point) {
            case "HEAD":
            case "TAIL":
            case "RETURN":
            case "STORE":
            case "LOAD":
                return 1;
            case "INVOKE":
            case "INVOKE_ASSIGN": {
                MemberRef ref = MemberRef.parse(target);
                return count(method, insn -> insn instanceof MethodInsnNode
                        && ((MethodInsnNode) insn).owner.equals(ref.owner)
                        && ((MethodInsnNode) insn).name.equals(ref.name)
                        && ((MethodInsnNode) insn).desc.equals(ref.desc));
            }
            case "FIELD": {
                MemberRef ref = MemberRef.parse(target);
                return count(method, insn -> insn instanceof FieldInsnNode
                        && ((FieldInsnNode) insn).owner.equals(ref.owner)
                        && ((FieldInsnNode) insn).name.equals(ref.name)
                        && ((FieldInsnNode) insn).desc.equals(ref.desc));
            }
            case "NEW": {
                if (target.startsWith("(")) {
                    // Constructor descriptor form: (args)Lowner;
                    Type type = Type.getMethodType(target);
                    String owner = type.getReturnType().getInternalName();
                    String desc = Type.getMethodDescriptor(Type.VOID_TYPE, type.getArgumentTypes());
                    return count(method, insn -> insn instanceof MethodInsnNode
                            && insn.getOpcode() == Opcodes.INVOKESPECIAL
                            && ((MethodInsnNode) insn).owner.equals(owner)
                            && ((MethodInsnNode) insn).name.equals("<init>")
                            && ((MethodInsnNode) insn).desc.equals(desc));
                }
                String owner = target.startsWith("L") ? target.substring(1, target.length() - 1) : target.replace('.', '/');
                return count(method, insn -> insn instanceof TypeInsnNode
                        && insn.getOpcode() == Opcodes.NEW && ((TypeInsnNode) insn).desc.equals(owner));
            }
            case "CONSTANT": {
                for (String arg : args) {
                    if (arg.startsWith("stringValue=")) {
                        String constant = arg.substring("stringValue=".length());
                        return count(method, insn -> insn instanceof LdcInsnNode && constant.equals(((LdcInsnNode) insn).cst));
                    }
                }
                return -1;
            }
            default:
                return -1;
        }
    }

    private static List<MethodNode> selectMethods(ClassNode target, String selector) {
        List<MethodNode> matched = new ArrayList<>();
        int paren = selector.indexOf('(');
        String name = paren >= 0 ? selector.substring(0, paren) : selector;
        String desc = paren >= 0 ? selector.substring(paren) : null;
        for (MethodNode method : target.methods) {
            boolean nameMatches = name.equals("*")
                    ? !method.name.equals("<init>") && !method.name.equals("<clinit>")
                    : method.name.equals(name);
            if (nameMatches && (desc == null || method.desc.equals(desc))) {
                matched.add(method);
            }
        }
        return matched;
    }

    // --- Class and annotation helpers ---

    private static ClassNode readClass(String internalName) {
        try (InputStream in = MixinTargetsTest.class.getClassLoader().getResourceAsStream(internalName + ".class")) {
            if (in == null) return null;
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, 0);
            return node;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static FieldNode findField(ClassNode target, String name, String desc) {
        for (ClassNode node = target; node != null; node = node.superName == null ? null : readClass(node.superName)) {
            for (FieldNode field : node.fields) {
                if (field.name.equals(name) && field.desc.equals(desc)) return field;
            }
        }
        return null;
    }

    private static MethodNode findMethod(ClassNode target, String name, String desc) {
        for (ClassNode node = target; node != null; node = node.superName == null ? null : readClass(node.superName)) {
            for (MethodNode method : node.methods) {
                if (method.name.equals(name) && method.desc.equals(desc)) return method;
            }
        }
        return null;
    }

    private static List<String> targets(AnnotationNode mixin) {
        List<String> targets = new ArrayList<>();
        Object classes = value(mixin, "value");
        if (classes instanceof List) {
            for (Object type : (List<?>) classes) targets.add(((Type) type).getInternalName());
        }
        Object names = value(mixin, "targets");
        if (names instanceof List) {
            for (Object target : (List<?>) names) targets.add(internal((String) target));
        }
        return targets;
    }

    private static boolean isMixin(String name) {
        ClassNode node = readClass(internal(name));
        return node != null && annotation(node.visibleAnnotations, node.invisibleAnnotations, MIXIN) != null;
    }

    private static List<AnnotationNode> annotations(MethodNode method) {
        List<AnnotationNode> all = new ArrayList<>();
        if (method.visibleAnnotations != null) all.addAll(method.visibleAnnotations);
        if (method.invisibleAnnotations != null) all.addAll(method.invisibleAnnotations);
        return all;
    }

    // Mixin's annotations differ in retention, so look in both lists
    private static AnnotationNode annotation(List<AnnotationNode> visible, List<AnnotationNode> invisible, String desc) {
        AnnotationNode found = annotation(visible, desc);
        return found != null ? found : annotation(invisible, desc);
    }

    private static AnnotationNode annotation(List<AnnotationNode> annotations, String desc) {
        if (annotations == null) return null;
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc.equals(desc)) return annotation;
        }
        return null;
    }

    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values == null) return null;
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (annotation.values.get(i).equals(key)) return annotation.values.get(i + 1);
        }
        return null;
    }

    private static List<AnnotationNode> ats(Object at) {
        List<AnnotationNode> ats = new ArrayList<>();
        if (at instanceof AnnotationNode) ats.add((AnnotationNode) at);
        if (at instanceof List) for (Object entry : (List<?>) at) ats.add((AnnotationNode) entry);
        return ats;
    }

    private static List<String> stringArgs(AnnotationNode at) {
        Object args = value(at, "args");
        List<String> result = new ArrayList<>();
        if (args instanceof List) for (Object arg : (List<?>) args) result.add((String) arg);
        return result;
    }

    private static int count(MethodNode method, java.util.function.Predicate<AbstractInsnNode> predicate) {
        int count = 0;
        for (AbstractInsnNode insn : method.instructions) {
            if (predicate.test(insn)) count++;
        }
        return count;
    }

    // --- Config helpers ---

    private static JsonObject readConfig(String config) throws IOException {
        try (InputStream in = MixinTargetsTest.class.getClassLoader().getResourceAsStream(config)) {
            assertNotNull(in, config + " not found");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static List<String> mixinClasses(String config) {
        try {
            JsonObject json = readConfig(config);
            String pkg = json.get("package").getAsString();
            List<String> names = new ArrayList<>();
            for (String side : List.of("mixins", "client", "server")) {
                if (!json.has(side)) continue;
                JsonArray entries = json.getAsJsonArray(side);
                for (JsonElement entry : entries) names.add(pkg + "." + entry.getAsString());
            }
            assertFalse(names.isEmpty(), config + " lists no mixins");
            return names;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String internal(String name) {
        return name.replace('.', '/');
    }

    private static String simpleName(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /**
     * A member reference in Mixin's `Lowner;name(desc)` or `Lowner;name:desc` form.
     */
    private static final class MemberRef {
        final String owner;
        final String name;
        final String desc;

        private MemberRef(String owner, String name, String desc) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }

        static MemberRef parse(String target) {
            int ownerEnd = target.indexOf(';');
            String owner = target.substring(1, ownerEnd);
            String rest = target.substring(ownerEnd + 1);
            int split = rest.indexOf(':') >= 0 ? rest.indexOf(':') : rest.indexOf('(');
            String name = rest.substring(0, split);
            String desc = rest.charAt(split) == ':' ? rest.substring(split + 1) : rest.substring(split);
            return new MemberRef(owner, name, desc);
        }
    }
}
