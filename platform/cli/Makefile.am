NULL =

bin_SCRIPTS = \
	nflabbergast \
	nflabbergast-repl \
	$(NULL)

clidir = $(libdir)/flabbergast-cli
cli_DATA = \
	bin/Debug/Flabbergast.Compiler.dll \
	bin/Debug/Flabbergast.Runtime.dll \
	bin/Debug/flabbergast-compiler.exe \
	bin/Debug/flabbergast-repl.exe \
	bin/Debug/flabbergast.exe \
	$(NULL)

compilerdir = $(libdir)/flabbergast-compiler
compiler_DATA = cli.makefile

$(cli_DATA): $(wildcard *.cs *.sln *.csproj)
	$(XBUILD) /verbosity:quiet flabbergast.sln

cli.makefile: Makefile
	echo "EXTS += dll\n%.dll: %.flbgst\n\t/usr/bin/env cli $(clidir)/flabbergast-compiler.exe $$<" > $@

nflabbergast: Makefile
	echo "#!/bin/sh\nexec env cli $(clidir)/flabbergast.exe \"$$@\"" > $@

nflabbergast-repl: Makefile
	echo "#!/bin/sh\nexec env cli $(clidir)/flabbergast-repl.exe \"$$@\"" > $@

clean-local:
	$(XBUILD) /verbosity:quiet /target:Clean flabbergast.sln

CLEANFILES = \
	cli.makefile \
	nflabbergast \
	nflabbergast-repl \
	$(NULL)
