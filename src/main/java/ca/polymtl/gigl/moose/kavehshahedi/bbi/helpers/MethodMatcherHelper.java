package ca.polymtl.gigl.moose.kavehshahedi.bbi.helpers;

import ca.polymtl.gigl.moose.kavehshahedi.bbi.FunctionEntryAgent;
import ca.polymtl.gigl.moose.kavehshahedi.bbi.models.Configuration;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.util.Arrays;
import java.util.List;

public class MethodMatcherHelper {

    public static ElementMatcher.Junction<MethodDescription> createMethodMatcher(Configuration.Instrumentation instrumentation) {
        List<String> instrumentMethodSignatures = instrumentation.getTargetMethods().getInstrument();
        List<String> ignoreMethodSignatures = instrumentation.getTargetMethods().getIgnore();

        ElementMatcher.Junction<MethodDescription> methodMatchers = ElementMatchers.none();
        if (instrumentMethodSignatures.isEmpty())
            methodMatchers = ElementMatchers.isMethod();

        for (String signature : instrumentMethodSignatures) {
            MethodSignature methodSignature = parseMethodSignature(signature);
            if (methodSignature != null) {
                methodMatchers = methodMatchers.or(createMatcherFromSignature(methodSignature));
            }
        }

        for (String signature : ignoreMethodSignatures) {
            MethodSignature methodSignature = parseMethodSignature(signature);
            if (methodSignature != null) {
                methodMatchers = methodMatchers.and(ElementMatchers.not(createMatcherFromSignature(methodSignature)));
            }
        }

        return methodMatchers;
    }

    public static ElementMatcher.Junction<TypeDescription> createTargetPackageMatcher(Configuration.Instrumentation instrumentation) {
        ElementMatcher.Junction<TypeDescription> targetPackageMatcher = ElementMatchers.any();
        if (!instrumentation.getTargetPackage().isEmpty()
                && !instrumentation.getTargetPackage().equals("*")) {
            targetPackageMatcher = ElementMatchers.nameStartsWith(instrumentation.getTargetPackage());
        }

        // Exclude the agent class from instrumentation (along with logging and other)
        targetPackageMatcher = targetPackageMatcher
                .and(ElementMatchers.not(ElementMatchers.nameStartsWith(FunctionEntryAgent.class.getPackageName())));
        targetPackageMatcher = targetPackageMatcher
                .and(ElementMatchers.not(ElementMatchers.nameStartsWith("org.apache.logging.log4j")));

        return targetPackageMatcher;
    }

    private static ElementMatcher.Junction<MethodDescription> createMatcherFromSignature(
            MethodSignature methodSignature) {
        ElementMatcher.Junction<MethodDescription> matcher = ElementMatchers.named(methodSignature.getMethodName())
                .and(ElementMatchers.takesArguments(methodSignature.getNumArguments()))
                .and(ElementMatchers.isDeclaredBy(ElementMatchers.named(methodSignature.getDeclaringClass())));

        // Add static/non-static matcher
        matcher = methodSignature.isStatic() ? matcher.and(ElementMatchers.isStatic())
                : matcher.and(ElementMatchers.not(ElementMatchers.isStatic()));

        // Add public/non-public matcher
        matcher = methodSignature.isPublic() ? matcher.and(ElementMatchers.isPublic())
                : matcher.and(ElementMatchers.not(ElementMatchers.isPublic()));

        // Add private/non-private matcher
        matcher = methodSignature.isPrivate() ? matcher.and(ElementMatchers.isPrivate())
                : matcher.and(ElementMatchers.not(ElementMatchers.isPrivate()));

        // Add protected/non-protected matcher
        matcher = methodSignature.isProtected() ? matcher.and(ElementMatchers.isProtected())
                : matcher.and(ElementMatchers.not(ElementMatchers.isProtected()));

        // Add package-private/non-package-private matcher
        matcher = methodSignature.isPackagePrivate() ? matcher.and(ElementMatchers.isPackagePrivate())
                : matcher.and(ElementMatchers.not(ElementMatchers.isPackagePrivate()));

        return matcher;
    }

    private static class MethodSignature {
        private final String methodName;
        private final String declaringClass;
        private final int numArguments;
        private final boolean isStatic;
        private final boolean isPublic;
        private final boolean isPrivate;
        private final boolean isProtected;
        private final boolean isPackagePrivate;

        public MethodSignature(String methodName, String declaringClass, int numArguments, boolean isStatic,
                boolean isPublic,
                boolean isPrivate, boolean isProtected, boolean isPackagePrivate) {
            this.methodName = methodName;
            this.declaringClass = declaringClass;
            this.numArguments = numArguments;
            this.isStatic = isStatic;
            this.isPublic = isPublic;
            this.isPrivate = isPrivate;
            this.isProtected = isProtected;
            this.isPackagePrivate = isPackagePrivate;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getDeclaringClass() {
            return declaringClass;
        }

        public int getNumArguments() {
            return numArguments;
        }

        public boolean isStatic() {
            return isStatic;
        }

        public boolean isPublic() {
            return isPublic;
        }

        public boolean isPrivate() {
            return isPrivate;
        }

        public boolean isProtected() {
            return isProtected;
        }

        public boolean isPackagePrivate() {
            return isPackagePrivate;
        }

        @Override
        public String toString() {
            return "MethodSignature{" + "methodName='" + methodName + '\'' + ", declaringClass='" + declaringClass
                    + '\''
                    + ", numArguments=" + numArguments + ", isStatic=" + isStatic + ", isPublic=" + isPublic
                    + ", isPrivate="
                    + isPrivate + ", isProtected=" + isProtected + ", isPackagePrivate=" + isPackagePrivate + '}';
        }
    }

    static MethodSignature parseMethodSignature(String methodSignature) {
        try {
            // Extract method part and argument part
            String methodPart = methodSignature.substring(0, methodSignature.indexOf('(')).trim();
            String argumentsPart = methodSignature
                    .substring(methodSignature.indexOf('(') + 1, methodSignature.indexOf(')')).trim();

            // Split method part into parts (modifiers, return type, class and method name)
            String[] methodParts = methodPart.split("\\s+");

            // Determine if method is static
            boolean isStatic = Arrays.asList(methodParts).contains("static");

            // Determine if method is public, private, protedted or package-private
            boolean isPublic = Arrays.asList(methodParts).contains("public");
            boolean isPrivate = Arrays.asList(methodParts).contains("private");
            boolean isProtected = Arrays.asList(methodParts).contains("protected");
            boolean isPackagePrivate = !isPublic && !isPrivate && !isProtected;

            // Extract method name and declaring class
            String fullMethodName = methodParts[methodParts.length - 1];
            String[] methodNameParts = fullMethodName.split("\\.");
            String methodName = methodNameParts[methodNameParts.length - 1];
            String declaringClass = String.join(".",
                    Arrays.copyOfRange(methodNameParts, 0, methodNameParts.length - 1));

            int numArguments = argumentsPart.isEmpty() ? 0 : argumentsPart.split(",").length;

            return new MethodSignature(methodName, declaringClass, numArguments, isStatic, isPublic, isPrivate,
                    isProtected,
                    isPackagePrivate);
        } catch (Exception e) {
            System.err.println("Error parsing method signature: " + methodSignature);
            return null;
        }
    }
}