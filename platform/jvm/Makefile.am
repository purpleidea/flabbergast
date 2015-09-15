NULL =

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
if BUILD_FATJAR
java_DATA = flabbergast-$(VERSION).jar
else
java_DATA = \
	flabbergast-runtime-$(VERSION).jar \
	flabbergast-compiler-$(VERSION).jar \
	$(NULL)
endif
java_DATA += flabbergast-stdlib-$(VERSION).jar

JAVA_RUNTIME_DEPS = \
	$(JLINE_JAR) \
	$(JODA_TIME_JAR) \
	$(NULL)

JAVA_COMPILER_DEPS = \
	$(ASM_JAR) \
	$(COMMONS_CLI_JAR) \
	$(NULL)

if BUILD_FATJAR
COMPILER_JAR = flabbergast-$(VERSION).jar
else
COMPILER_JAR = flabbergast-runtime-$(VERSION).jar flabbergast-compiler-$(VERSION).jar
endif

INSTALLED_CP = $(subst $(SPACE),:,$(JAVA_COMPILER_DEPS) $(JAVA_RUNTIME_DEPS)):$(subst $(SPACE),:$(javadir)/,$(COMPILER_JAR)):$${CLASSPATH:-}

TESTS = flabbergast-testing-$(VERSION).jar
TEST_EXTENSIONS = .jar
JAR_LOG_COMPILER = ./run_test
AM_JAR_LOG_FLAGS = "$(TESTING_CLASSPATH)"
TESTING_CLASSPATH = $(subst $(SPACE),:,$(JAVA_COMPILER_DEPS) $(JAVA_RUNTIME_DEPS)):flabbergast-stdlib-$(VERSION).jar:$(subst $(SPACE),:,$(COMPILER_JAR))

RUNTIME_INPUTS = $(wildcard runtime/flabbergast/*.java runtime/flabbergast/*/*.java)
COMPILER_INPUTS = $(wildcard compiler/flabbergast/*.java)
STDLIB_INPUTS = $(find ../../lib/ -name '*.o_0' -o -name '*.jo_0')
TESTING_INPUTS = $(wildcard testing/flabbergast/*.java)

SPACE = $(NULL) $(NULL)

if BUILD_FATJAR
flabbergast-$(VERSION).jar: $(RUNTIME_INPUTS) $(COMPILER_INPUTS)
	rm -rf .build
	mkdir .build
	cd .build && for each in $(sort $(JAVA_RUNTIME_DEPS) $(JAVA_COMPILER_DEPS)); do $(JAR) xf $$each; done
	$(JAVAC) -g -encoding UTF-8 -cp .build -sourcepath runtime -d .build $(RUNTIME_INPUTS)
	$(JAVAC) -g -encoding UTF-8 -cp .build -sourcepath compiler -d .build $(COMPILER_INPUTS)
	$(JAR) cf "flabbergast-${VERSION}.jar" -C .build .
	rm -rf .build
else
flabbergast-runtime-$(VERSION).jar flabbergast-runtime-$(VERSION)-sources.jar flabbergast-runtime-$(VERSION)-javadoc.jar: runtime.manifest $(RUNTIME_INPUTS)
	CLASSPATH=$(subst $(SPACE),:,$(JAVA_RUNTIME_DEPS)) ./jarc runtime $(VERSION)

flabbergast-compiler-$(VERSION).jar flabbergast-compiler-$(VERSION)-sources.jar flabbergast-compiler-$(VERSION)-javadoc.jar: compiler.manifest $(COMPILER_INPUTS) flabbergast-runtime-$(VERSION).jar
	CLASSPATH=$(subst $(SPACE),:,$(JAVA_COMPILER_DEPS) $(JAVA_RUNTIME_DEPS)):flabbergast-runtime-$(VERSION).jar ./jarc compiler $(VERSION)
endif

flabbergast-stdlib-$(VERSION).jar flabbergast-stdlib-$(VERSION)-sources.jar flabbergast-stdlib-$(VERSION)-javadoc.jar: stdlib.manifest $(STDLIB_INPUTS) flabbergast-compiler-$(VERSION).jar
	rm -rf .build
	mkdir .build
	rsync -a --include '**.o_0' --include '**.jo_0' --include '**/' --exclude '*' ../../lib/ .build
	$(JAR) cfm flabbergast-stdlib-$(VERSION)-sources.jar stdlib.manifest -C .build .
	cd .build && $(JAVA) -cp $(subst $(SPACE),:,$(JAVA_COMPILER_DEPS) $(JAVA_RUNTIME_DEPS)):../flabbergast-runtime-$(VERSION).jar:../flabbergast-compiler-$(VERSION).jar flabbergast.MainBuildCache -P
	find .build -type f -a -not -name '*.class' -exec rm {} \;
	$(JAR) cfm flabbergast-stdlib-$(VERSION).jar stdlib.manifest -C .build .
	rm -rf .build
	$(JAR) cf flabbergast-stdlib-$(VERSION)-javadoc.jar -C stdlib-javadoc .

flabbergast-testing-$(VERSION).jar flabbergast-testing-$(VERSION)-sources.jar flabbergast-testing-$(VERSION)-javadoc.jar: testing.manifest $(TESTING_INPUTS) flabbergast-compiler-$(VERSION).jar flabbergast-stdlib-$(VERSION).jar
	CLASSPATH=$(TESTING_CLASSPATH) ./jarc testing $(VERSION)

runtime.manifest: Makefile
	@echo "Manifest-Version: 1.0" > $@
	@echo "Class-Path: $(JAVA_RUNTIME_DEPS)" >> $@
	@echo "Created-By: $(VERSION) (The Flabbergast Project)" >> $@

compiler.manifest: Makefile
	@echo "Manifest-Version: 1.0" > $@
	@echo "Class-Path: $(javadir)/flabbergast-runtime-$(VERSION).jar $(JAVA_COMPILER_DEPS)" >> $@
	@echo "Created-By: $(VERSION) (The Flabbergast Project)" >> $@

stdlib.manifest: Makefile
	@echo "Manifest-Version: 1.0" > $@
	@echo "Created-By: $(VERSION) (The Flabbergast Project)" >> $@

testing.manifest: Makefile
	@echo "Manifest-Version: 1.0" > $@
	@echo "Main-Class: flabbergast.TestHarness" >> $@
	@echo "Created-By: $(VERSION) (The Flabbergast Project)" >> $@

jvm-build-cache: Makefile
	@echo '#!/bin/sh' > $@
	@echo 'cd "$$1" || exit 1' >> $@
	@echo 'exec $(JAVA) -cp $(INSTALLED_CP) flabbergast.MainBuildCache' >> $@

jflabbergast: Makefile
	@echo '#!/bin/sh' > $@
	@echo 'exec $(JAVA) -cp $(INSTALLED_CP) flabbergast.MainPrinter "$$@"' >> $@

jflabbergast-repl: Makefile
	@echo '#!/bin/sh' > $@
	@echo 'exec $(JAVA) -cp $(INSTALLED_CP) flabbergast.MainREPL "$$@"' >> $@

jflabbergast-docgen: Makefile
	@echo '#!/bin/sh' > $@
	@echo 'exec $(JAVA) -cp $(INSTALLED_CP) flabbergast.MainDocumenter "$$@"' >> $@

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
