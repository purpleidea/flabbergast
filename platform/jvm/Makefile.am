NULL =

BUILDDIR = $(shell pwd)

bin_SCRIPTS = \
	jflabbergast \
	jflabbergast-repl \
	jflabbergast-docgen \
	$(NULL)

man1_MANS = \
	jflabbergast.1 \
	jflabbergast-repl.1 \
	jflabbergast-docgen.1 \
	$(NULL)

compilerdir = $(pkgdatadir)/compiler
compiler_SCRIPTS = jvm-build-cache

javadir = $(datadir)/java
java_DATA = \
	flabbergast-runtime-$(VERSION).jar \
	flabbergast-compiler-$(VERSION).jar \
	flabbergast-stdlib-$(VERSION).jar \
	$(NULL)

JAVA_RUNTIME_DEPS = \
	$(JANSI_JAR) \
	$(JLINE_JAR) \
	$(JSON_JAR) \
	$(NULL)

JAVA_COMPILER_DEPS = \
	$(ASM_JAR) \
	$(ASM_COMMONS_JAR) \
	$(COMMONS_CLI_JAR) \
	$(NULL)

if BUILD_FATJAR
COMPILER_JAR = flabbergast-$(VERSION).jar
else
COMPILER_JAR = flabbergast-runtime-$(VERSION).jar flabbergast-compiler-$(VERSION).jar
endif

INSTALLED_CP = $(subst $(SPACE),:,$(JAVA_COMPILER_DEPS) $(JAVA_RUNTIME_DEPS) $(addprefix $(javadir)/, $(COMPILER_JAR))):$${CLASSPATH:-}:$(pkgdatadir)/jvm/*
BUILD_COMPILER_JAR = $(addprefix $(BUILDDIR)/, $(COMPILER_JAR))

TESTS = flabbergast-testing-$(VERSION).jar
TEST_EXTENSIONS = .jar
JAR_LOG_COMPILER = ./run_test
AM_JAR_LOG_FLAGS = "$(TESTING_CLASSPATH)"
TESTING_CLASSPATH = $(subst $(SPACE),:,$(JAVA_COMPILER_DEPS) $(JAVA_RUNTIME_DEPS) $(BUILDDIR)/flabbergast-stdlib-$(VERSION).jar $(BUILD_COMPILER_JAR))

RUNTIME_INPUTS = $(wildcard runtime/flabbergast/*.java runtime/flabbergast/*/*.java)
COMPILER_INPUTS = $(wildcard compiler/flabbergast/*.java compiler/flabbergast/*/*.java)
STDLIB_INPUTS = $(shell find ../../stdlib/ -name '*.o_0' -o -name '*.jo_0')
TESTING_INPUTS = $(wildcard testing/flabbergast/*.java)

SPACE = $(NULL) $(NULL)

if BUILD_FATJAR
flabbergast-$(VERSION).jar: $(RUNTIME_INPUTS) $(COMPILER_INPUTS) compiler-jvm.a
	rm -rf .build
	mkdir .build
	cd .build && for each in $(sort $(JAVA_RUNTIME_DEPS) $(JAVA_COMPILER_DEPS)); do $(JAR) xf $$each; done
	rm -rf fat-generated
	./unar compiler-jvm.a fat-generated/flabbergast/compiler
	./unar db-jvm.a fat-generated/flabbergast/db

jvm-build-cache: Makefile
	@echo '#!/bin/sh' > $@
	@echo 'cd "$$1" || exit 1' >> $@
	@echo 'exec $(JAVA) -cp $(INSTALLED_CP) flabbergast.MainBuildCache' >> $@

jflabbergast: Makefile
	@echo '#!/bin/sh' > $@
	@echo 'if [ $$(java -version 2>&1 | head -n 1 | cut -f 2 -d .) -le 7 ]; then JVM_OPTS=-XX:MaxPermSize=512m; else JVM_OPTS=""; fi' >> $@
	@echo 'exec $(JAVA) $${JVM_OPTS} -cp $(INSTALLED_CP) flabbergast.MainPrinter "$$@"' >> $@

jflabbergast-repl: Makefile
	@echo '#!/bin/sh' > $@
	@echo 'if [ $$(java -version 2>&1 | head -n 1 | cut -f 2 -d .) -le 7 ]; then JVM_OPTS=-XX:MaxPermSize=512m; else JVM_OPTS=""; fi' >> $@
	@echo 'exec $(JAVA) $${JVM_OPTS} -cp $(INSTALLED_CP) flabbergast.MainREPL "$$@"' >> $@

jflabbergast-docgen: Makefile
	@echo '#!/bin/sh' > $@
	@echo 'exec $(JAVA) -cp $(INSTALLED_CP) flabbergast.MainDocumenter "$$@"' >> $@

testingclasspath.sh: Makefile
	@echo export CLASSPATH=$(TESTING_CLASSPATH) > $@

install-data-hook:
if BUILD_FATJAR
	$(LN_S) -f flabbergast-$(VERSION).jar $(DESTDIR)$(datadir)/java/flabbergast.jar
else
	$(LN_S) -f flabbergast-runtime-$(VERSION).jar $(DESTDIR)$(datadir)/java/flabbergast-runtime.jar
	$(LN_S) -f flabbergast-compiler-$(VERSION).jar $(DESTDIR)$(datadir)/java/flabbergast-compiler.jar
endif
	$(LN_S) -f flabbergast-stdlib-$(VERSION).jar $(DESTDIR)$(datadir)/java/flabbergast-stdlib.jar

CLEANFILES = \
	jvm-build-cache \
	jflabbergast \
	jflabbergast-repl \
	jflabbergast-docgen \
	compiler.manifest \
	runtime.manifest \
	stdlib.manifest \
	testing.manifest \
	$(java_DATA) \
	flabbergast-runtime-$(VERSION)-sources.jar \
	flabbergast-compiler-$(VERSION)-sources.jar \
	flabbergast-stdlib-$(VERSION)-sources.jar \
	flabbergast-testing-$(VERSION)-sources.jar \
	flabbergast-runtime-$(VERSION)-javadoc.jar \
	flabbergast-compiler-$(VERSION)-javadoc.jar \
	flabbergast-stdlib-$(VERSION)-javadoc.jar \
	flabbergast-testing-$(VERSION)-javadoc.jar \
	$(TESTS) \
	$(NULL)
