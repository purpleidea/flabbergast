AC_INIT([flabbergast], [0.19], [andre@masella.name], [flabbergast], [http://flabbergast.org])
AC_CONFIG_SRCDIR([Makefile.am])
AC_CONFIG_AUX_DIR([build-aux])
AC_CONFIG_MACRO_DIR([m4])
AM_INIT_AUTOMAKE([1.11 foreign no-dependencies])
AM_MAINTAINER_MODE

m4_ifdef([AM_SILENT_RULES], [AM_SILENT_RULES([yes])], )

# Checks for programs.
AC_PROG_LN_S

# JVM
AC_CHECK_TOOL(AR, ar)
AC_PATH_TOOL(JAVA, java, undefined)
AC_PATH_TOOL(JAVAC, javac, undefined)
AC_PATH_TOOL(JAR, jar, undefined)
AC_PATH_TOOL(MAVEN, mvn, undefined)
if test "$JAVA" = undefined -o "$JAVAC" = undefined -o "$JAR" = undefined -o "$JAR" = undefined; then
	target_java=no
else
	AC_CHECK_FILES([${ASM_JAR} ${COMMONS_CLI_JAR} ${JLINE_JAR} ${JSON_JAR} ${METAINF_JAR}], [ target_java=yes ], [ target_java=no ])
fi
else
	target_java=no
fi

AC_CONFIG_FILES([
	Makefile
	platform/jvm/Makefile
	vim/Makefile
]) 
AC_CONFIG_FILES([update-flabbergast:update-flabbergast.in], [chmod +x update-flabbergast])
AC_OUTPUT
