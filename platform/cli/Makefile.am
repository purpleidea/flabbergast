NULL =

bin_SCRIPTS = \
	nflabbergast \
	nflabbergast-repl \
	$(NULL)

man1_MANS = \
	nflabbergast.1 \
	nflabbergast-repl.1 \
	$(NULL)

clidir = $(pkgdatadir)/cli
cli_DATA = \
	bin/Debug/Flabbergast.Compiler.dll \
	bin/Debug/Flabbergast.Runtime.dll \
	bin/Debug/flabbergast-build.exe \
	bin/Debug/flabbergast-compiler.exe \
	bin/Debug/flabbergast-repl.exe \
	bin/Debug/flabbergast.exe \
	$(NULL)

compilerdir = $(pkgdatadir)/compiler
compiler_SCRIPTS = cli-build-cache

TESTS = bin/Debug/flabbergast-test.exe
TEST_EXTENSIONS = .exe
EXE_LOG_COMPILER = cli

$(cli_DATA) $(TEST): $(wildcard *.cs *.sln *.csproj)
	$(XBUILD) /verbosity:quiet flabbergast.sln

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

clean-local:
	$(XBUILD) /verbosity:quiet /target:Clean flabbergast.sln

CLEANFILES = \
	cli-build-cache \
	nflabbergast \
	nflabbergast-repl \
	$(NULL)
