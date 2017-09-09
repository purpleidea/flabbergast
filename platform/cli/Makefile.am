NULL =

bin_SCRIPTS = \
	nflabbergast \
	nflabbergast-repl \
	nflabbergast-docgen \
	nflabbergast-urihandler \
	$(NULL)

man1_MANS = \
	nflabbergast.1 \
	nflabbergast-repl.1 \
	nflabbergast-docgen.1 \
	$(NULL)

MSBUILD_OUTPUT = \
	bin/Debug/Flabbergast.Compiler.dll \
	bin/Debug/Flabbergast.Runtime.dll \
	bin/Debug/flabbergast-build.exe \
	bin/Debug/flabbergast-compiler.exe \
	bin/Debug/flabbergast-documenter.exe \
	bin/Debug/flabbergast-repl.exe \
	bin/Debug/flabbergast-urihandler.exe \
	bin/Debug/flabbergast.exe \
	$(NULL)

clidir = $(pkgdatadir)/cli
cli_DATA = \
	$(MSBUILD_OUTPUT)
	Flabbergast.Runtime.urihandler \
	$(NULL)

compilerdir = $(pkgdatadir)/compiler
compiler_SCRIPTS = cli-build-cache

pkgconfigdir = $(prefix)/lib/pkgconfig
pkgconfig_DATA = \
	flabbergast-compiler-cli.pc \
	flabbergast-runtime-cli.pc \
	$(NULL)

TESTS = bin/Debug/flabbergast-test.exe
TEST_EXTENSIONS = .exe
EXE_LOG_COMPILER = cli

$(cli_DATA) $(TEST): $(wildcard *.cs *.sln *.csproj)
	$(MSBUILD) /verbosity:quiet flabbergast.sln

flabbergast-runtime-cli.pc: Makefile
	@echo Libraries=$(clidir)/Flabbergast.Runtime.dll > $@
	@echo Name: Flabbergast Runtime >> $@
	@echo Description: The Flabbergast runtime support library >> $@
	@echo Version: $(VERSION) >> $@
	@echo Libs: -r:Flabbergast.Runtime.dll >> $@

flabbergast-compiler-cli.pc: Makefile
	@echo Libraries=$(clidir)/Flabbergast.Compiler.dll > $@
	@echo Name: Flabbergast Compiler >> $@
	@echo Description: The Flabbergast compiler-as-a-service library >> $@
	@echo Version: $(VERSION) >> $@
	@echo Requires: flabbergast-runtime-cli >> $@
	@echo Libs: -r:Flabbergast.Compiler.dll >> $@

cli-build-cache: Makefile
	@echo '#!/bin/sh' > $@
	@echo 'cd "$$1" || exit 1' >> $@
	@echo 'exec cli $(clidir)/flabbergast-build.exe' >> $@

nflabbergast: Makefile
	@echo '#!/bin/sh' > $@
	@echo 'exec cli $(clidir)/flabbergast.exe "$$@"' >> $@

nflabbergast-repl: Makefile
	@echo '#!/bin/sh' > $@
	@echo 'exec cli $(clidir)/flabbergast-repl.exe "$$@"' >> $@

nflabbergast-docgen: Makefile
	@echo '#!/bin/sh' > $@
	@echo 'exec cli $(clidir)/flabbergast-documenter.exe "$$@"' >> $@

nflabbergast-urihandler: Makefile
	@echo '#!/bin/sh' > $@
	@echo 'exec cli $(clidir)/flabbergast-urihandler.exe "$$@"' >> $@

Flabbergast.Runtime.urihandler: bin/Debug/flabbergast-urihandler.exe bin/Debug/Flabbergast.Runtime.dll
	@cli bin/Debug/flabbergast-urihandler.exe bin/Debug/Flabbergast.Runtime.dll > $@

clean-local:
	$(MSBUILD) /verbosity:quiet /target:Clean flabbergast.sln

CLEANFILES = \
	cli-build-cache \
	nflabbergast \
	nflabbergast-repl \
	nflabbergast-docgen \
	$(NULL)
