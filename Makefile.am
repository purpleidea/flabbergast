ACLOCAL_AMFLAGS = -I m4
NULL =

SUBDIRS = \
	vim \
	$(NULL)

if BUILD_CLI
SUBDIRS += platform/cli
endif

if BUILD_JVM
SUBDIRS += platform/jvm
endif

bin_SCRIPTS = \
	update-flabbergast \
	$(NULL)

man7_MANS = \
	flabbergast_language.7 \
	$(NULL)

pkgdata_DATA = \
	update.makefile \
	$(NULL)

install-data-hook:
	@for f in $$(find lib -name '*.flbgst' -type f -printf '%P\n'); do \
		$(mkinstalldirs) $(DESTDIR)$(pkglibdir)/$$(dirname $$f); \
		$(INSTALL_DATA) $(srcdir)/lib/$$f $(DESTDIR)$(pkglibdir)/$$f; \
	done	
